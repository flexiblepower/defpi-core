package org.flexiblepower.service;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Maarten Kollenstart
 *
 * Annotation for the ConnectionFactory that indicates which interface
 * it will implement. Which will be used by AbstractService to find the 
 * correct Factory by reflection
 */
@Documented
@Target({ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Factory {
	public String interfaceName();
}
