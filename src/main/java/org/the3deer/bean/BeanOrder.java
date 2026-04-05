package org.the3deer.bean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BeanOrder {
    int order = 0;
    int order();
}
