package com.austinv11.persistence;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * This represents a property setter, this means the method MUST accept one parameter matching the property type.
 * NOTE: This must have a corresponding {@link Getter} method.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Setter {
	
	/**
	 * The property name that this setter corresponds to, if this is an empty string then the property name will attempt
	 * to be inferred via heuristic analysis.
	 */
	@Nonnull String property() default "";
}
