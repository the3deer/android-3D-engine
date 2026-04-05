package org.the3deer.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * BeanFactory Pre-Processor. Allow implementing a method that returns a <code>Map&lt;String,Object&gt;</code>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BeanFactory {
}
