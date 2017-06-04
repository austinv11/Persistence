package com.austinv11.persistence;

import javax.annotation.Nonnull;

/**
 * This represents a store factory.
 */
@FunctionalInterface
public interface StoreFactory {
	
	/**
	 * Requests a {@link Store} object for the provided type.
	 * 
	 * @param manager The manager requesting a store.
	 * @param type The store type to build.
	 * @return The store.
	 */
	@Nonnull <T> Store<T> buildStore(@Nonnull PersistenceManager manager, @Nonnull Class<T> type);
}
