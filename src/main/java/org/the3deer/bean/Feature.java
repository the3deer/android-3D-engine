package org.the3deer.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents an application feature
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PACKAGE})
public @interface Feature {
    /**
     * @return the unique identifier of the feature.
     */
    String name() default "";
    /**
     * @return the category of the feature.
     */
    String category() default "";
    /**
     * @return true if the feature is experimental.
     */
    boolean experimental() default false;
}
