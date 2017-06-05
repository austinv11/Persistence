package com.austinv11.persistence.impl

import com.austinv11.persistence.PersistenceManager
import com.austinv11.persistence.Store
import com.austinv11.persistence.internal.Payload
import com.austinv11.persistence.internal.TwoWaySocket
import com.austinv11.persistence.map
import com.austinv11.persistence.unwrapObject
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking

/**
 * This represents a [Store] which manages the network.
 */
class NetworkStore<T: Any>(private val localStore: Store<T>,
                           private val socket: TwoWaySocket,
                           private val manager: PersistenceManager = socket.context) : Store<T> {
    
    override fun insert(obj: T): T? {
        val obj = obj.unwrapObject()
        launch(CommonPool) {
            socket.connections.forEach {
                it.send(Payload.Creation(d = obj.map(manager), h = manager.generateHash(obj)))
            }
        }
        
        return insertQuietly(obj)
    }

    override fun insertQuietly(obj: T): T? {
        val obj = obj.unwrapObject()
        return localStore.insert(obj)
    }

    override fun remove(obj: T): Boolean {
        val obj = obj.unwrapObject()
        return removeHash(manager.generateHash(obj))
    }

    override fun removeHash(hash: Long): Boolean {
        launch(CommonPool) {
            socket.connections.forEach {
                it.send(Payload.Removal(h = hash))
            }
        }
        
        return removeHashQuietly(hash)
    }

    override fun removeQuietly(obj: T): Boolean {
        val obj = obj.unwrapObject()
        return removeHashQuietly(manager.generateHash(obj))
    }

    override fun removeHashQuietly(hash: Long): Boolean {
        return localStore.removeHash(hash)
    }

    override fun contains(obj: T): Boolean {
        val obj = obj.unwrapObject()
        return localStore.contains(obj)
    }

    override fun containsHash(hash: Long): Boolean {
        return localStore.containsHash(hash)
    }

    override fun get(hash: Long): T? {
        return localStore.get(hash)
    }

    override fun update(originalHash: Long, obj: T, hint: Pair<Class<*>, String>): T? {
        val obj = obj.unwrapObject()
        launch(CommonPool) {
            val map = obj.map(manager).filterKeys { it == hint.second }
            socket.connections.forEach { 
                it.send(Payload.Change(d = map, h = manager.generateHash(obj), oh = originalHash))
            }
        }
        return updateQuietly(originalHash, obj)
    }

    override fun updateQuietly(originalHash: Long, obj: T): T? {
        val obj = obj.unwrapObject()
        return localStore.updateQuietly(originalHash, obj)
    }

    override fun size(): Int {
        return localStore.size()
    }

    override fun iterator(): MutableIterator<T> {
        return localStore.iterator()
    }

    override fun clear() {
        runBlocking {
            socket.connections.forEach {
                it.send(Payload.Removal(h = manager.generateHash(it)))
            }
        }
        clearQuietly()
    }

    override fun clearQuietly() {
        localStore.clearQuietly()
    }

    override fun collect(): MutableCollection<T> {
        return localStore.collect()
    }
}
