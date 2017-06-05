package com.austinv11.persistence.impl

import com.austinv11.persistence.Connection
import com.austinv11.persistence.internal.Payload
import com.austinv11.persistence.internal.TwoWaySocket
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

class ConnectionImpl(val socket: TwoWaySocket.CommunicationManager) : Connection {
    
    @Volatile internal var lastPing: Long = -1
    @Volatile internal var lastPingTime: Long = 0
    
    override fun getHost(): String {
        return socket.host
    }

    override fun getPort(): Int {
        return socket.port
    }

    override fun getLastPing(): Long {
        return lastPing
    }

    override fun ping(): Long {
        val originalTime = lastPingTime
        runBlocking {
            socket.send(Payload.Ping())
            while (lastPingTime == originalTime) {
                delay(1)
            }
        }
        return lastPing
    }

    override fun disconnect() {
        runBlocking { 
            socket.send(Payload.Kick())
        }
        socket.close()
    }
}
