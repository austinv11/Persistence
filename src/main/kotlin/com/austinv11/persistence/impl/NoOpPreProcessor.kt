package com.austinv11.persistence.impl

import com.austinv11.persistence.PreProcessor

/**
 * This does no pre processing to provided data.
 */
class NoOpPreProcessor : PreProcessor {
    
    override fun getKey(): Byte = 0

    override fun pack(host: String, port: Int, input: ByteArray): ByteArray = input

    override fun consume(host: String, port: Int, input: ByteArray): ByteArray = input
}
