package com.austinv11.persistence.internal

import com.austinv11.persistence.*
import com.austinv11.persistence.impl.ConnectionImpl
import com.austinv11.persistence.impl.NoOpConnectionSpy
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.yield
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.CopyOnWriteArrayList

class TwoWaySocket {
    
    internal val server: ServerSocket
    internal val connections = CopyOnWriteArrayList<CommunicationManager>()
    internal val context: PersistenceManager
    internal val allowedConnections: Int
    internal val spy: ConnectionSpy

    constructor(context: PersistenceManager, port: Int, allowedConnections: Int, spy: ConnectionSpy = NoOpConnectionSpy()) {
        if (allowedConnections < 1) throw IllegalArgumentException("Need a least one allowed connection!")
        
        this.context = context
        this.allowedConnections = allowedConnections
        this.spy = spy
        server = ServerSocket(port)
        waitForConnection()
    }
    
    fun waitForConnection() {
        launch(newSingleThreadContext("Connection Waiter")) { 
            while(connections.size < allowedConnections) {
                val socket = server.accept()
                connections += CommunicationManager(socket, SocketHook(this@TwoWaySocket, spy), socket.inetAddress.hostName, socket.port)
            }
            yield()
        }
    }

    suspend fun connectTo(host: String, port: Int, metadata: Map<String, Any?>? = null) {
        val socket = Socket(host, port)
        val manager = CommunicationManager(socket, SocketHook(this, spy), host, port)
        manager.send(Payload.Identify(context.version, d = metadata))
        connections += manager
    }
    
    inner class CommunicationManager(val socket: Socket, 
                                     val hook: Hook,
                                     val host: String,
                                     val port: Int,
                                     val manager: PersistenceManager = context) : AutoCloseable {

        val input = socket.getInputStream()!!
        val output = socket.getOutputStream()!!
        val connection = ConnectionImpl(this)
        
        init {
            hook.hook(this)
            launch(CommonPool) {
                while (!socket.isClosed) {
                    try {
                        val typeHeader = input.read()
                        if (typeHeader != manager.processor.key.toInt())
                            throw Exception("PreProcessor mismatch! Expected ${manager.processor.key.toInt()} but received $typeHeader")
                        
                        val lenHeader = ByteArray(4)
                        input.read(lenHeader)
                        var len = 0
                        for (i in 0..(lenHeader.size - 1)) {
                            len = len shl 8
                            len = len xor (lenHeader[i].toInt() and mask_int)
                        }
                        val data = ByteArray(len)
                        input.read(data)
                        val processed = manager.processor.consume(host, port, data)
                        receive(manager.unpack(decompress(processed)))
                    } catch (e: Exception) {
                        logger.error("Exception caught processing data, cleanly closing connection to $host:$port...", e)
                        use {
                            send(Payload.Kick())
                        }
                    }
                }
            }
        }
        
        suspend fun send(payload: Payload) {
            try {
                val dataArray = manager.processor.pack(host, port, compress(manager.pack(payload)))
                var len = dataArray.size
                val len1 = len and mask_int
                len = len shr 8
                val len2 = len and mask_int
                len = len shr 8
                val len3 = len and mask_int
                len = len shr 8
                val len4 = len and mask_int
                output.write(manager.processor.key.toInt())
                output.write(byteArrayOf(len4.toByte(), len3.toByte(), len2.toByte(), len1.toByte()))
                output.write(dataArray)
                output.flush()
            } catch (e: Exception) {
                logger.error("Exception caught sending data, harshly closing connection to $host:$port...", e)
                close()
            }
        }
        
        suspend fun receive(payload: Payload) {
            when (ops[payload.op]) {
                OpCode.IDENTIFY -> {
                    send(hook.requestConnection(payload as Payload.Identify) ?: Payload.Rejection())
                }
                OpCode.OK -> {
                    if (!hook.verify(payload as Payload.Ok))
                        send(Payload.Rejection())
                    else
                        send(Payload.Initialize(d = InitializeValueWrapper(manager.stores.values.flatMap { it.collect() }.toTypedArray(), true)))
                }
                OpCode.REJECTION -> {
                    hook.rejected()
                    close()
                }
                OpCode.PING -> {
                    hook.pinged(payload as Payload.Ping)
                    send(Payload.Pong())
                }
                OpCode.PONG -> {
                    hook.ponged(payload as Payload.Pong)
                }
                OpCode.KICK -> {
                    hook.kicked(payload as Payload.Kick)
                    close()
                }
                OpCode.INITIALIZE -> {
                    if (hook.initialize(payload as Payload.Initialize)) {
                        connections.filterNot { it == this }.forEach { it.send(payload) }
                    }
                }
                OpCode.CREATION -> {
                    if (hook.created(payload as Payload.Creation))
                        connections.filterNot { it == this }.forEach { it.send(payload) }
                }
                OpCode.CHANGE -> {
                    if (hook.changed(payload as Payload.Change))
                        connections.filterNot { it == this }.forEach { it.send(payload) }
                }
                OpCode.REMOVAL -> {
                    if (hook.removed(payload as Payload.Removal))
                        connections.filterNot { it == this }.forEach { it.send(payload) }
                }
            }
        }

        override fun close() {
            input.close()
            output.close()
            socket.close()
            
            val oldSize = connections.size
            connections.remove(this)
            if (oldSize == allowedConnections) //Wait loop was previously terminated so we must restart it
                waitForConnection()
        }
    }
    
    interface Hook {
        
        val socket: TwoWaySocket
        
        fun hook(manager: CommunicationManager)
        
        fun requestConnection(payload: Payload.Identify): Payload.Ok?
        
        fun verify(payload: Payload.Ok): Boolean
        
        fun rejected()
        
        fun pinged(payload: Payload.Ping)

        fun ponged(payload: Payload.Pong)
        
        fun kicked(payload: Payload.Kick)
        
        fun initialize(payload: Payload.Initialize): Boolean
        
        fun created(payload: Payload.Creation): Boolean
        
        fun changed(payload: Payload.Change): Boolean
        
        fun removed(payload: Payload.Removal): Boolean
    }
}
