package com.austinv11.persistence.internal

import com.austinv11.persistence.OpCode
import com.austinv11.persistence.PersistenceManager
import com.austinv11.persistence.logger
import com.austinv11.persistence.map
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker
import org.msgpack.core.buffer.MessageBuffer
import org.msgpack.value.ValueType
import java.util.*

private typealias RefArray = java.lang.reflect.Array
internal val ops = OpCode.values()
internal const val WRAPPER_KEY = "p"
internal const val RESPOND_KEY = "r"

internal fun PersistenceManager.pack(payload: Payload): MessageBuffer {
    val packer = MessagePack.newDefaultBufferPacker()
    
    val payloadMap = payload.toMap()
    
    packer.packMapHeader(payloadMap.size + (if (payload.d == null) 0 else 1))
    payloadMap.forEach { k, v -> 
        packer.insert(k, this)
        packer.insert(v, this)
    }
    
    if (payload.d != null) {
        packer.insert("d", this)
        
        if (payload.d !is InitializeValueWrapper) {
            packer.insert(payload.d, this)
        } else {
            packer.packMapHeader(2)
            packer.insert(RESPOND_KEY, this)
            packer.insert(payload.d.r, this)
            packer.insert(WRAPPER_KEY, this)
            packer.packArrayHeader(payload.d.p.size)
            payload.d.p.forEach { 
                packer.insert(it!!.map(this), this)
            }
        }
    }
    
    packer.flush()
    return packer.toMessageBuffer().also { packer.close() }
}

internal fun PersistenceManager.unpack(bytes: ByteArray): Payload {
    val unpacker = MessagePack.newDefaultUnpacker(bytes)
    
    if (!unpacker.nextFormat.valueType.isMapType) throw InputMismatchException("Expected map, did not get one!")
    
    var v: Int? = null
    var t: Long = 0
    var op: Int = -1
    var d: Map<String, Any?>? = null
    var h: Long? = null
    var oh: Long? = null
    
    for (i in 0..(unpacker.unpackMapHeader() - 1)) {
        if (!unpacker.nextFormat.valueType.isStringType) throw InputMismatchException("Expected string key, did not get one!")
        
        val key = unpacker.unpackString()
        when (key) {
            "v" -> {
                v = unpacker.unpackInt()
            }
            "t" -> {
                t = unpacker.unpackLong()
            }
            "op" -> {
                op = unpacker.unpackInt()
            }
            "d" -> {
                d = unpacker.consumeMapFully(unpacker.unpackMapHeader(), this)
            }
            "h" -> {
                h = unpacker.unpackLong()
            }
            "oh" -> {
                oh = unpacker.unpackLong()
            }
        }
    }
    
    val payload: Payload
    
    when(ops[op]) {
        OpCode.IDENTIFY -> {
            payload = Payload.Identify(v, t, d)
        }
        OpCode.OK -> {
            payload = Payload.Ok(v, t, d)
        }
        OpCode.REJECTION -> {
            payload = Payload.Rejection(t)
        }
        OpCode.PING -> {
            payload = Payload.Ping(t)
        }
        OpCode.PONG -> {
            payload = Payload.Pong(t)
        }
        OpCode.KICK -> {
            payload = Payload.Kick(t)
        }
        OpCode.INITIALIZE -> {
            payload = Payload.Initialize(t, InitializeValueWrapper((d!![WRAPPER_KEY] as List<Any?>).toTypedArray(), d[RESPOND_KEY] as Boolean))
        }
        OpCode.CREATION -> {
            payload = Payload.Creation(t, d!!, h!!)
        }
        OpCode.CHANGE -> {
            payload = Payload.Change(t, d!!, h!!, oh!!)
        }
        OpCode.REMOVAL -> {
            payload = Payload.Removal(t, h!!)
        }
    }
    
    return payload
}

internal fun MessageUnpacker.consumeMapFully(mapLength: Int, context: PersistenceManager): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>()
    for (i in 0..(mapLength-1)) {
        map[unpackString()] = unpackAny(context)
    }
    return map
}

internal fun MessageUnpacker.unpackAny(context: PersistenceManager): Any? {
    val formatType = nextFormat.valueType
    when (formatType!!) {
        ValueType.NIL -> {
            unpackNil()
            return null
        }
        ValueType.BOOLEAN -> {
            return unpackBoolean()
        }
        ValueType.INTEGER -> {
            return unpackInt()
        }
        ValueType.FLOAT -> {
            return unpackFloat()
        }
        ValueType.STRING -> {
            return unpackString()
        }
        ValueType.BINARY -> {
            this.skipValue()
            logger.debug("Ignoring binary value...")
            return null
        }
        ValueType.ARRAY -> {
            val arrayLen = unpackArrayHeader()
            val list = mutableListOf<Any?>()
            for (i in 0..(arrayLen-1)) {
                list += unpackAny(context)
            }
            return list
        }
        ValueType.MAP -> {
            val mapLen = unpackMapHeader()
            val map = mutableMapOf<Any?, Any?>()
            for (i in 0..(mapLen-1)) {
                map[unpackAny(context)] = unpackAny(context)
            }
            return map
        }
        ValueType.EXTENSION -> {
            val type = unpackExtensionTypeHeader()
            val transformer = context.customDataTransformers.first { it.type() == type.type }
            return transformer.readFromBytes(readPayload(type.length))
        }
    }
}

internal fun MessageBufferPacker.insert(value: Any?, context: PersistenceManager) {
    if (value == null) {
        this.packNil()
    } else {
        when (value) {
            is Byte -> {
                this.packByte(value)
            }
            is Char -> {
                this.packByte(value as Byte)
            }
            is Short -> {
                this.packShort(value)
            }
            is Float -> {
                this.packFloat(value)
            }
            is Int -> {
                this.packInt(value)
            }
            is Double -> {
                this.packDouble(value)
            }
            is Long -> {
                this.packLong(value)
            }
            is String -> {
                this.packString(value)
            }
            is Boolean -> {
                this.packBoolean(value)
            }
            is Enum<*> -> {
                this.packInt(value.ordinal)
            }
            is List<*> -> {
                this.packArrayHeader(value.size)
                value.forEach {
                    this.insert(it, context)
                }
            }
            value.javaClass.isArray -> {
                val length = RefArray.getLength(value)
                this.packArrayHeader(length)
                for (i in 0..(length-1)) {
                    this.insert(RefArray.get(value, i), context)   
                }
            }
            is Map<*, *> -> {
                this.packMapHeader(value.size)
                value.forEach { k, v -> 
                    this.insert(k, context)
                    this.insert(v, context)
                }
            }
            else -> {
                val transformer = context.customDataTransformers.firstOrNull { it.canAccept(value.javaClass) } ?: throw InputMismatchException("Unexpected data type ${value.javaClass}!")
                val bytes = transformer.writeToBytes(value)
                this.packExtensionTypeHeader(transformer.type(), bytes.size)
                this.writePayload(bytes)
            }
        }
    }
}

data class InitializeValueWrapper(val p: Array<Any?>,
                                  val r: Boolean) : HashMap<String, Any?>()

sealed class Payload(val v: Int? = null, 
                     val t: Long = System.currentTimeMillis(),
                     val op: Int,
                     val d: Map<String, Any?>? = null,
                     val h: Long? = null,
                     val oh: Long? = null) {
    
    class Identify(v: Int? = null,
                   t: Long = System.currentTimeMillis(),
                   d: Map<String, Any?>? = null) : Payload(v, t, op = OpCode.IDENTIFY.ordinal, d = d)
    
    class Ok(v: Int? = null,
             t: Long = System.currentTimeMillis(),
             d: Map<String, Any?>? = null) : Payload(v, t, op = OpCode.OK.ordinal, d = d)
    
    class Rejection(t: Long = System.currentTimeMillis()) : Payload(t = t, op = OpCode.REJECTION.ordinal)
    
    class Ping(t: Long = System.currentTimeMillis()) : Payload(t = t, op = OpCode.PING.ordinal)
    
    class Pong(t: Long = System.currentTimeMillis()) : Payload(t = t, op = OpCode.PONG.ordinal)
    
    class Kick(t: Long = System.currentTimeMillis()) : Payload(t = t, op = OpCode.KICK.ordinal)
    
    class Initialize(t: Long = System.currentTimeMillis(),
                     d: InitializeValueWrapper) : Payload(t = t, op = OpCode.INITIALIZE.ordinal, d = d)
    
    class Creation(t: Long = System.currentTimeMillis(),
                   d: Map<String, Any?>,
                   h: Long) : Payload(t = t, op = OpCode.CREATION.ordinal, d = d, h = h)
    
    class Change(t: Long = System.currentTimeMillis(),
                 d: Map<String, Any?>,
                 h: Long,
                 oh: Long) : Payload(t = t, op = OpCode.CHANGE.ordinal, d = d, h = h, oh = oh)
    
    class Removal(t: Long = System.currentTimeMillis(),
                  h: Long) : Payload(t = t, op = OpCode.REMOVAL.ordinal, h = h)
    
    fun toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        
        if (v != null) map["v"] = v
        map["t"] = t
        map["op"] = op
//        if (d != null) map["d"] = d ignore d here, process it in pack() instead
        if (h != null) map["h"] = h
        if (oh != null) map["oh"] = oh
        
        return map
    }
}
