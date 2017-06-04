package com.austinv11.persistence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * This represents an entry-point for listening to communications in order to read/add metadata.
 */
public interface ConnectionSpy {
	
	/**
	 * This is called when an IDENTIFY payload is received.
	 * 
	 * @param v The optional custom version metadata.
	 * @param time The time when this was sent.
	 * @param data The optional custom metadata.
	 * @return The result of this attempt, a failed value will cause the connection to terminate whereas a successful
	 * one does not and then forwards the provided metadata to the new client.
	 */
	@Nonnull FailableValue<Map<String, Object>> interceptConnectionRequest(@Nullable Integer v, long time, @Nullable Map<String, Object> data);
	
	/**
	 * This is called when an OK payload is received.
	 *
	 * @param v The optional custom version metadata.
	 * @param time The time when this was sent.
	 * @param data The optional custom metadata.
	 * @return The result of this attempt, a false value will cause the connection to terminate whereas a successful
	 * one does not.
	 */
	boolean interceptCompletedHandshake(@Nullable Integer v, long time, @Nullable Map<String, Object> data);
	
	/**
	 * This is called when a REJECTED or KICK payload is received.
	 */
	void disconnected();
	
	/**
	 * This is called when the nodes perform a latency check (ping/pong).
	 * 
	 * @param diff The estimated latency in ms.
	 */
	void latencyCheck(long diff);
}
