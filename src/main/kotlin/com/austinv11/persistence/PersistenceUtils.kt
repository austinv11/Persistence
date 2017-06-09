@file:JvmName("PersistenceUtils")

package com.austinv11.persistence

import com.austinv11.persistence.internal.SourceAwareProxy
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import kotlin.experimental.and

/**
 * This is the version of PPPP this version of the api is using.
 */
const val PPPP_VERSION = 2

internal val logger = LoggerFactory.getLogger("Persistence")

internal const val mask_int = 0xff
internal const val mask = mask_int.toByte()

private typealias Unsafe = sun.misc.Unsafe //So we don't import
internal val UNSAFE: Unsafe? = try { 
    val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe")
    theUnsafe.isAccessible = true
    theUnsafe.get(null) as Unsafe 
} catch (t: Throwable) { null }

fun Any.isPersisted(): Boolean {
    return this is Persisted
}

fun Any.safeUnpersist() {
    (this as? Persisted)?.unpersist()
}

internal fun <T: Any> T.unwrapObject(): T {
    if (!Proxy.isProxyClass(this.javaClass)) return this

    return (Proxy.getInvocationHandler(this) as SourceAwareProxy<*>).source as T
}

internal fun <T> Class<T>.fastInstance(): T { //We should use unsafe for fast allocation if possible, else fall back to reflection
    if (UNSAFE != null) {
        return UNSAFE.allocateInstance(this) as T
    } else {
        return this.newInstance() //Please don't crash
    }
}

internal fun Any.map(manager: PersistenceManager): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    val properties = manager.matchProperties(this)
    properties.forEach { 
        map[it.name] = it.getter.invokeWithArguments()
    }
    return map
}

internal fun shiftAndAdd(original: Long, toAdd: Byte): Long {
    return (original shl 8) + (toAdd and mask)
}

internal fun <T: Any> PersistenceManager.matchProperties(clazz: Class<T>): List<Property<*>> {
    return matchProperties(null, clazz)
}

internal fun <T: Any> PersistenceManager.matchProperties(obj: T?, clazz: Class<T> = obj!!.javaClass): List<Property<*>> {
    val getters = mutableListOf<UnmatchedGetter<*>>()
    val setters = mutableListOf<UnmatchedSetter<*>>()

    clazz.methods.filter {
        it.isAnnotationPresent(Getter::class.java)
                || (/*!explicitPropertiesOnly
                && */Modifier.isPublic(it.modifiers)
                && it.parameterCount == 0
                && it.name.applyGetterHeuristics() != null)
    }.forEach {
        getters += UnmatchedGetter(it, it.heuristicName!!, it.returnType)
    }
    clazz.methods.filter {
        it.isAnnotationPresent(Setter::class.java)
                || (/*!explicitPropertiesOnly
                && */Modifier.isPublic(it.modifiers)
                && it.parameterCount == 1
                && !it.isVarArgs
                && it.name.applySetterHeuristics() != null)
    }.forEach {
        setters += UnmatchedSetter(it, it.heuristicName!!, it.parameterTypes[0])
    }

    val properties = mutableListOf<Property<*>>()
    val lookup = MethodHandles.lookup()
    getters.forEach {
        val name = it.name
        val setter = setters.firstOrNull { it.name == name }
        if (setter != null)
            properties += Property(lookup.unreflect(it.method).let { if (obj != null) it.bindTo(obj) else it },
                    lookup.unreflect(setter.method).let { if (obj != null) it.bindTo(obj) else it },
                    name, it.type)
    }

    return properties
}

internal val Method.heuristicName: String?
    get() {
        if (this.isAnnotationPresent(Getter::class.java)) {
            val name = this.getAnnotation(Getter::class.java).property
            return if (name.isEmpty()) this.name.applyGetterHeuristics() else name
        } else if (this.isAnnotationPresent(Setter::class.java)) {
            val name = this.getAnnotation(Setter::class.java).property
            return if (name.isEmpty()) this.name.applySetterHeuristics() else name
        } else if (this.parameterCount == 0) {
            return this.name.applyGetterHeuristics()
        } else if (this.parameterCount == 1 && !this.isVarArgs) {
            return this.name.applySetterHeuristics()
        } else {
            return null
        }
    }

private fun String.applyGetterHeuristics(): String? = applyGenericPropertyHeuristics("get")

private fun String.applySetterHeuristics(): String? = applyGenericPropertyHeuristics("set")

private fun String.applyGenericPropertyHeuristics(key: String): String? {
    if (startsWith(key)) return removePrefix(key).decapitalize()

    return null
}

private data class UnmatchedGetter<T>(val method: Method,
                                      val name: String,
                                      val type: Class<T>)

private data class UnmatchedSetter<T>(val method: Method,
                                      val name: String,
                                      val type: Class<T>)

data class Property<T>(val getter: MethodHandle,
                                val setter: MethodHandle,
                                val name: String,
                                val type: Class<T>)
