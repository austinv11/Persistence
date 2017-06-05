package com.austinv11.persistence;

/**
 * This represents a connection to another node.
 */
public interface Connection {
	
	/**
	 * The host which the node is on.
	 * 
	 * @return The host.
	 */
	String getHost();
	
	/**
	 * The port which the node is on.
	 * 
	 * @return The port.
	 */
	int getPort();
	
	/**
	 * Gets the last recorded ping in ms to this connection.
	 * 
	 * @return The last recorded ping, or -1 if none are recorded.
	 */
	long getLastPing();
	
	/**
	 * Requests a ping to this node.
	 * NOTE: This blocks until a PONG packet is received.
	 * 
	 * @return The ping in ms.
	 */
	long ping();
	
	/**
	 * Severs the connection to this node.
	 */
	void disconnect();
}
