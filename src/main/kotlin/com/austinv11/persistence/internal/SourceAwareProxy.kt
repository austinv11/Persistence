package com.austinv11.persistence.internal

import com.austinv11.persistence.*
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class SourceAwareProxy<T: Any>(val source: T,
                          val store: Store<T>,
                          val properties: List<Property<*>>,
                          val manager: PersistenceManager) : InvocationHandler {
    
    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        val args = args ?: emptyArray()
        if (method.declaringClass == Object::class.java) return method.invoke(source, *args) //Short circuit since we don't care about object methods

        if (method.declaringClass == Persisted::class.java) {
            if (method.name == "unpersist") {
                logger.debug("Un-persisting object {}!", source)
                store.remove(source)
            }
            return null
        }

        val name = method.heuristicName
        if (method.parameterCount == 0) {
            val getter = properties.find { it.name == name && it.type == method.returnType }?.getter
            if (getter != null) { //This is a getter! Time for magic
                logger.trace("Getter called for property {} in {}", name, source)
                return getter.invokeWithArguments()
            }
        } else if (method.parameterCount == 1 && !method.isVarArgs) {
            val setterProperty = properties.find { it.name == name && it.type == method.parameterTypes[0] }
            if (setterProperty != null) { //This is a setter! Time for magic
                logger.trace("Setter called for property {} in {}", name, source)
                val originalHash = manager.generateHash(source)
                val returnVal = setterProperty.setter.invokeWithArguments(args[0])
                store.update(originalHash, source, setterProperty.type to name)
                return returnVal
            }
        }
        return method.invoke(source, *args)
    }
}
