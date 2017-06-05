package com.austinv11.persistence.internal

import com.austinv11.persistence.ConnectionSpy
import com.austinv11.persistence.Store
import com.austinv11.persistence.impl.ConnectionImpl
import com.austinv11.persistence.internal.TwoWaySocket.Hook
import com.austinv11.persistence.matchProperties
import kotlinx.coroutines.experimental.runBlocking

internal class SocketHook(override val socket: TwoWaySocket,
                          val spy: ConnectionSpy) : Hook {
    
    private lateinit var manager: TwoWaySocket.CommunicationManager
    private val connection: ConnectionImpl by lazy { 
        manager.connection
    }
    
    override fun hook(manager: TwoWaySocket.CommunicationManager) {
        this.manager = manager
    }

    override fun requestConnection(payload: Payload.Identify): Payload.Ok? {
        val value = spy.interceptConnectionRequest(payload.v, payload.t, payload.d)
        if (!value.didFail())
            return Payload.Ok(socket.context.version, d = value.value()) //TODO send INITIALIZE
        
        return null
    }

    override fun verify(payload: Payload.Ok): Boolean {
        return spy.interceptCompletedHandshake(payload.v, payload.t, payload.d)
    }

    override fun rejected() {
        spy.disconnected()
    }

    override fun pinged(payload: Payload.Ping) {
        val time = System.currentTimeMillis()
        val latency = time - payload.t
        spy.latencyCheck(latency)
        connection.lastPingTime = time
        connection.lastPing = latency
    }

    override fun ponged(payload: Payload.Pong) {
        val time = System.currentTimeMillis()
        val latency = time - payload.t
        spy.latencyCheck(latency)
        connection.lastPingTime = time
        connection.lastPing = latency
    }

    override fun kicked(payload: Payload.Kick) {
        spy.disconnected()
        socket.context.invalidate()
    }

    override fun initialize(payload: Payload.Initialize): Boolean {
        val data = (payload.d as InitializeValueWrapper).p
        val mapped = data.filterNotNull()
                .map { it as Map<String, Any?> }
                .map { it to socket.context.findType(it) }
                .map { socket.context.mapValues(it.first, it.second) }
        
        val first = mapped.first()
        if (socket.context.storeFor(first.javaClass).containsHash(socket.context.generateHash(first)))
            return false //All this was for nothing :(

        if (payload.d.r)
            runBlocking {
                manager.send(Payload.Initialize(d = InitializeValueWrapper(socket.context.stores.values.flatMap { it.collect() }.toTypedArray(), false)))
            }
        
        mapped.forEach { socket.context.persistQuietly(it) }
        return true
    }

    override fun created(payload: Payload.Creation): Boolean {
        if (!socket.context.stores.filter { it.value.containsHash(payload.h!!) }.isEmpty())
            return false //All this was for nothing :(

        val type = socket.context.findType(payload.d!!)
        val obj = socket.context.mapValues(payload.d, type)
        
        socket.context.persistQuietly(obj)
        return true
    }

    override fun changed(payload: Payload.Change): Boolean {
        val store: Store<in Any> = socket.context.stores.filter { it.value.containsHash(payload.oh!!) }.entries.firstOrNull()?.value as Store<in Any>? ?: return@changed false //All this was for nothing :(

        val obj = store.get(payload.oh!!)
        val pseudoProperty = payload.d!!.iterator().next() //We can assume a single pair
        val realProperty = socket.context.matchProperties(obj).find { it.name == pseudoProperty.key }!!
        
        realProperty.setter.invokeWithArguments(pseudoProperty.value)
        store.updateQuietly(payload.oh, obj!!)
        return true
    }

    override fun removed(payload: Payload.Removal): Boolean {
        val store = socket.context.stores.filter { it.value.containsHash(payload.h!!) }.entries.firstOrNull()?.toPair() ?: return false //All this was for nothing :(
        
        store.second.removeHashQuietly(payload.h!!)
        return true
    }
}
