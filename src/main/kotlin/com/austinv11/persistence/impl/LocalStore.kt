package com.austinv11.persistence.impl

import com.austinv11.persistence.PersistenceManager
import com.austinv11.persistence.Store
import java.util.concurrent.ConcurrentHashMap

/**
 * A [Store] implementation which is backed by a [ConcurrentHashMap].
 */
class LocalStore<T: Any>(val persistenceManager: PersistenceManager) : Store<T> {
    
    private val backing = ConcurrentHashMap<Long, T>()
    
    override fun insert(obj: T): T? {
        return backing.put(persistenceManager.generateHash(obj), obj)
    }

    override fun insertQuietly(obj: T): T? {
        return insert(obj)
    }

    override fun remove(obj: T): Boolean {
        return backing.remove(persistenceManager.generateHash(obj)) != null
    }

    override fun removeQuietly(obj: T): Boolean {
        return remove(obj)
    }

    override fun removeHash(hash: Long): Boolean {
        return backing.remove(hash) != null
    }

    override fun removeHashQuietly(hash: Long): Boolean {
        return removeHash(hash)
    }

    override fun contains(obj: T): Boolean {
        return backing.containsKey(persistenceManager.generateHash(obj))
    }

    override fun containsHash(hash: Long): Boolean {
        return backing.containsKey(hash)
    }

    override fun get(hash: Long): T? {
        return backing[hash]
    }

    override fun update(originalHash: Long, obj: T, hint: Pair<Class<*>, String>): T? {
        return updateQuietly(originalHash, obj)
    }

    override fun updateQuietly(originalHash: Long, obj: T): T? {
        if (!containsHash(originalHash)) throw NoSuchElementException()

        return backing.remove(originalHash).also { insert(obj) }
    }

    override fun size(): Int {
        return backing.size
    }

    override fun clear() {
        backing.clear()
    }

    override fun clearQuietly() {
        clear()
    }

    override fun collect(): MutableCollection<T> {
        return backing.values
    }

    override fun iterator(): MutableIterator<T> {
        return backing.values.iterator()
    }
}
