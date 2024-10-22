package org.the3deer.util.bean;

import android.util.Log;

import androidx.annotation.Nullable;

import java.lang.annotation.Annotation;
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
 * A bean factory implementation
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

    public void init() {

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
    }

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

    public Object refreshBean(String id) {
        status.put(id, STATUS_INSTANTIATED);
        return configureBean(id);
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
        if (status == STATUS_CONFIGURED || status == STATUS_INITIALIZED) {
            return bean;
        }
        this.status.put(id, STATUS_CONFIGURED);

        // check
        if (bean == null) return null;

        // init once
        try {
            Log.d("BeanFactory", "Initializing object... " + id);

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
                        Log.v("BeanFactory", "Injecting dependency (" + id + ")... field:" + field.getName() + ", value: " + candidate);
                        field.set(bean, candidate);
                    } else {
                        Log.e("BeanFactory", "Dependency not found (" + id + ")... field:" + field.getName()+", class: "+ field.getType());
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
        if (status == STATUS_INITIALIZED) {
            return bean;
        }
        this.status.put(id, STATUS_INITIALIZED);

        // check
        if (bean == null) return null;

        try {
            Log.v("BeanFactory", "Invoking setUp()... "+id);
            Method method = bean.getClass().getMethod("setUp");
            return method.invoke(bean);
        } catch (NoSuchMethodException e) {
            for (Method method : bean.getClass().getDeclaredMethods()){
                if (method.getAnnotation(BeanPostConstruct.class) != null){
                    try {
                        //Log.e("BeanFactory", "Exception invoking @BeanPostConstruct, bean: " + id + ". " + e.getMessage(), e);
                        LOG.severe("Exception invoking @BeanPostConstruct, bean: " + id + ". " + e.getMessage());
                        LOG.throwing(this.getClass().getName(), "setUpBean", e);
                        return method.invoke(bean);
                    } catch (IllegalAccessException | InvocationTargetException ex) {
                        throw new RuntimeException(ex);
                    }
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

    public void refresh() {

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

            for (String id : beans.keySet().toArray(new String[0])) {
                if (status.containsKey(id) && Objects.equals(status.get(id), STATUS_CONFIGURED)){
                    continue;
                }
                try {
                    startBean(id);
                } catch (Exception e) {
                    Log.e("BeanFactory", "Exception setting-up class (" + id + "): " + e.getMessage(), e);
                }
            }

            // avoid infinite loop
            if (max-- < 0) break;

        } while (definitionsUpdated || beansUpdated);
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
        this.beans.put(id, object);
        this.beansUpdated = true;
        //this.parents.put(id, parent);
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

    private <T> List<T> findAll(Class<T> clazz, String parent) {
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
                    return a2.order();
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

}
