package org.the3deer.util.bean;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Information about a bean property.
 *
 * @author andresoviedo
 * @author Gemini AI
 */
public class BeanPropertyInfo {
    private final String id;
    private final String beanName;
    private final String name;
    private final String[] values;
    private final Class<?> type;
    private final Field field;
    private final Method getter;
    private final Method setter;
    private final Method valuesMethod;

    public BeanPropertyInfo(String id, String beanName, String name, String[] values, Class<?> type, Field field, Method getter, Method setter, Method valuesMethod) {
        this.id = id;
        this.beanName = beanName;
        this.name = name;
        this.values = values;
        this.type = type;
        this.field = field;
        this.getter = getter;
        this.setter = setter;
        this.valuesMethod = valuesMethod;
    }

    public String getId() {
        return id;
    }

    public String getBeanName() {
        return beanName;
    }

    public String getName() {
        return name;
    }

    /**
     * Resolve the label for this property.
     * @param context The context
     * @return The localized label or null if not found
     */
    public String resolveLabel(Context context) {
        if (beanName != null && !beanName.isEmpty()) {
            String propertyName = (name != null && !name.isEmpty()) ? name : id;
            int resId = context.getResources().getIdentifier("property_" + beanName + "_" + propertyName + "_label", "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
            // fallback to user's example naming convention
            resId = context.getResources().getIdentifier("bean_" + beanName + "_" + propertyName + "_label", "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        }
        return null;
    }

    /**
     * Resolve the description for this property.
     * @param context The context
     * @return The localized description or null if not found
     */
    public String resolveDescription(Context context) {
        if (beanName != null && !beanName.isEmpty()) {
            String propertyName = (name != null && !name.isEmpty()) ? name : id;
            int resId = context.getResources().getIdentifier("property_" + beanName + "_" + propertyName + "_description", "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        }
        return null;
    }

    /**
     * Resolve labels for a list of values.
     * @param context The context
     * @return The localized labels or null if not found
     */
    public String[] resolveValueLabels(Context context) {
        if (beanName != null && !beanName.isEmpty()) {
            String propertyName = (name != null && !name.isEmpty()) ? name : id;
            int resId = context.getResources().getIdentifier("property_" + beanName + "_" + propertyName + "_values_descriptions", "array", context.getPackageName());
            if (resId != 0) {
                return context.getResources().getStringArray(resId);
            }
            
            // fallback to the previous naming if descriptions not found
            resId = context.getResources().getIdentifier("property_" + beanName + "_" + propertyName + "_values", "array", context.getPackageName());
            if (resId != 0) {
                return context.getResources().getStringArray(resId);
            }
        }
        return null;
    }

    /**
     * Resolve a label for a specific value.
     * @param context The context
     * @param valueId The value ID
     * @return The localized label or the valueId itself
     */
    public String resolveValueLabel(Context context, String valueId) {
        if (beanName != null && !beanName.isEmpty()) {
            String propertyName = (name != null && !name.isEmpty()) ? name : id;
            // sanitize valueId for resource lookup (e.g. default.renderer -> default_renderer)
            String sanitizedValueId = valueId.replace(".", "_");
            int resId = context.getResources().getIdentifier("value_" + beanName + "_" + propertyName + "_" + sanitizedValueId, "string", context.getPackageName());
            if (resId != 0) {
                return context.getString(resId);
            }
        }
        return valueId;
    }

    public String[] getValues() {
        return values;
    }

    public Class<?> getType() {
        return type;
    }

    public Method getValuesMethod() {
        return valuesMethod;
    }

    public String[] getValues(Object bean) {
        if (valuesMethod != null) {
            try {
                return (String[]) valuesMethod.invoke(bean);
            } catch (Exception e) {
                return values;
            }
        }
        return values;
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

    @NonNull
    @Override
    public String toString() {
        return "BeanPropertyInfo{" +
                "id='" + id + '\'' +
                ", beanName='" + beanName + '\'' +
                ", name='" + name + '\'' +
                ", values=" + Arrays.toString(values) +
                ", type=" + type +
                ", field=" + field +
                ", getter=" + getter +
                ", setter=" + setter +
                ", valuesMethod=" + valuesMethod +
                '}';
    }
}
