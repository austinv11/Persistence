package com.austinv11.persistence.impl

import com.austinv11.persistence.PersistenceManager
import com.austinv11.persistence.Store
import com.austinv11.persistence.StoreFactory

class DefaultFactory : StoreFactory {
    
    override fun <T : Any> buildStore(manager: PersistenceManager, type: Class<T>): Store<T> = LocalStore(manager)
}
