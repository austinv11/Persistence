package com.austinv11.persistence;

import javax.annotation.Nonnull;

/**
 * This represents a custom preprocessor which mutates data before consumption and mutates data before packaging.
 */
public interface PreProcessor {
	
	/**
	 * A unique key for this pre processor type.
	 * 
	 * @return The key for the pre-processor.
	 */
	byte getKey();
	
	/**
	 * Called to pack data to be sent.
	 * 
	 * @param host The host this data belongs to.
	 * @param port The port from the provided host this data belongs to.
	 * @param input The data to pack.
	 * @return The packed data.
	 */
	@Nonnull
	byte[] pack(@Nonnull String host, int port, @Nonnull byte[] input);
	
	/**
	 * Called to consume data received.
	 * 
	 * @param host The host this data belongs to.
	 * @param port The port from the provided host this data belongs to.
	 * @param input The data to consume.
	 * @return The processed data.
	 */
	@Nonnull
	byte[] consume(@Nonnull String host, int port, @Nonnull byte[] input);
}
