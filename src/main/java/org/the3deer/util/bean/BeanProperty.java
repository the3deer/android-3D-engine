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
    /**
     * @return the name (id) of the property.
     */
    String name() default "";
    /**
     * @return an array with the list of allowed String values
     */
    String[] values() default {};
}
