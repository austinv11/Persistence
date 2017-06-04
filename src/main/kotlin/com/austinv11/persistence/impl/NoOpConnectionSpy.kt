package com.austinv11.persistence.impl

import com.austinv11.persistence.ConnectionSpy
import com.austinv11.persistence.FailableValue
import com.austinv11.persistence.logger

class NoOpConnectionSpy : ConnectionSpy {
    
    override fun interceptConnectionRequest(v: Int?, time: Long, data: MutableMap<String, Any>?): FailableValue<MutableMap<String, Any>> {
        return FailableValue.succeeded(null)
    }

    override fun interceptCompletedHandshake(v: Int?, time: Long, data: MutableMap<String, Any>?): Boolean {
        return true
    }
    
    override fun disconnected() {}

    override fun latencyCheck(diff: Long) {
        logger.debug("Measured latency at around {}ms", diff)
    }
}
