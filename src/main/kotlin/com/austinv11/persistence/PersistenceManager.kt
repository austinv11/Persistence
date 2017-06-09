@file:JvmName("PersistenceManager")

package com.austinv11.persistence

import com.austinv11.persistence.impl.DefaultFactory
import com.austinv11.persistence.impl.NetworkStore
import com.austinv11.persistence.impl.NoOpConnectionSpy
import com.austinv11.persistence.impl.NoOpPreProcessor
import com.austinv11.persistence.internal.SourceAwareProxy
import com.austinv11.persistence.internal.TwoWaySocket
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import java.io.InvalidClassException
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.experimental.and

/**
 * This manages persistence.
 */
class PersistenceManager {

    internal val stores: MutableMap<Class<*>, Store<*>> = ConcurrentHashMap()
    internal val customDataTransformers: MutableList<ExternalData<in Any>> = CopyOnWriteArrayList()
    @Volatile internal var factory: StoreFactory = DefaultFactory()
//    @Volatile internal var explicitPropertiesOnly = false
    @Volatile internal var deepPersistence = false
    @Volatile internal var version: Int? = null 
    @Volatile internal var port = 6000
    @Volatile internal var allowedConnections = 2
    @Volatile internal var spy: ConnectionSpy = NoOpConnectionSpy()
    @Volatile internal var processor: PreProcessor = NoOpPreProcessor()
    internal val socket: TwoWaySocket by lazy { 
        TwoWaySocket(this@PersistenceManager, port, allowedConnections, spy)
    }  

    /**
     * This gets or generates a [Store] for the provided type.
     *
     * @param clazz The type to get a storeFor for.
     * @return The store.
     */
    fun <T : Any> storeFor(clazz: Class<T>): Store<T> {
        return stores.computeIfAbsent(clazz) { NetworkStore<T>(factory.buildStore(this, clazz), socket) } as Store<T>
    }

    /**
     * This gets or generates a [Store] for the provided type.
     *
     * @return The store.
     */
    inline fun <reified T : Any> storeFor(): Store<T> {
        return storeFor(T::class.java)
    }

    /**
     * This persists an object in the appropriate storeFor.
     * **WARNING:** Only use the object returned by this method and NOT the originally passed in value;
     * they are different!
     * 
     * It should also be noted that the returned instance also now implements the [Persisted] interface.
     *
     * @param obj The object to persist.
     * @return The mutable object clone.
     */
    fun <T : Any> persist(obj: T): T {
        val store = storeFor(obj.javaClass)
        val wrapped = wrap(obj, store)
        store.insert(wrapped)
        return wrapped
    }
    
    internal fun <T : Any> persistQuietly(obj: T): T {
        val store = storeFor(obj.javaClass)
        val wrapped = wrap(obj, store)
        store.insertQuietly(wrapped)
        return wrapped
    }

    /**
     * This sets the [StoreFactory] this manager uses to create [Store]s.
     *
     * @param factory The new factory to use.
     */
    fun setFactory(factory: StoreFactory) {
        this.factory = factory
    }

    /**
     * This generates a a hash for any given object following the PPPP spec.
     *
     * This hash has bytes in the following (big-endian) format:
     * {123}{4}{5678}
     *   ^   ^   ^
     *   a   b   c
     *
     * Where:
     * a: Name hash; Byte 1 is the first letter of the object name, byte 2 is the middle letter of the object name (or rather, the letter at position floor(name.len / 2)), and byte 3 is the letter at the end of the name.
     * b: Field count; single byte representation of the number of properties in the object (overflows are allowed, just use the 8 least significant bits!).
     * c: Object hashcode; the 4 byte custom object hashcode which should be identical to Persistable#hashCode.
     */
    fun generateHash(obj: Any): Long {
        val obj = obj.unwrapObject() //Make sure this isn't proxied
        
        val name = obj::class.simpleName!!
        val nameHash = charArrayOf(name[0], name[Math.floor(name.length / 2.0).toInt()], name[name.length-1])
        val fieldCount = matchProperties(obj).size
        var hashCode = obj.hashCode()
        var hash = 0L

        hash = shiftAndAdd(hash, nameHash[0].toByte())
        hash = shiftAndAdd(hash, nameHash[1].toByte())
        hash = shiftAndAdd(hash, nameHash[2].toByte())

        hash = shiftAndAdd(hash, fieldCount.toByte())

        hash = shiftAndAdd(hash, (hashCode and mask_int).toByte())
        hash = shiftAndAdd(hash, ((hashCode shr 8).apply { hashCode = this }).toByte() and mask)
        hash = shiftAndAdd(hash, ((hashCode shr 8).apply { hashCode = this }).toByte() and mask)
        hash = shiftAndAdd(hash, ((hashCode shr 8).apply { hashCode = this }).toByte() and mask)

        return hash
    }

    /**
     * This attempts to connect to the provided node.
     */
    @JvmOverloads 
    fun connectTo(host: String, port: Int, metadata: Map<String, Any?>? = null) {
        launch(CommonPool) {
            socket.connectTo(host, port, metadata)
        }
    }

//    /**
//     * This sets whether properties getters and setters must be annotated with [Getter] and [Setter]. If
//     * this is not set to true (the default), heuristic analysis will be employed to attempt to find eligible
//     * properties.
//     *
//     * @param explicitPropertiesOnly Set this to true to not employ heuristics when detecting properties.
//     * @return The same manager for chaining methods.
//     */
//    fun setExplicitPropertiesOnly(explicitPropertiesOnly: Boolean): PersistenceManager {
//        this.explicitPropertiesOnly = explicitPropertiesOnly
//        return this
//    }

    /**
     * This sets whether nested objects are also persisted. This can hurt performance and lead to weird bugs so this is
     * FALSE by default. It is recommended that persisted objects' fields are immutable to prevent necessitating this.
     *
     * @param deepPersistence Set this to true to enable deep persistence.
     * @return The same manager for chaining methods.
     */
    fun setDeepPersistence(deepPersistence: Boolean): PersistenceManager {
        this.deepPersistence = deepPersistence
        return this
    }

    /**
     * This registers a custom data transformer. NOTE: Transformers registered earlier are given higher priority in
     * translating data.
     */
    fun registerDataType(type: ExternalData<in Any>): PersistenceManager {
        this.customDataTransformers += type
        return this
    }

    /**
     * This sets a custom version which can be analyzed by data receivers.
     */
    fun setVersion(v: Int?): PersistenceManager {
        this.version = v
        return this
    }

    /**
     * This sets the port which the server listens on (default is 6000).
     */
    fun setServerPort(port: Int): PersistenceManager {
        this.port = port
        return this
    }

    /**
     * This sets the number of allowed connections to this node (default is 2).
     */
    fun setSetAllowedConnections(allowedConnections: Int): PersistenceManager {
        this.allowedConnections = allowedConnections
        return this
    }

    /**
     * This sets the connection spy/interceptor.
     */
    fun setConnectionSpy(spy: ConnectionSpy): PersistenceManager {
        this.spy = spy
        return this
    }

    /**
     * This sets the pre processor for data. By default this uses a No-Op implementation.
     * 
     * @see NoOpPreProcessor
     * @see EncryptedPreProcessor
     */
    fun setPreProcessor(processor: PreProcessor) : PersistenceManager {
        this.processor = processor
        return this
    }

    /**
     * Invalidates persisted caches. If this node is disconnected with no other peers, it is recommended that this node
     * saves the persisted objects somehow locally.
     * 
     * @see [ConnectionSpy]
     */
    @Synchronized fun invalidate() {
        synchronized(stores) {
            stores.forEach { k, v ->
                v.clearQuietly()
            }
            stores.clear()
        }
    }

    /**
     * This gets the currently active connections.
     */
    fun getConnections(): List<Connection> {
        return socket.connections.map { it.connection }
    }
    
    internal fun Any.findInterfaces(): Array<Class<*>> = this.javaClass.interfaces

    @Suppress("UNCHECKED_CAST")
    internal fun <T: Any> wrap(obj: T, store: Store<T>): T {
        val properties = matchProperties(obj)

        val proxy = Proxy.newProxyInstance(obj::class.java.classLoader, obj.findInterfaces() + Persisted::class.java, 
                SourceAwareProxy(obj, store, properties, this)) as T
        
        return proxy
    }
    
    internal fun findType(map: Map<String, Any?>): Class<*> {
        val typeCandidates = stores.mapValues { socket.context.matchProperties(it.key) }
        val likelyCandidates = mutableMapOf<Double, MutableList<Class<*>>>() //Key = percent certainty of match, candidates
        typeCandidates.forEach { k, v ->
            //TODO: More advanced heuristics for checking types
            var certainty = v.filter { map.containsKey(it.name) }.size.toDouble() / v.size.toDouble()

            if (certainty > 0.1) //We want a certainty which is greater than 10% only
                likelyCandidates.computeIfAbsent(certainty) { mutableListOf() }.add(k)
        }

        if (likelyCandidates.isEmpty()) throw InvalidClassException("Unable to map object! Maybe a storeFor for it doesn't exist?")

        return likelyCandidates[likelyCandidates.toSortedMap().lastKey()]!!.first()
    }

    internal fun mapValues(map: Map<String, Any?>, clazz: Class<*>, _instance: Any? = null): Any {
        val instance = _instance ?: clazz.fastInstance()
        val properties = if (_instance == null) matchProperties(clazz) else matchProperties(instance)
        map.mapKeys { val key = it.key; properties.firstOrNull { it.name == key } }
                .filter { it.key != null }
                .forEach { k, v -> k!!.setter.let { if (_instance == null) it.bindTo(instance) else it }.invokeWithArguments(v) }
        return instance
    }
}
