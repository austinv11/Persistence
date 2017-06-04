package com.austinv11.persistence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This represents a custom data type used to convert objects to/from Java types from/to msgpack.
 */
public interface ExternalData<T> {
	
	/**
	 * This is called to serialize data to a byte array to be transmitted.
	 * 
	 * @param obj The object to write.
	 * @return The serialized data.
	 */
	@Nonnull byte[] writeToBytes(@Nonnull T obj);
	
	/**
	 * This is called to deserialize data from a received byte array.
	 * 
	 * @param data The data to read.
	 * @return The deserialized data.
	 */
	@Nullable T readFromBytes(@Nonnull byte[] data);
	
	/**
	 * This gets an arbitrary byte to match this custom data type. This MUST be unique from all other external
	 * data formats being used.
	 * 
	 * @return The type.
	 */
	byte type();
	
	/**
	 * Return the class type that this accepts.
	 * 
	 * @return The class which this accepts.
	 */
	@Nonnull Class<T> accepts();
	
	/**
	 * Return whether the class which is returned by {@link #accepts()} is strict. This means that it only accepts the
	 * exact class returned and not any subclass.
	 * 
	 * @return True if this is strict, false if otherwise.
	 */
	default boolean isStrict() {
		return false;
	}
	
	/**
	 * Returns whether this data can accept a given class.
	 * 
	 * @param clazz The class to check.
	 * @return True if accepted, false if otherwise.
	 */
	default boolean canAccept(@Nonnull Class<?> clazz) {
		if (isStrict()) {
			return clazz.equals(accepts());
		} else {
			return accepts().isAssignableFrom(clazz);
		}
	}
}
