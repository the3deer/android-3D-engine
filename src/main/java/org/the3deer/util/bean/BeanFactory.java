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
 * <p>
 * Features:
 * - @Inject dependencies
 * - invoke setUp() method
 *
 * <p>
 * scene
 * node_0
 * _transform
 * camera
 * mesh
 * material
 * skin
 * animation
 * renderer
 * ...
 * node_1
 * _transform
 * gui
 * camera
 * ...
 *  // FIXME:
 *     next steps (bean proxy)
 *     i want to implement a BeanFactory List implementation that is "refreshable"
 *     i want to implement a BeanFactory object proxy that is "refreshable"
 *     it means the @Inject List<T> or @Inject T will be proxies.
 *     #addScene . this is manually injecting the dependency
 *     #SceneDrawer . we have a dynamic list of entities
 * <pre></pre>
 * {@code
 *     {
 *         BeanFactory.getInstance().add("myobjid","mystring");
 *         BeanFactory.getInstance().addAndInit("myobjid", "mystring");
 *         // this would automatically update and inject new references if any
 *         BeanFactory.getInstance().addOrReplace("myobjid", "mystring");
 *         // this would automatically add and update the dependencies if any
 *     }
 *     }
 *
 *     </pre>
 *
 *     Gemini advice:
 * Start by implementing re-injection for single @Inject T fields.
 * When addOrReplace(id, newBeanInstance) is called:
 * Store newBeanInstance.
 * Iterate through all beans currently managed by BeanFactory.
 * For each bean, inspect its fields. If a field f is annotated with @Inject (and potentially @Named("id") or matches by type to newBeanInstance.getClass()), then update beanInstance.f = newBeanInstance.
 * Consider calling an @PostRefresh method if defined on the bean.
 *
 * Then tackle List<T>:
 * When a relevant change happens (a bean of type T is added/removed/replaced) :
 * Iterate through all beans.
 * If a bean has a field List<T> someList, re-generate the entire list List<T> newList = beanFactory.findAll(T.class) and set beanInstance.someList = newList. You will need to store metadata about which fields in which beans are injection points. You could gather this information during the initial configureBean phase. This approach avoids the complexity of writing and managing proxy InvocationHandlers and custom list implementations, but you must be aware of the implications regarding object identity and the potential need for beans to re-initialize themselves more explicitly. Test thoroughly, especially the scenarios where beans are replaced or lists change, to see how consumers react.
 */
public class BeanFactory {

    //private static BeanFactory instance;

    private final Logger LOG = LogManager.getLogManager().getLogger(this.getClass().getName());

    // static vars
    private static final Integer STATUS_INSTANTIATED = 0;
    private static final Integer STATUS_CONFIGURED = 1;
    private static final Integer STATUS_INITIALIZED = 2;

    // obj --> parent
    private final Map<String, Class<?>> definitions = new TreeMap<>();
    private final Map<String, Object> beans = new TreeMap<>();
    //private final Map<String, String> parents = new HashMap<>();
    private final Map<String, Integer> status = new HashMap<>();

    // vars
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

    /*public void init() {

        int max = 3;
        do {
            // iterate only once
            definitionsUpdated = false;

            // With reflection to instantiate an object
            for (Map.Entry<String, Class<?>> entry : definitions.entrySet()) {
                String id = entry.getKey();
                try {
                    instantiateBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception instantiating class (" + id + "): " + e.getMessage(), e);
                }
            }

            // iterate only once
            beansUpdated = false;

            for (String id : beans.keySet().toArray(new String[0])) {
                try {
                    startBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception setting-up class (" + id + "): " + e.getMessage(), e);
                }
            }

            // avoid infinite loop
            if (max-- < 0) break;

        } while (definitionsUpdated || beansUpdated);
    }*/

    private Object instantiateBean(String id) {

        // check
        if (id != null && beans.containsKey(id))
            throw new IllegalArgumentException("Bean already instantiated: " + id);

        // check
        if (id == null || !definitions.containsKey(id))
            throw new IllegalArgumentException("id or class not found: " + id);

        // target
        Class<?> beanClass = definitions.get(id);
        if (beanClass == null)
            throw new IllegalArgumentException("bean class cannot be null: " + id);

        try {
            // update status
            Log.d("BeanFactory", "Instantiating bean... id: " + id);
            status.put(id, STATUS_INSTANTIATED);

            // instantiate
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

    private Object configureBean(String id) {
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);

        /*// initialize parent first
        final String parentId = getParentId(id);
        if (parentId != null) {
            configureBean(parentId);
        }*/

        // target bean
        final Object bean = beans.get(id);

        // update status
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_CONFIGURED) {
            return bean;
        }
        this.status.put(id, STATUS_CONFIGURED);

        // check
        if (bean == null) return null;

        // init once
        try {
            Log.v("BeanFactory", "Configuring object... " + id);

            // inject first the dependencies
            Class<?> currentClass = bean.getClass();

            // loop the hierarchy
            while (currentClass != null) {

                // inject the dependencies
                for (Field field : currentClass.getDeclaredFields()) {
                    field.setAccessible(true);

                    // check
                    if (field.getAnnotation(Inject.class) == null) continue;

                    final Object candidate;
                    String named = null;
                    if (field.getAnnotation(Named.class) != null) {
                        named = Objects.requireNonNull(field.getAnnotation(Named.class)).value();
                    }
                    if (named != null) {
                        if (get(named) != null) {
                            candidate = get(named);
                        } else if (get(getNamespace(id)+"."+named) != null){
                            candidate = get(getNamespace(id)+"."+named);
                        } else {
                            candidate = null;
                        }
                    } else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                        candidate = findAll((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0], null);
                    } else {
                        final String context = getNamespace(id);
                        candidate = find(field.getType(), context);
                    }
                    if (candidate != null) {
                        Log.v("BeanFactory", "Dependency " + id + "." + field.getName() + ": " + candidate);
                        field.set(bean, candidate);
                    } else {
                        Log.v("BeanFactory", "Dependency not found: " + id + "." + field.getName()+", class: "+ field.getType());
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

    private Object setUpBean(String id) {

        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("id or bean not found: " + id);

        /*// initialize parent first
        final String parentId = getParentId(id);
        if (parentId != null) {
            initBean(parentId);
        }*/

        // target bean
        final Object bean = beans.get(id);

        // update status
        final Integer status = this.status.get(id);
        if (status != null && status >= STATUS_INITIALIZED) {
            return bean;
        }
        this.status.put(id, STATUS_INITIALIZED);

        // check
        if (bean == null) return null;

        try {
            for (Method method : bean.getClass().getDeclaredMethods()){
                if (method.getAnnotation(BeanInit.class) != null){
                    Log.v("BeanFactory", "Setting up object... " + id);
                    return method.invoke(bean);
                } /*else if (method.getName().equals("setUp")){
                    Log.v("BeanFactory", "Setting up object... " + id);
                    return method.invoke(bean);
                }*/
            }
            return null;
        } catch (InvocationTargetException e) {
            Log.e("BeanFactory", "Exception initializing bean: " + id + ", " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void init() {

        // check
        if (initialized){
            throw new IllegalStateException("Already initialized");
        }
        initialized = true;

        int max = 3;
        do {
            // iterate only once
            definitionsUpdated = false;

            // With reflection to instantiate an object
            for (Map.Entry<String, Class<?>> entry : definitions.entrySet()) {
                String id = entry.getKey();
                if (status.containsKey(id)){
                    continue;
                }
                try {
                    instantiateBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception refreshing bean (" + id + "): " + e.getMessage(), e);
                }
            }

            // iterate only once
            beansUpdated = false;

            Log.v("BeanFactory", "Configuring objects... ");
            for (String id : beans.keySet().toArray(new String[0])) {
                if (status.containsKey(id) && status.get(id) >= STATUS_CONFIGURED){
                    continue;
                }
                try {
                    configureBean(id);
                    //setUpBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception setting-up class (" + id + "): " + e.getMessage(), e);
                }
            }

            Log.v("BeanFactory", "Setting up objects... ");
            for (String id : beans.keySet().toArray(new String[0])) {
                if (status.containsKey(id) && status.get(id) >= STATUS_INITIALIZED){
                    continue;
                }
                try {
                    //configureBean(id);
                    Log.v("BeanFactory", "Setting up object... " + id);
                    setUpBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception setting-up class (" + id + "): " + e.getMessage(), e);
                }
            }

            // avoid infinite loop
            if (max-- < 0) break;

        } while (definitionsUpdated || beansUpdated);

        Log.d("BeanFactory", "Factory initialized");
    }

    /**
     * Look for dependants and update references
     * @param id the bean identifier
     */
    private void onBeanUpdate(String id) {
        if (id == null || !beans.containsKey(id)) {
            throw new IllegalArgumentException("id or bean not found: " + id);
        }

        final Object beanUpdated = beans.get(id);
        if (beanUpdated == null) return;
        final Class<?> beanUpdatedClass = beanUpdated.getClass();

        // get beans of same type
        final List<?> duplicates = findAll(beanUpdated.getClass(), null);
        int beanIdx = duplicates.indexOf(beanUpdated);

        for (Map.Entry<String,Object> entry : beans.entrySet()){

            final Object bean = entry.getValue();
            if (bean == null) continue;

            final String beanId = entry.getKey();

            try {

                // inject first the dependencies
                Class<?> currentClass = bean.getClass();

                // loop the hierarchy
                while (currentClass != null) {

                    // inject the dependencies
                    for (Field field : currentClass.getDeclaredFields()) {
                        field.setAccessible(true);

                        // check
                        if (field.getAnnotation(Inject.class) == null) continue;

                        // qualified match
                        String named = null;
                        if (field.getAnnotation(Named.class) != null) {
                            named = Objects.requireNonNull(field.getAnnotation(Named.class)).value();
                        }

                        // type match
                        if (field.getType().isAssignableFrom(beanUpdatedClass)) {

                            // qualified match
                            if (id.equals(named)){
                                field.set(bean, beanUpdated);
                                field.setAccessible(false);
                                onBeanUpdateCallback(bean, id, beanUpdated);
                                Log.v("BeanFactory", "Dependency injected (Named). " + beanId + "." + field.getName() + ": " + beanUpdated);
                            }

                            // singleton
                            else if (duplicates.size() == 1){
                                field.set(bean, beanUpdated);
                                field.setAccessible(false);
                                onBeanUpdateCallback(bean, id, beanUpdated);
                                Log.v("BeanFactory", "Dependency injected (Singleton). " + beanId + "." + field.getName() + ": " + beanUpdated);
                            }

                            // default bean
                            else if (beanIdx == 0){
                                field.set(bean, beanUpdated);
                                field.setAccessible(false);
                                onBeanUpdateCallback(bean, id, beanUpdated);
                                Log.v("BeanFactory", "Dependency injected (Default). " + beanId + "." + field.getName() + ": " + beanUpdated);
                            }
                        }

                        // list match
                        else if (field.getType().isAssignableFrom(List.class) && field.getGenericType() instanceof ParameterizedType) {
                            final Class<?> actualTypeArgument = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
                            if (!actualTypeArgument.isAssignableFrom(beanUpdatedClass)){
                                continue;
                            }

                            // type match
                            List<?> all = findAll(actualTypeArgument, null);
                            field.set(bean, all);
                            field.setAccessible(false);
                            onBeanUpdateCallback(bean, beanId, beanUpdated);
                            Log.v("BeanFactory", "Dependency updated (List). " + beanId + "." + field.getName() + ": " + all);
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch (IllegalAccessException e) {
                // FIXME: log
            }
        }

    }

    /**
     * Invoke a method on a bean.
     *
     * @param id         bean identifier
     * @param methodName bean method
     * @return the returned object of the invocation
     */
    public Object invoke(String id, String methodName) {
        if (id == null || !beans.containsKey(id))
            throw new IllegalArgumentException("bean not found: " + id);
        if (methodName == null) throw new IllegalArgumentException("method name cant be null");

        // target bean
        final Object bean = beans.get(id);
        assert bean != null;
        try {
            Log.v("BeanFactory", "Invoking method... " + methodName);
            Method method = bean.getClass().getMethod(methodName);

            return method.invoke(bean);
        } catch (Exception e) { // ignore
            Log.e("BeanFactory", "InvocationTargetException: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static String getNamespace(String id) {
        return id != null && id.contains(".") ? id.substring(0, id.lastIndexOf('.')) : null;
    }

    public void add(String id, Class<?> clazz) {
        if (this.definitions.containsKey(id)){
            throw new IllegalArgumentException("Definition already exists: "+id);
        }
        this.definitions.put(id, clazz);
        this.definitionsUpdated = true;
        //this.parents.put(id, parent);
    }

    public void add(String id, Object object) {
        if (this.beans.containsKey(id)){
            throw new IllegalArgumentException("Bean already exists: "+id);
        }
        this.addOrReplace(id, object);
    }

    public <T> T addAndGet(String id, Class<T> clazz) {
        this.definitions.put(id, clazz);
        Object bean = this.instantiateBean(id);
        if (bean == null)
            throw new RuntimeException("exception loading bean: " + id + ", " + clazz);

        return (T)startBean(id);
    }

    public <T> T addOrReplace(String id, T object) {
        T old = (T)this.beans.put(id, object);
        this.status.put(id, STATUS_INSTANTIATED);
        this.beansUpdated = true;

        if (!initialized) return old;

        // FIXME: the Fragment is calling this method. The initialization is crashing
        // inject

        configureBean(id);

        // start
        setUpBean(id);

        // refresh dependants
        onBeanUpdate(id);

        return old;
    }

/*    public void add(Object obj, Object parent) {
        beans.put(String.valueOf(System.identityHashCode(obj)), obj);
        //parents.put(String.valueOf(System.identityHashCode(obj)), parent == null? null: String.valueOf(System.identityHashCode(parent)));
    }*/

    public <T> T find(Class<T> clazz) {
        return find(clazz, null);
    }

    /*public <T> T find(Class<T> clazz, Object self) {
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            if (!entry.getValue().equals(self)) continue;
            return find(clazz, getParentContext(entry.getKey()));
        }
        return null;
    }*/

    private <T> T find(Class<T> clazz, String namespace) {

        // search in namespace
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String entryNamespace = getNamespace(entry.getKey());
            if ((namespace == null ||
                    (entryNamespace != null && namespace.startsWith(entryNamespace)))) { // not root
                final Object candidate = entry.getValue();
                // check class
                if (candidate != null && clazz.isAssignableFrom(candidate.getClass())) {
                    return clazz.cast(candidate);
                }
            }
        }

        // search in parent
        if (namespace != null) {
            return find(clazz, getNamespace(namespace));
        }

        // search any
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            final Object candidate = entry.getValue();
            // check class
            if (candidate != null && clazz.isAssignableFrom(candidate.getClass())) {
                return clazz.cast(candidate);
            }
        }

        return null;
    }

    /*public <T> T findIn(Class<T> clazz, Object parent){
        final String parentId = String.valueOf(System.identityHashCode(parent));
        for (Map.Entry<String,String> entry : parents.entrySet()){
            // filter out same parent
            if (Objects.equals(entry.getValue(), parentId)){
                final Object candidate = beans.get(entry.getKey());
                // check class
                if (candidate != null && clazz.isAssignableFrom(candidate.getClass())) {
                    return clazz.cast(setup(candidate));
                }
            }
        }
        return null;
    }*/

    /*private <T> List<T> findAll(Class<T> clazz) {
        return findAll(clazz, null);
    }*/

    public <T> List<T> findAll(Class<T> clazz, String parent) {
        final List<T> ret = new ArrayList<>();
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            // filter out same parent
            if ((parent == null || entry.getKey().startsWith(parent))) {
                final Object candidate = entry.getValue();
                // check class
                if (candidate != null && clazz.isAssignableFrom(candidate.getClass())) {
                    ret.add(clazz.cast(candidate));
                }
            }
        }

        // order
        if (!ret.isEmpty()) {
            Collections.sort(ret, (o1, o2) -> {
                BeanOrder a1 = o1.getClass().getAnnotation(BeanOrder.class);
                BeanOrder a2 = o2.getClass().getAnnotation(BeanOrder.class);
                if (a1 != null && a2 != null) {
                    return a1.order() - a2.order();
                } else if (a1 != null) {
                    return a1.order();
                } else if (a2 != null) {
                    return -a2.order();
                }
                return 0;
            });
        }

        return ret;
    }

    public Object get(String key) {
        return beans.get(key);
    }

    public <T> T get(String key, Class<T> clazz) {
        if (beans.containsKey(key)) {
            return clazz.cast(beans.get(key));
        }
        return null;
    }

    public static List<Field> getFields(Object bean, Class<? extends Annotation> annotation) {
        List<Field> ret = new ArrayList<>();
        for (Field field : bean.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getAnnotation(annotation) != null) {
                ret.add(field);
            }
            field.setAccessible(false);
        }
        return ret;
    }

    public static Method getSetter(Field field) throws NoSuchMethodException {
        final Class<?> declaringClass = field.getDeclaringClass();
        Method ret = null;
        if (field.getType() == Boolean.TYPE) {
            String fieldCapitalized = field.getName().substring(0, 1).toUpperCase();
            if (field.getName().length() > 1) fieldCapitalized += field.getName().substring(1);
            ret = declaringClass.getMethod("set" + fieldCapitalized, Boolean.TYPE);
        }
        return ret;
    }

    private Object onBeanUpdateCallback(Object bean, String id, Object updated) {
        try {
            for (Method method : bean.getClass().getDeclaredMethods()){
                if (method.getAnnotation(OnBeanUpdate.class) != null){
                    return method.invoke(bean, id, updated);
                }
            }
            return null;
        } catch (InvocationTargetException e) {
            Log.e("BeanFactory", "Exception initializing bean: " + id + ", " + e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
