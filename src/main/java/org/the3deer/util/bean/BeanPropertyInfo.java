package org.the3deer.util.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BeanPropertyInfo {
    private final String name;
    private final String description;
    private final String[] values;
    private final String[] valueNames;
    private final Class<?> type;
    private final Field field;
    private final Method getter;
    private final Method setter;
    private final Method valuesMethod;

    public BeanPropertyInfo(String name, String description, String[] values, String[] valueNames, Class<?> type, Field field, Method getter, Method setter, Method valuesMethod) {
        this.name = name;
        this.description = description;
        this.values = values;
        this.valueNames = valueNames;
        this.type = type;
        this.field = field;
        this.getter = getter;
        this.setter = setter;
        this.valuesMethod = valuesMethod;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String[] getValues() {
        return values;
    }

    public String[] getValueNames() {
        return valueNames;
    }

    public Class<?> getType() {
        return type;
    }

    public Method getValuesMethod() {
        return valuesMethod;
    }

    public Object getValue(Object bean) throws Exception {
        if (getter != null) {
            return getter.invoke(bean);
        } else if (field != null) {
            field.setAccessible(true);
            return field.get(bean);
        }
        return null;
    }

    public void setValue(Object bean, Object value) throws Exception {
        if (setter != null) {
            setter.invoke(bean, value);
        } else if (field != null) {
            field.setAccessible(true);
            field.set(bean, value);
        }
    }
}
