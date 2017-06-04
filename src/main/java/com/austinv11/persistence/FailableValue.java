package com.austinv11.persistence;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This represents a value which may or may not signal success.
 */
public interface FailableValue<T> {
	
	/**
	 * This returns true if the value does represent a failure.
	 * 
	 * @return True if failed, false if otherwise.
	 */
	boolean didFail();
	
	/**
	 * This returns the value held by this value. It may or may not exist. Additionally, nullability is not an 
	 * indicator of success, {@link #didFail()} is instead.
	 * 
	 * @return The nullable value which is held.
	 */
	@Nullable T value();
	
	/**
	 * This generates a {@link FailableValue} which is in a failed state.
	 * 
	 * @return The failed, {@link FailableValue}.
	 */
	@Nonnull static <T> FailableValue<T> failed() {
		return new FailableValue<T>() {
			@Override
			public boolean didFail() {
				return true;
			}
			
			@Nullable
			@Override
			public T value() {
				return null;
			}
		};
	}
	
	/**
	 * This generates a {@link FailableValue} which is in a success state.
	 *
	 * @param obj The object to put in the value (nullable).
	 * @return The succeeded, {@link FailableValue}.
	 */
	@Nonnull static <T> FailableValue<T> succeeded(@Nullable T obj) {
		return new FailableValue<T>() {
			@Override
			public boolean didFail() {
				return false;
			}
			
			@Nullable
			@Override
			public T value() {
				return obj;
			}
		};
	}
}
