package com.austinv11.persistence.impl

import com.austinv11.persistence.PreProcessor
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal const val KEY_LENGTH: Int = 16
internal const val SALT_LENGTH: Int = 32

/**
 * This encrypts data via a AES with the provided key.
 */
class EncryptedPreProcessor(val key: String) : PreProcessor {
    
    private val secretKey = SecretKeySpec(MessageDigest.getInstance("SHA-1").digest(key.toByteArray()).copyOf(KEY_LENGTH), "AES")
    private val encryptionCipher
        get() = Cipher.getInstance("AES/ECB/PKCS5Padding").apply { this.init(Cipher.ENCRYPT_MODE, secretKey) }
    private val decryptionCipher
        get() = Cipher.getInstance("AES/ECB/PKCS5Padding").apply { this.init(Cipher.DECRYPT_MODE, secretKey) }
    private val connectionMetadata = ConcurrentHashMap<String, ConnectionMetadata>()
    
    override fun getKey(): Byte = 1

    override fun pack(host: String, port: Int, input: ByteArray): ByteArray {
        if (!connectionMetadata.containsKey("host:$port")) { //If there isn't metadata on this end, this is a client since its the one to pack first
            connectionMetadata["host:$port"] = ConnectionMetadata(false)
        }
        
        val metadata = connectionMetadata["host:$port"]!!
        synchronized(metadata) {
            if (!metadata.didCompleteHandshake) { //No need to do special handling if the handshake is finished
                if (metadata.isServerSide) { //When this is server side, we are likely packing the OK payload (if not, who cares? its a failed connection)
                    metadata.didCompleteHandshake = true //Mark completed handshake server side
                    return ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this); metadata.salt = this } + input //Create a random salt and prepend it to the (raw) payload since there's no sensitive info yet
                } else {
                    return input //This is the client side, so we don't have the salt yet, so no processing yet
                }
            }


            return encryptionCipher.doFinal(metadata.salt!! + input) //Normal operation, encrypt the input with the prepended salt
        }
    }

    override fun consume(host: String, port: Int, input: ByteArray): ByteArray {
        if (!connectionMetadata.containsKey("host:$port")) { //If there isn't metadata on this end, this is a server since its the one to consume first
            connectionMetadata["host:$port"] = ConnectionMetadata(true)
        }

        val metadata = connectionMetadata["host:$port"]!!
        synchronized(metadata) {
            if (!metadata.didCompleteHandshake) { //No need to do special handling if the handshake is finished
                if (!metadata.isServerSide) { //When client side, we must take the salt
                    metadata.salt = input.copyOfRange(0, SALT_LENGTH) //Strip salt and store it
                    metadata.didCompleteHandshake = true //Mark completed handshake client side
                    return input.copyOfRange(SALT_LENGTH, input.size) //Strip the salt from the input and pass it forward (it still isn't encrypted at this point)
                } else {
                    return input //When server side and we did not complete handshake, no need for processing since its unencrypted and has no salt yet
                }
            }

            return decryptionCipher.doFinal(input).copyOfRange(SALT_LENGTH, input.size) //Normal operation, decrypt the input and remove the salt
        }
    }
    
    data class ConnectionMetadata(@Volatile var isServerSide: Boolean,
                                  @Volatile var didCompleteHandshake: Boolean = false,
                                  @Volatile var salt: ByteArray? = null)
}
