package com.austinv11.persistence;

import javax.annotation.Nonnull;

/**
 * This represents a storeFor factory.
 */
@FunctionalInterface
public interface StoreFactory {
	
	/**
	 * Requests a {@link Store} object for the provided type.
	 * 
	 * @param manager The manager requesting a storeFor.
	 * @param type The storeFor type to build.
	 * @return The storeFor.
	 */
	@Nonnull <T> Store<T> buildStore(@Nonnull PersistenceManager manager, @Nonnull Class<T> type);
}
