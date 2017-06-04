package com.austinv11.persistence.test

import com.austinv11.persistence.Persisted
import com.austinv11.persistence.PersistenceManager

fun main(args: Array<String>) {
    val thing: Thing = PersistenceManager().persist(ThingImpl())
    thing.thing = "Hi"
    thing.thing
    (thing as Persisted).unpersist()
}

interface Thing {
    var thing: String
}

data class ThingImpl(val thing2: String? = null) : Thing {
    private var _thing = ""
    override var thing: String
        get() = _thing
        set(value) {_thing = value}

}
