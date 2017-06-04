package com.austinv11.persistence.internal

import com.austinv11.persistence.ConnectionSpy
import com.austinv11.persistence.internal.TwoWaySocket.Hook

internal class SocketHook(override val socket: TwoWaySocket,
                          val spy: ConnectionSpy) : Hook {
    
    private lateinit var manager: TwoWaySocket.CommunicationManager
    
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
        spy.latencyCheck(System.currentTimeMillis() - payload.t)
    }

    override fun ponged(payload: Payload.Pong) {
        spy.latencyCheck(System.currentTimeMillis() - payload.t)
    }

    override fun kicked(payload: Payload.Kick) {
        spy.disconnected()
        socket.context.invalidate()
    }

    override fun initialize(payload: Payload.Initialize): Boolean {
        val data = payload.d!![WRAPPER_KEY] as List<Any?>
        val mapped = data.filterNotNull()
                .map { it as Map<String, Any?> }
                .map { it to socket.context.findType(it) }
                .map { socket.context.mapValues(it.first, it.second) }
        
        val first = mapped.first()
        if (socket.context.store(first.javaClass).containsHash(socket.context.generateHash(first)))
            return false //All this was for nothing :(
        
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
        val store = socket.context.stores.filter { it.value.containsHash(payload.oh!!) }.entries.firstOrNull()?.toPair() ?: return false //All this was for nothing :(

        socket.context.mapValues(payload.d!!, store.first, store.second.get(payload.oh!!))
        return true
    }

    override fun removed(payload: Payload.Removal): Boolean {
        val store = socket.context.stores.filter { it.value.containsHash(payload.h!!) }.entries.firstOrNull()?.toPair() ?: return false //All this was for nothing :(
        
        store.second.removeHashQuietly(payload.h!!)
        return true
    }
}
