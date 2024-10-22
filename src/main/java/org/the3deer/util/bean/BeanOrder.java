package org.the3deer.util.bean;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface BeanOrder {
    int order = 0;
    int order();
}
