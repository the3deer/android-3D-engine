package org.the3deer.util.bean;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * A simple CDI (Component Dependency Injection) implementation.
 *
 * <p>The BeanManager manages a collection of beans, providing dependency injection,
 * dynamic bean creation, and lifecycle management.</p>
 *
 * <ul>
 *   <li><b>Order of Addition:</b> Beans are processed in the order they are added.</li>
 *   <li><b>Dependency Order:</b> Dependent beans must be defined before the beans that depend on them.</li>
 *   <li><b>Dynamic Creation:</b> Supports dynamic bean creation via {@link BeanFactory}.</li>
 *   <li><b>Lifecycle:</b> Supports post-initialization logic via {@link BeanInit} and {@link BeanStart}.</li>
 *   <li><b>Namespace Isolation:</b> Beans are identified by a key (e.g., "namespace.beanName").</li>
 *   <li><b>Lookup Priority:</b> When injecting a bean without a regex, the manager first searches
 *       the current namespace, then parent namespaces up to the root. If still not found, it
 *       performs a global search.</li>
 *   <li><b>Explicit Privacy:</b> Beans whose key ends with a hyphen (e.g., "gui.camera-") are private.
 *       They are only visible within their own namespace or its children.</li>
 *   <li><b>Regex Bypass:</b> To inject private beans from other namespaces, or to perform a broad
 *       lookup, use the {@link Named} annotation with a regular expression (e.g., "@Named(\".*\")").</li>
 * </ul>
 */
public class BeanManager {

    private static final Logger logger = Logger.getLogger(BeanManager.class.getName());

    /**
     * Bean is instantiated
     */
    private static final Integer STATUS_INSTANTIATED = 0;
    /**
     * Beans has all dependencies injected
     */
    private static final Integer STATUS_CONFIGURED = 1;
    /**
     * Beans is initialized
     */
    private static final Integer STATUS_INITIALIZED = 2;
    /**
     * Bean is started
     */
    private static final Integer STATUS_STARTED = 3;

    private final Map<String, Class<?>> definitions = new LinkedHashMap<>();
    private final Map<String, Object> beans = new LinkedHashMap<>();
    private final Map<String, Integer> status = new LinkedHashMap<>();

    private final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setPriority(Thread.NORM_PRIORITY); // Standard priority
        thread.setName("BeanFactory");
        return thread;
    });

    private boolean definitionsUpdated;
    private boolean beansUpdated;
    private boolean isInitialized;
    private boolean isStarted;

    private BeanManager() {
    }

    public static BeanManager getInstance() {
        BeanManager instance = new BeanManager();
        instance.beans.put("beanFactory", instance);
        instance.definitions.put("beanFactory", BeanManager.class);
        instance.status.put("beanFactory", STATUS_INITIALIZED);
        return instance;
    }

    public Map<String, Object> getBeans() {
        return Collections.unmodifiableMap(beans);
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setStarted(boolean started) {
        isStarted = started;
    }

    public static Map<String, BeanPropertyInfo> getProperties(Object bean) {

        final Map<String, BeanPropertyInfo> ret = new TreeMap<>();

        Class<?> currentClass = bean.getClass();
        final String beanId = currentClass.getName();

        while (currentClass != null) {

            final Bean beanAnn = currentClass.getAnnotation(Bean.class);

            final String beanName = (beanAnn != null && !beanAnn.name().isEmpty()) ? beanAnn.name() : BeanUtils.getSnakeCase(currentClass);

            for (Field field : currentClass.getDeclaredFields()) {

                BeanProperty ann = field.getAnnotation(BeanProperty.class);

                if (ann == null) continue;

                // inputs
                final String fieldName = field.getName();
                final String propertyId = currentClass.getName() + "." + fieldName;

                // set property name
                String propertyName = (ann.name() != null && !ann.name().isEmpty()) ? ann.name() : fieldName;

                // get values method
                Method valuesMethod = findBeanValuesMethod(currentClass, fieldName, propertyName);

                // get setter/getter
                Method setter = findSetterMethod(currentClass, fieldName, propertyName, field.getType());
                Method getter = findGetterMethod(currentClass, fieldName, propertyName, field.getType());

                logger.finest("Bean Property found. propertyId: " + propertyId + ", propertyName: " + propertyName + ", bean: " + beanId + " - " + setter + " - " + getter);

                ret.put(propertyId, new BeanPropertyInfo(propertyId, fieldName, beanName, propertyName, ann.description(), ann.values(), field.getType(), field, getter, setter, valuesMethod));
            }
            for (Method method : currentClass.getDeclaredMethods()) {

                // get annotation
                final BeanProperty ann = method.getAnnotation(BeanProperty.class);

                // check
                if (ann == null) continue;

                // inputs
                final String annotatedName = ann.name();
                final String methodName = method.getName();

                // check values method
                if (methodName.endsWith("Values")) {
                    // ignore. it will be processed later on by the
                    continue;
                }

                // build property name
                final String propertyName = annotatedName != null && !annotatedName.isEmpty() ? annotatedName :
                        (methodName.startsWith("get") || methodName.startsWith("set")) ?
                        methodName.substring(3, 4).toLowerCase() + methodName.substring(4) :
                        (methodName.startsWith("is") ? methodName.substring(2, 3).toLowerCase() + methodName.substring(3) : methodName);

                // build property id
                final String propertyId = beanId + "." + propertyName;

                // check
                if (ret.containsKey(propertyId)) continue;

                // check if method follows bean naming convention
                if (!methodName.startsWith("get") && !methodName.startsWith("set") && !methodName.startsWith("is")) {
                    logger.log(Level.SEVERE, "Bean Property is not prefixed with set|get|is. bean: " + beanId + ", method: " + methodName);
                    continue;
                }

                // find getter/setter methods
                Method getter = findGetterMethod(currentClass, propertyName, propertyName, Object.class);

                // check
                if (getter == null) {
                    logger.log(Level.SEVERE, "Bean Property error. Getter is null. bean: " + beanId + ", method: " + methodName);
                    continue;
                }

                // get setter
                Method setter = findSetterMethod(currentClass, propertyName, propertyName, getter.getReturnType());

                // check
                if (setter == null) {
                    logger.log(Level.SEVERE, "Bean Property error. Setter is null. bean: " + beanId + ", method: " + methodName);
                    continue;
                }

                // get values method
                Method valuesMethod = findBeanValuesMethod(currentClass, propertyName, propertyName);

                logger.finest("Bean Property found. propertyId: " + propertyId + ", propertyName: " + propertyName + ", bean: " + beanId + ", setter: " + setter + ", getter: " + getter);

                ret.put(propertyId, new BeanPropertyInfo(propertyId, null, beanName, propertyName, ann.description(), ann.values(), getter.getReturnType(), null, getter, setter, valuesMethod));
            }
            currentClass = currentClass.getSuperclass();
        }
        return ret;
    }

    /**
     * Find a getter method with the specified return type
     *
     * @param clazz        the bean class
     * @param propertyName the method name
     * @param returnType   the return type
     * @return the method
     */
    private static Method findGetterMethod(Class<?> clazz, String fieldName, String propertyName, Class<?> returnType) {

        // get all methods
        final Method[] methods = clazz.getMethods();

        // expected method name
        final String expectedMethodName1;
        final String expectedMethodName2;
        if (Boolean.class.isAssignableFrom(returnType)) {
            expectedMethodName1 = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            expectedMethodName2 = "is" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        } else {
            expectedMethodName1 = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            expectedMethodName2 = "get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        }

        // loop all methods
        for (final Method method : methods) {

            // inputs
            final String methodName = method.getName();
            final BeanProperty annotation = method.getAnnotation(BeanProperty.class);

            // check
            if (annotation == null) continue;
            if (method.getParameterTypes().length != 0) continue;
            if (returnType.isAssignableFrom(method.getReturnType())) continue;
            if (!methodName.startsWith("get") && !methodName.startsWith("is")) continue;

            // match with property
            if (propertyName.equals(annotation.name())) return method;

            // match capitalized method name
            if (methodName.equals(expectedMethodName1)) return method;
        }

        // try with field name
        try {
            return clazz.getMethod(expectedMethodName1);

        } catch (NoSuchMethodException ignored) {
        }

        // try with property name
        try {
            return clazz.getMethod(expectedMethodName1);

        } catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    private static Method findSetterMethod(Class<?> clazz, String fieldName, String propertyName, Class<?> parameterType) {

        // get all methods
        final Method[] methods = clazz.getMethods();

        // loop all methods
        String expectedMethodName1 = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        String expectedMethodName2 = "set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1);
        for (int i = 0; i < methods.length; i++) {

            // inputs
            final Method method = methods[i];
            final String methodName = method.getName();
            final BeanProperty annotation = method.getAnnotation(BeanProperty.class);

            // check
            if (annotation == null) continue;
            if (method.getParameterTypes().length != 1) continue;
            if (!method.getParameterTypes()[0].isAssignableFrom(parameterType)) continue;
            if (!methodName.startsWith("set")) continue;

            // match with property
            if (propertyName.equals(annotation.name())) return method;

            // match capitalized method name
            if (methodName.equals(expectedMethodName1)) return method;
        }

        // try default field name
        try {
            return clazz.getMethod(expectedMethodName1, parameterType);
        } catch (NoSuchMethodException ignored) {
        }

        // try with property name
        try {
            return clazz.getMethod(expectedMethodName2, parameterType);
        } catch (NoSuchMethodException ignored) {
        }

        return null;
    }

    private static Method findBeanValuesMethod(Class<?> clazz, String fieldName, String propertyName) {
        for (Method method : clazz.getMethods()) {
            // Naming convention fallback
            if (method.getName().equals(propertyName + "Values") ||
                    method.getName().equals("get" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1) + "Values")) {
                return method;
            } else if (method.getName().equals("get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1) + "Values")) {
                return method;
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

    private boolean startBean(String id) {

        // check
        if (id == null) throw new IllegalArgumentException("id cannot be null");

        // get bean
        final Object bean = beans.get(id);

        // check
        if (bean == null) throw new IllegalStateException("bean not found: " + id);

        // check current status
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_STARTED) {
            return false;
        }

        // get annotated method
        final Method annotatedMethod = findAnnotatedMethod(bean.getClass(), BeanStart.class);

        if (annotatedMethod == null) return false;

        // update status
        this.status.put(id, STATUS_STARTED);

        // invoke
        executorService.execute(() -> invokeAnnotatedMethod(bean, BeanStart.class));

        return true;
    }

    /**
     * Configure a bean. That is, injecting all dependencies
     *
     * @param bean the bean to configure
     * @return the bean ready to be used
     */
    public <T> T configure(T bean) {
        return (T) configureBean("no-id", bean);
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

    private Object configureBean(String id, Object bean) {
        if (bean == null) return null;
        logger.finest("Configuring bean... id: " + id);
        try {
            Class<?> currentClass = bean.getClass();
            while (currentClass != null) {
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    if (field.getAnnotation(Inject.class) == null) continue;

                    final String named = field.getAnnotation(Named.class) != null ?
                            field.getAnnotation(Named.class).value() : null;

                    final Object candidate;
                    if (field.getType().isAssignableFrom(Map.class) && field.getGenericType() instanceof ParameterizedType) {
                        final Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
                        if (named != null) {
                            candidate = findAllByRegex(type, named);
                        } else {
                            candidate = findAll2(type, getNamespace(id));
                        }
                    } else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                        final Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                        if (named != null) {
                            candidate = findAllByRegex(type, named);
                        } else {
                            candidate = findAll(type, getNamespace(id));
                        }
                    } else {
                        if (named != null) {
                            candidate = findByRegex(field.getType(), named);
                        } else {
                            candidate = find(field.getType(), getNamespace(id));
                        }
                    }

                    if (candidate != null) {
                        field.set(bean, candidate);
                    } else {
                        if (field.getName().startsWith("_")) {
                            throw new IllegalStateException("Dependency not found. bean: " + id + ", field: " + field.getName());
                        }
                    }
                    field.setAccessible(false);
                }
                currentClass = currentClass.getSuperclass();
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return beans.get(id);
    }

    private boolean setUpBean(String id) {

        // check
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);

        // get bean
        final Object bean = beans.get(id);
        if (bean == null) throw new IllegalStateException("bean not found: " + id);

        // check current status
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_INITIALIZED) {
            return false;
        }

        // update status
        this.status.put(id, STATUS_INITIALIZED);

        logger.finest("Initializing bean... id: " + id);

        // invoke
        invokeAnnotatedMethod(bean, BeanInit.class);

        return true;
    }

    public Method findAnnotatedMethod(Class<?> clazz, Class<? extends Annotation> annotationClass) {

        // check bean
        if (clazz == null) throw new IllegalArgumentException("class cannot be null");

        // check annotation
        if (annotationClass == null)
            throw new IllegalArgumentException("annotation cannot be null");

        // invoke
        Method methodFound = null;
        for (Method method : clazz.getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.getAnnotation(annotationClass) != null) {
                methodFound = method;
                method.setAccessible(false);
                break;
            }
            method.setAccessible(false);
        }
        return methodFound;
    }

    /**
     * Set up a bean. That is, calling the @BeanInit method
     *
     * @param bean the bean to set up
     * @return the result of the invocation
     */
    private Object invokeAnnotatedMethod(Object bean, Class<? extends Annotation> annotationClass) {

        // invoke
        try {
            Object returned = null;
            for (Method method : bean.getClass().getDeclaredMethods()) {
                method.setAccessible(true);
                if (method.getAnnotation(annotationClass) != null) {
                    returned = method.invoke(bean);
                    break;
                }
                method.setAccessible(false);
            }
            return returned;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize() {

        if (isInitialized) return;

        definitionsUpdated = false;
        for (Map.Entry<String, Class<?>> entry : definitions.entrySet()) {
            String id = entry.getKey();
            if (!status.containsKey(id)) instantiateBean(id);
        }

        buildBeans();

        isInitialized = true;
    }

    /**
     * Pre-Processor to build beans
     */
    private void buildBeans() {

        // check
        if (beans.isEmpty()) return;

        // instantiate factory beans
        for (Map.Entry<String, Class<?>> entry : definitions.entrySet().toArray(new Map.Entry[0])) {

            // get factory method
            final Method annotatedMethod = findAnnotatedMethod(entry.getValue(), BeanFactory.class);

            // check
            if (annotatedMethod == null) continue;

            // configure beforehand
            configureBean(entry.getKey());

            // get bean
            final Object bean = beans.get(entry.getKey());
            if (bean == null) throw new IllegalStateException("bean not found: " + entry.getKey());

            // invoke
            final Map<String, Object> newBeans = (Map<String, Object>) invokeAnnotatedMethod(bean, BeanFactory.class);

            // log event
            if (newBeans == null || newBeans.isEmpty()) {

                // log warning
                logger.warning("Factory method returned null. bean: " + entry.getKey() + ", method: " + annotatedMethod);

                continue;
            }

            // update model
            this.beans.putAll(newBeans);

            // log event
            logger.config("Created " + beans.size() + " beans by bean: " + entry.getKey());

        }
    }

    public void start() {
        if (isStarted) return;
        isStarted = true;

        int max = 3;
        do {
            definitionsUpdated = false;
            for (Map.Entry<String, Class<?>> entry : definitions.entrySet().toArray(new Map.Entry[0])) {
                String id = entry.getKey();
                if (!status.containsKey(id)) instantiateBean(id);
            }
            beansUpdated = false;

            // Pre-Processor
            buildBeans();

            for (String id : definitions.keySet().toArray(new String[0])) {
                if (!status.containsKey(id) || status.get(id) < STATUS_CONFIGURED)
                    configureBean(id);
            }
            for (String id : definitions.keySet().toArray(new String[0])) {
                if (!status.containsKey(id) || status.get(id) < STATUS_INITIALIZED)
                    setUpBean(id);
            }
            for (String id : definitions.keySet().toArray(new String[0])) {
                if (!status.containsKey(id) || status.get(id) < STATUS_STARTED)
                    startBean(id);
            }
        } while ((definitionsUpdated || beansUpdated) && max-- > 0);
    }

    private void onBeanUpdate(String id) {
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);
        final Object beanUpdated = beans.get(id);
        if (beanUpdated == null) return;
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            final Object bean = entry.getValue();
            if (bean == null || bean == beanUpdated) continue;
            try {
                Class<?> currentClass = bean.getClass();
                while (currentClass != null) {
                    for (Field field : currentClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (field.getAnnotation(Inject.class) == null) continue;

                        final String named = field.getAnnotation(Named.class) != null ?
                                field.getAnnotation(Named.class).value() : null;

                        if (field.getType().isAssignableFrom(beanUpdated.getClass())) {
                            if (named != null) {
                                if (id.matches(named)) {
                                    field.set(bean, beanUpdated);
                                    onBeanUpdateCallback(entry.getKey(), bean, beanUpdated);
                                }
                            } else {
                                // If no regex, only update if this bean is now the "best" match
                                Object bestMatch = find(field.getType(), getNamespace(entry.getKey()));
                                if (bestMatch == beanUpdated) {
                                    field.set(bean, beanUpdated);
                                    onBeanUpdateCallback(entry.getKey(), bean, beanUpdated);
                                }
                            }
                        } else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                            final Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            if (type.isAssignableFrom(beanUpdated.getClass())) {
                                if (named != null) {
                                    field.set(bean, findAllByRegex(type, named));
                                } else {
                                    field.set(bean, findAll(type, getNamespace(entry.getKey())));
                                }
                                onBeanUpdateCallback(entry.getKey(), bean, beanUpdated);
                            }
                        } else if (field.getType().isAssignableFrom(Map.class) && field.getGenericType() instanceof ParameterizedType) {
                            final Class<?> type = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];
                            if (type.isAssignableFrom(beanUpdated.getClass())) {
                                if (named != null) {
                                    field.set(bean, findAll2ByRegex(type, named));
                                } else {
                                    field.set(bean, findAll2(type, getNamespace(entry.getKey())));
                                }
                                onBeanUpdateCallback(entry.getKey(), bean, beanUpdated);
                            }
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch (IllegalAccessException e) {
            }
        }
    }

    public Object invoke(String id, String methodName) {
        final Object bean = beans.get(id);
        if (bean == null) throw new IllegalArgumentException("bean not found: " + id);
        try {
            return bean.getClass().getMethod(methodName).invoke(bean);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String getNamespace(String id) {
        return id != null && id.contains(".") ? id.substring(0, id.lastIndexOf('.')) : null;
    }

    public void add(String id, Class<?> clazz) {
        if (this.definitions.containsKey(id))
            throw new IllegalArgumentException("Definition already exists: " + id);
        this.definitions.put(id, clazz);
        this.definitionsUpdated = true;
    }

    public boolean add(String id, Object object) {

        // check bean exists and it is the same
        if (this.beans.containsKey(id) && this.beans.get(id) == object) return false;

        // add bean
        addOrReplace(id, object);

        return true;
    }

/*    public <T> T addAndGet(String id, Class<T> clazz) {
        this.definitions.put(id, clazz);
        this.instantiateBean(id);
        return (T) startBean(id);
    }*/

    public <T> Object addOrReplace(String id, T object) {

        // check
        if (id == null || object == null)
            throw new IllegalArgumentException("id or object cannot be null");

        // get current
        Object old = this.beans.put(id, object);

        // update model
        this.definitions.put(id, object.getClass());
        this.status.put(id, STATUS_INSTANTIATED);
        this.beansUpdated = true;

        // check
        if (!isInitialized) return old;

        if (!isStarted) return old;

        configureBean(id);
        setUpBean(id);
        onBeanUpdate(id);

        return old;
    }

    private <T> T findByRegex(Class<T> clazz, String regex) {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getKey().matches(regex) && clazz.isInstance(entry.getValue())) {
                return clazz.cast(entry.getValue());
            }
        }
        return null;
    }

    private <T> List<T> findAllByRegex(Class<T> clazz, String regex) {
        final List<T> ret = new ArrayList<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getKey().matches(regex) && clazz.isInstance(entry.getValue())) {
                ret.add(clazz.cast(entry.getValue()));
            }
        }
        sort(ret);
        return ret;
    }

    private <T> Map<String, T> findAll2ByRegex(Class<T> clazz, String regex) {
        final Map<String, T> ret = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (entry.getKey().matches(regex) && clazz.isInstance(entry.getValue())) {
                ret.put(entry.getKey(), clazz.cast(entry.getValue()));
            }
        }
        return ret;
    }

    public <T> T find(Class<T> clazz) {
        return find(clazz, null);
    }

    public <T> T find(Class<T> clazz, String namespace) {

        // check
        if (clazz == null) return null;

        // Step 1: Search for the bean in the specified namespace or its parents (includes private beans)
        String currentNamespace = namespace;
        while (true) {
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                if (Objects.equals(currentNamespace, getNamespace(entry.getKey())) &&
                        clazz.isInstance(entry.getValue())) {
                    return clazz.cast(entry.getValue());
                }
            }
            if (currentNamespace == null) break;
            currentNamespace = getNamespace(currentNamespace);
        }

        // Step 2: Global search (excludes private beans from other namespaces)
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (clazz.isInstance(entry.getValue()) && !entry.getKey().endsWith("-")) {
                return clazz.cast(entry.getValue());
            }
        }

        return null;
    }

    public <T> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null);
    }

    private <T> List<T> findAll(Class<T> clazz, String namespace) {
        final Map<String, T> matches = new LinkedHashMap<>();

        // Step 1: Hierarchy search
        String currentNamespace = namespace;
        while (true) {
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                if (Objects.equals(currentNamespace, getNamespace(entry.getKey())) &&
                        clazz.isInstance(entry.getValue())) {
                    matches.putIfAbsent(entry.getKey(), clazz.cast(entry.getValue()));
                }
            }
            if (currentNamespace == null) break;
            currentNamespace = getNamespace(currentNamespace);
        }

        // Step 2: Global search (public only)
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (clazz.isInstance(entry.getValue()) && !entry.getKey().endsWith("-")) {
                matches.putIfAbsent(entry.getKey(), clazz.cast(entry.getValue()));
            }
        }

        final List<T> ret = new ArrayList<>(matches.values());
        sort(ret);
        return ret;
    }

    private <T> void sort(List<T> list) {
        if (list.isEmpty()) return;
        list.sort((o1, o2) -> {
            BeanOrder a1 = o1.getClass().getAnnotation(BeanOrder.class);
            BeanOrder a2 = o2.getClass().getAnnotation(BeanOrder.class);
            return (a1 != null ? a1.order() : 0) - (a2 != null ? a2.order() : 0);
        });
    }

    public <T> Map<String, T> findAll2(Class<T> clazz, String namespace) {
        final Map<String, T> matches = new LinkedHashMap<>();

        // Step 1: Hierarchy search
        String currentNamespace = namespace;
        while (true) {
            for (Map.Entry<String, Object> entry : beans.entrySet()) {
                if (Objects.equals(currentNamespace, getNamespace(entry.getKey())) &&
                        clazz.isInstance(entry.getValue())) {
                    matches.putIfAbsent(entry.getKey(), clazz.cast(entry.getValue()));
                }
            }
            if (currentNamespace == null) break;
            currentNamespace = getNamespace(currentNamespace);
        }

        // Step 2: Global search (public only)
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (clazz.isInstance(entry.getValue()) && !entry.getKey().endsWith("-")) {
                matches.putIfAbsent(entry.getKey(), clazz.cast(entry.getValue()));
            }
        }
        return matches;
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

    public boolean contains(String beanId) {
        return beans.containsKey(beanId);
    }

    public boolean contains(Object bean) {
        return beans.containsValue(bean);
    }

    private static void onBeanUpdateCallback(String beanId, Object bean, Object updated) {
        try {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(OnBeanUpdate.class)) {
                    method.invoke(bean, beanId, updated);
                    return;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean remove(String beanId, Object bean) {

        // check
        if (!beans.containsKey(beanId)) return false;

        // remove from dependencies
        for (Map.Entry<String, Object> entry : beans.entrySet()) {

            // candidate bean to be impacted
            final Object beanCandidate = entry.getValue();

            // get dependencies
            final List<Field> fields = getFields(beanCandidate, Inject.class);

            // loop all fields
            for (Field field : fields) {

                // check field is of bean type
                if (field.getType().isAssignableFrom(bean.getClass())) {

                    // set field and log error if any
                    setFieldToNull(beanId, beanCandidate, field);
                }

                // check fields of type List
                else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                    Type actualTypeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];

                    // check parameterized type is of bean class
                    if (actualTypeArgument == bean.getClass()) {

                        // assign null value
                        removeBeanFromList(beanId, beanCandidate, field);
                    }
                }

                // check fields of type Map
                else if (field.getType().isAssignableFrom(Map.class) && field.getGenericType() instanceof ParameterizedType) {
                    Type actualTypeArgument = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[1];

                    // check parameterized type is of bean class
                    if (actualTypeArgument == bean.getClass()) {

                        // assign null value
                        removeBeanFromMap(beanId, beanCandidate, field);
                    }
                }
            }
        }

        // remove from factory
        beans.remove(beanId);
        definitions.remove(beanId);
        status.remove(beanId);

        return true;
    }

    private static boolean setFieldToNull(String beanId, Object bean, Field field) {
        // assign null value
        try {

            // notify in advance
            onBeanUpdateCallback(beanId, bean, null);

            // update bean
            field.set(bean, null);

            return true;

        } catch (IllegalAccessException e) {

            // log error
            logger.log(Level.SEVERE, "Error setting property. field: " + field.getName() + ", bean: " + beanId, e);

            return false;
        }
    }

    private static boolean removeBeanFromList(String beanId, Object bean, Field field) {
        // assign null value
        try {

            // get field value
            List<?> fieldValue = (List<?>) field.get(bean);

            // check field value is not null
            if (fieldValue == null) return false;

            // check the bean is in the list
            if (!fieldValue.contains(bean)) return false;

            // notify in advance
            onBeanUpdateCallback(beanId, bean, null);

            // remove bean from list
            return fieldValue.remove(bean);

        } catch (IllegalAccessException e) {

            // log error
            logger.log(Level.SEVERE, "Error setting property. field: " + field.getName() + ", bean: " + beanId, e);

            // continue;
            return false;
        }
    }

    private static boolean removeBeanFromMap(String beanId, Object bean, Field field) {
        // assign null value
        try {

            // get field value
            Map<String, ?> fieldValue = (Map<String, ?>) field.get(bean);

            // check the bean is in the list
            if (!fieldValue.containsKey(beanId)) return false;

            // notify in advance
            onBeanUpdateCallback(beanId, bean, null);

            // remove bean from list
            return fieldValue.remove(beanId, bean);

        } catch (IllegalAccessException e) {

            // log error
            logger.log(Level.SEVERE, "Error setting property. field: " + field.getName() + ", bean: " + beanId, e);

            // continue;
            return false;
        }
    }

    public void close() {
        this.beans.clear();
        this.definitions.clear();
        this.status.clear();
        this.isInitialized = false;
        this.isStarted = false;
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface OnBeanUpdate {
    }
}
