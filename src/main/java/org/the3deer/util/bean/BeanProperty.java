package org.the3deer.util.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represent a field that is a functional property of the component
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface BeanProperty {
    String name() default "";

    String description() default "";

    String[] values() default {};

    /**
     * Human-readable names for the values. 
     * If used with a method providing dynamic values, these will be the display labels.
     */
    String[] valueNames() default {};
}
