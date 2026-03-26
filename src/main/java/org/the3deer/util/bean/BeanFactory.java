package org.the3deer.util.bean;

import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A simple CDI (Component Dependency Injection) implementation
 */
public class BeanFactory {

    private final Logger LOG = LogManager.getLogManager().getLogger(this.getClass().getName());

    private static final Integer STATUS_INSTANTIATED = 0;
    private static final Integer STATUS_CONFIGURED = 1;
    private static final Integer STATUS_INITIALIZED = 2;

    private final Map<String, Class<?>> definitions = new TreeMap<>();
    private final Map<String, Object> beans = new TreeMap<>();
    private final Map<String, Integer> status = new HashMap<>();

    private boolean definitionsUpdated;
    private boolean beansUpdated;
    private boolean initialized;

    private BeanFactory() {
    }

    public static BeanFactory getInstance() {
        BeanFactory instance = new BeanFactory();
        instance.beans.put("beanFactory", instance);
        instance.status.put("beanFactory", STATUS_INITIALIZED);
        return instance;
    }

    public Map<String, Object> getBeans() {
        return Collections.unmodifiableMap(beans);
    }

    public Map<String, BeanPropertyInfo> getProperties(Object bean) {
        Map<String, BeanPropertyInfo> ret = new TreeMap<>();
        Class<?> currentClass = bean.getClass();
        while (currentClass != null) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(BeanProperty.class)) {
                    BeanProperty ann = field.getAnnotation(BeanProperty.class);
                    String name = field.getName();
                    Method valuesMethod = findBeanValuesMethod(bean.getClass(), name);
                    
                    // find setter/getter if they exist
                    String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
                    Method setter = findMethod(bean.getClass(), "set" + capitalized, field.getType());
                    Method getter = findMethod(bean.getClass(), "get" + capitalized);
                    if (getter == null && (field.getType() == boolean.class || field.getType() == Boolean.class)) {
                        getter = findMethod(bean.getClass(), "is" + capitalized);
                    }

                    ret.put(name, new BeanPropertyInfo(name, ann.description(), ann.values(), ann.valueNames(), field.getType(), field, getter, setter, valuesMethod));
                }
            }
            for (Method method : currentClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(BeanProperty.class)) {
                    BeanProperty ann = method.getAnnotation(BeanProperty.class);
                    String methodName = method.getName();
                    String name = ann.name().isEmpty() ? 
                        (methodName.startsWith("get") || methodName.startsWith("set") ? 
                            methodName.substring(3, 4).toLowerCase() + methodName.substring(4) : 
                            (methodName.startsWith("is") ? methodName.substring(2, 3).toLowerCase() + methodName.substring(3) : methodName)) 
                        : ann.name();

                    if (ret.containsKey(name)) continue;

                    String capitalized = name.substring(0, 1).toUpperCase() + name.substring(1);
                    Method getter = findMethod(bean.getClass(), "get" + capitalized);
                    if (getter == null) getter = findMethod(bean.getClass(), "is" + capitalized);
                    
                    Class<?> type = getter != null ? getter.getReturnType() : method.getParameterTypes().length > 0 ? method.getParameterTypes()[0] : void.class;
                    Method setter = findMethod(bean.getClass(), "set" + capitalized, type);
                    Method valuesMethod = findBeanValuesMethod(bean.getClass(), name);

                    ret.put(name, new BeanPropertyInfo(name, ann.description(), ann.values(), ann.valueNames(), type, null, getter, setter, valuesMethod));
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return ret;
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private Method findBeanValuesMethod(Class<?> clazz, String propertyName) {
        for (Method method : clazz.getMethods()) {
            // Check merged BeanProperty annotation first
            BeanProperty propertyAnnotation = method.getAnnotation(BeanProperty.class);
            if (propertyAnnotation != null && propertyAnnotation.name().equals(propertyName + "Values")) {
                return method;
            }
            // Naming convention fallback
            if (method.getName().equals(propertyName + "Values") ||
                method.getName().equals("get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1) + "Values")) {
                return method;
            }
        }
        return null;
    }

    public void setProperty(Object bean, String propertyName, Object value) {
        try {
            Map<String, BeanPropertyInfo> properties = getProperties(bean);
            BeanPropertyInfo info = properties.get(propertyName);
            if (info != null) {
                info.setValue(bean, value);
            } else {
                // Fallback for non-annotated fields?
                Field field = findField(bean.getClass(), propertyName);
                if (field != null) {
                    field.setAccessible(true);
                    field.set(bean, value);
                }
            }
        } catch (Exception e) {
            Log.e("BeanFactory", "Error setting property " + propertyName, e);
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> clazz, String propertyName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(propertyName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object instantiateBean(String id) {
        if (id != null && beans.containsKey(id))
            throw new IllegalArgumentException("Bean already instantiated: " + id);
        if (id == null || !definitions.containsKey(id))
            throw new IllegalArgumentException("id or class not found: " + id);
        Class<?> beanClass = definitions.get(id);
        if (beanClass == null)
            throw new IllegalArgumentException("bean class cannot be null: " + id);
        try {
            status.put(id, STATUS_INSTANTIATED);
            Object newObj = beanClass.newInstance();
            beans.put(id, newObj);
            return newObj;
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
        }
    }

    private Object startBean(String id) {
        Object bean = configureBean(id);
        setUpBean(id);
        return bean;
    }

    public <T> T configure(T bean) {
        return (T)configureBean("no-id", bean);
    }

    private Object configureBean(String id) {
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);
        final Object bean = beans.get(id);
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_CONFIGURED) {
            return bean;
        }
        this.status.put(id, STATUS_CONFIGURED);
        return configureBean(id, bean);
    }

    @Nullable
    private Object configureBean(String id, Object bean) {
        if (bean == null) return null;
        try {
            Class<?> currentClass = bean.getClass();
            while (currentClass != null) {
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getAnnotation(Inject.class) == null) continue;
                    final Object candidate;
                    String named = null;
                    if (field.getAnnotation(Named.class) != null) {
                        named = Objects.requireNonNull(field.getAnnotation(Named.class)).value();
                    }
                    if (named != null) {
                        candidate = get(named) != null ? get(named) : get(getNamespace(id) + "." + named);
                    } else if (field.getType().isAssignableFrom(Map.class) && field.getGenericType() instanceof ParameterizedType) {
                        Type actualTypeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
                        candidate = findAll2((Class<?>) actualTypeArgument, null);
                    } else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                        candidate = findAll((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], null);
                    } else {
                        candidate = find(field.getType(), getNamespace(id));
                    }
                    if (candidate != null) field.set(bean, candidate);
                    field.setAccessible(false);
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return beans.get(id);
    }

    private Object setUpBean(String id) {
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);
        final Object bean = beans.get(id);
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_INITIALIZED) {
            return bean;
        }
        this.status.put(id, STATUS_INITIALIZED);
        if (bean == null) return null;
        try {
            for (Method method : bean.getClass().getDeclaredMethods()){
                if (method.getAnnotation(BeanInit.class) != null){
                    return method.invoke(bean);
                }
            }
            return null;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() {
        if (initialized) return;
        initialized = true;
        int max = 3;
        do {
            definitionsUpdated = false;
            for (Map.Entry<String, Class<?>> entry : definitions.entrySet()) {
                String id = entry.getKey();
                if (!status.containsKey(id)) instantiateBean(id);
            }
            beansUpdated = false;
            for (String id : beans.keySet().toArray(new String[0])) {
                if (!status.containsKey(id) || status.get(id) < STATUS_CONFIGURED) configureBean(id);
            }
            for (String id : beans.keySet().toArray(new String[0])) {
                if (!status.containsKey(id) || status.get(id) < STATUS_INITIALIZED) setUpBean(id);
            }
        } while ((definitionsUpdated || beansUpdated) && max-- > 0);
    }

    private void onBeanUpdate(String id) {
        if (id == null || !beans.containsKey(id)) throw new IllegalArgumentException("id or bean not found: " + id);
        final Object beanUpdated = beans.get(id);
        if (beanUpdated == null) return;
        final List<?> duplicates = findAll(beanUpdated.getClass(), null);
        int beanIdx = duplicates.indexOf(beanUpdated);
        for (Map.Entry<String,Object> entry : beans.entrySet()){
            final Object bean = entry.getValue();
            if (bean == null) continue;
            try {
                Class<?> currentClass = bean.getClass();
                while (currentClass != null) {
                    for (Field field : currentClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getAnnotation(Inject.class) == null) continue;
                        String named = field.getAnnotation(Named.class) != null ? field.getAnnotation(Named.class).value() : null;
                        if (field.getType().isAssignableFrom(beanUpdated.getClass())) {
                            if (id.equals(named) || duplicates.size() == 1 || beanIdx == 0) {
                                field.set(bean, beanUpdated);
                                onBeanUpdateCallback(bean, id, beanUpdated);
                            }
                        } else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                            final Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            if (type.isAssignableFrom(beanUpdated.getClass())) {
                                field.set(bean, findAll(type, null));
                                onBeanUpdateCallback(bean, entry.getKey(), beanUpdated);
                            }
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch (IllegalAccessException e) {}
        }
    }

    public Object invoke(String id, String methodName) {
        final Object bean = beans.get(id);
        if (bean == null) throw new IllegalArgumentException("bean not found: " + id);
        try {
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Nullable
    private static String getNamespace(String id) {
        return id != null && id.contains(".") ? id.substring(0, id.lastIndexOf('.')) : null;
    }

    public void add(String id, Class<?> clazz) {
        if (this.definitions.containsKey(id)) throw new IllegalArgumentException("Definition already exists: "+id);
        this.definitions.put(id, clazz);
        this.definitionsUpdated = true;
    }

    public void add(String id, Object object) {
        if (this.beans.containsKey(id)) throw new IllegalArgumentException("Bean already exists: "+id);
        this.addOrReplace(id, object);
    }

    public <T> T addAndGet(String id, Class<T> clazz) {
        this.definitions.put(id, clazz);
        this.instantiateBean(id);
        return (T)startBean(id);
    }

    public <T> Object addOrReplace(String id, T object) {
        Object old = this.beans.put(id, object);
        this.status.put(id, STATUS_INSTANTIATED);
        this.beansUpdated = true;
        if (!initialized) return old;
        configureBean(id);
        setUpBean(id);
        onBeanUpdate(id);
        return old;
    }

    public <T> T find(Class<T> clazz) {
        return find(clazz, null);
    }

    private <T> T find(Class<T> clazz, String namespace) {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String entryNamespace = getNamespace(entry.getKey());
            if (namespace == null || (entryNamespace != null && namespace.startsWith(entryNamespace))) {
                if (clazz.isAssignableFrom(entry.getValue().getClass())) return clazz.cast(entry.getValue());
            }
        }
        if (namespace != null) return find(clazz, getNamespace(namespace));
        for (Object bean : beans.values()) {
            if (clazz.isAssignableFrom(bean.getClass())) return clazz.cast(bean);
        }
        return null;
    }

    public <T> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null);
    }

    public <T> List<T> findAll(Class<T> clazz, String parent) {
        List<T> ret = new ArrayList<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (parent == null || entry.getKey().startsWith(parent)) {
                if (clazz.isAssignableFrom(entry.getValue().getClass())) ret.add(clazz.cast(entry.getValue()));
            }
        }
        if (!ret.isEmpty()) {
            Collections.sort(ret, (o1, o2) -> {
                BeanOrder a1 = o1.getClass().getAnnotation(BeanOrder.class);
                BeanOrder a2 = o2.getClass().getAnnotation(BeanOrder.class);
                return (a1 != null ? a1.order() : 0) - (a2 != null ? a2.order() : 0);
            });
        }
        return ret;
    }

    public <T> Map<String,T> findAll2(Class<T> clazz, String parent) {
        Map<String,T> ret = new HashMap<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (parent == null || entry.getKey().startsWith(parent)) {
                if (clazz.isAssignableFrom(entry.getValue().getClass())) ret.put(entry.getKey(), clazz.cast(entry.getValue()));
            }
        }
        return ret;
    }

    public Object get(String key) {
        return beans.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(beans.get(key));
    }

    public static List<Field> getFields(Object bean, Class<? extends Annotation> annotation) {
        List<Field> ret = new ArrayList<>();
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(annotation)) {
                field.setAccessible(true);
                ret.add(field);
            }
        }
        return ret;
    }

    private Object onBeanUpdateCallback(Object bean, String id, Object updated) {
        try {
            for (Method method : bean.getClass().getDeclaredMethods()){
                if (method.isAnnotationPresent(OnBeanUpdate.class)) return method.invoke(bean, id, updated);
            }
            return null;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void remove(String id) {
        beans.remove(id);
        definitions.remove(id);
        status.remove(id);
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnBeanUpdate {
    }
}
