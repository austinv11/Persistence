package com.austinv11.persistence;

/**
 * This is implemented by persisted objects.
 * NOTE: DO NOT IMPLEMENT THIS YOURSELF.
 */
public interface Persisted {
	
	/**
	 * This un-persists this object.
	 */
	void unpersist();
}
