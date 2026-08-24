package org.the3deer.util.bean;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Information about a bean property.
 *
 * @author andresoviedo
 * @author Gemini AI
 */
public class BeanPropertyInfo {
    private final String id;
    private final String fieldName;
    private final String beanName;
    private final String name;
    private String label;
    private String description;
    private List<String> options;
    private final Class<?> type;
    
    @JsonIgnore
    private final Field field;
    @JsonIgnore
    private final Method getter;
    @JsonIgnore
    private final Method setter;
    @JsonIgnore
    private final Method valuesMethod;

    private Object value;
    private List<CharSequence> optionLabels;

    public BeanPropertyInfo(String id, String fieldName, String beanName, String name, String description, String[] values, Class<?> type, Field field, Method getter, Method setter, Method valuesMethod) {
        this.id = id;
        this.fieldName = fieldName;
        this.beanName = beanName;
        this.name = name;
        this.label = name;
        this.description = description;
        this.options = values != null ? Arrays.asList(values) : null;
        this.type = type;
        this.field = field;
        this.getter = getter;
        this.setter = setter;
        this.valuesMethod = valuesMethod;
    }

    public String getId() {
        return id;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label != null ? label : name;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public Class<?> getType() {
        return type;
    }

    public Method getValuesMethod() {
        return valuesMethod;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public List<CharSequence> getOptionLabels() {
        return optionLabels;
    }

    public void setOptionLabels(List<CharSequence> optionLabels) {
        this.optionLabels = optionLabels;
    }

    /**
     * Returns the allowed values for this property.
     * It first tries to invoke the valuesMethod on the bean, 
     * then falls back to the static options.
     */
    public String[] getValues(Object bean) {
        if (valuesMethod != null) {
            try {
                Object result = valuesMethod.invoke(bean);
                if (result instanceof String[]) {
                    return (String[]) result;
                } else if (result instanceof List) {
                    List<?> list = (List<?>) result;
                    String[] arr = new String[list.size()];
                    for (int i = 0; i < list.size(); i++) {
                        Object item = list.get(i);
                        arr[i] = item != null ? item.toString() : null;
                    }
                    return arr;
                } else if (result instanceof Set){
                    Set<?> set = (Set<?>) result;
                    return set.toArray(new String[0]);
                }
            } catch (Exception e) {
                return getValues();
            }
        }
        return getValues();
    }

    /**
     * Returns the static allowed values for this property.
     */
    public String[] getValues() {
        return options != null ? options.toArray(new String[0]) : null;
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

    @Override
    public String toString() {
        return "BeanPropertyInfo{" +
                "id='" + id + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", beanName='" + beanName + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", options=" + options +
                ", type=" + type +
                ", field=" + field +
                ", getter=" + getter +
                ", setter=" + setter +
                ", valuesMethod=" + valuesMethod +
                '}';
    }
}
