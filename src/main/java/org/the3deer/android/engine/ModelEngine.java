
package org.the3deer.android.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android.engine.camera.CameraController;
import org.the3deer.android.engine.camera.DefaultCameraHandler;
import org.the3deer.android.engine.collision.CollisionController;
import org.the3deer.android.engine.collision.CollisionDrawer;
import org.the3deer.android.engine.controller.TouchController;
import org.the3deer.android.engine.decorator.BoundingBoxDrawer;
import org.the3deer.android.engine.decorator.LightBulbDrawer;
import org.the3deer.android.engine.decorator.SkeletonDrawer;
import org.the3deer.android.engine.decorator.SkyboxDrawer;
import org.the3deer.android.engine.decorator.WireframeDrawer;
import org.the3deer.android.engine.gui.Axis;
import org.the3deer.android.engine.gui.FontFactory;
import org.the3deer.android.engine.gui.GUI;
import org.the3deer.android.engine.gui.GUIDrawer;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Light;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.PerspectiveProjection;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.objects.Point;
import org.the3deer.android.engine.renderer.AnaglyphRenderer;
import org.the3deer.android.engine.renderer.DefaultRenderer;
import org.the3deer.android.engine.renderer.StereoscopicRenderer;
import org.the3deer.android.engine.scene.SceneDrawer;
import org.the3deer.android.engine.scene.SceneLoader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.v3.GpuManager;
import org.the3deer.android.engine.shadow.ShadowDrawer;
import org.the3deer.android.util.AndroidURLStreamHandlerFactory;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventManager;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the 3D Engine Viewer Model implementation.
 * It loads a 3D model into the screen.
 * <p>
 * It creates all the basic engine components to interact with the model.
 * It relays on the {@link BeanFactory} class to manage all the beans.
 * It propagates the events using {@link java.util.EventListener}.
 * <p>
 * The engine is designed using different architectural patterns.
 *
 */
public class ModelEngine {

    private final static String TAG = ModelEngine.class.getSimpleName();

    // Custom handler: org/the3deer/util/android/assets/Handler.class
    static {
        System.setProperty("java.protocol.handler.pkgs", "org.the3deer.android.util");
        try {
            URL.setURLStreamHandlerFactory(new AndroidURLStreamHandlerFactory());
        } catch (Error ex) {
            Log.e(TAG, "Exception registering the android:// protocol", ex);
        }
    }

    public enum Status {
        UNKNOWN, LOADING, OK, WARNING, ERROR
    }

    // attributes
    public final String id;

    Status status = Status.UNKNOWN;

    String message;

    // Model
    private final Screen screen;
    private final Model model;
    private final Context context;

    // variables
    private final Handler handler;
    private final BeanFactory beanFactory = BeanFactory.getInstance();;

    private final EventManager eventManager = new ModelController();

    //private final GLSurfaceView surface;
    /**
     * Background executor for heavy loading operations.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ModelEngine(String id, @NonNull Screen screen, @NonNull Model model, Context context) {
        this.id = id;
        this.screen = screen;
        this.model = model;
        this.context = context;

        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());

        initialize();
    }


    @NonNull
    public Model getModel() {
        return model;
    }

    @NonNull
    public EventManager controller() {
        return eventManager;
    }

    @NonNull
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public boolean isLoaded() {
        return beanFactory.isInitialized();
    }


    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Initialize the engine. That is, add all the engine components to the bean factory
     */
    private void initialize() {

        // debug
        Log.i(TAG, "Initializing BeanFactory... " + id);

        initEngine();

        initUserInterface();

        Log.d(TAG, "BeanFactory initialized");

    }

    private void initEngine() {
        // init
        Log.i(TAG, "Setting up engine...");

        // model
        beanFactory.add("model", this.model);
        // activity
        beanFactory.add(Constants.BEAN_ID_CONTEXT, this.context);
        // FIXME: remove this
        beanFactory.add("handler", this.handler);

        // core
        beanFactory.add(Constants.BEAN_ID_SCREEN, this.screen);
        beanFactory.add("engine", ModelEngine.this);
        beanFactory.add(Constants.BEAN_ID_CONTROLLER, eventManager);

        // system
        beanFactory.add("10.gpuManager", GpuManager.class);
        beanFactory.add("10.shaderFactory", ShaderFactory.class);
        beanFactory.add("10.sceneLoader", SceneLoader.class);
        beanFactory.add("10.cameraHandler", DefaultCameraHandler.class);

        beanFactory.add("10.touchController", TouchController.class);

        // objects
        //beanFactory.add("20.scene_0.scene", SceneImpl.class);
        beanFactory.add("20.scene.projection", PerspectiveProjection.class);
        beanFactory.add("20.scene.camera", new Camera("default", Constants.DEFAULT_CAMERA_POSITION));
        //beanFactory.add("20.scene.light", new Light(Constants.DEFAULT_LIGHT_LOCATION));
        beanFactory.add("20.scene.light", new Light(new float[]{50f, 100f, 50f}));


        // projections
        beanFactory.add("20.scene.projections.orthographic", OrthographicProjection.class);

        // controllers
        //beanFactory.add("20.controller.animationController", AnimationController.class);
        beanFactory.add("30.controller.cameraController", CameraController.class);
        beanFactory.add("30.controller.collisionController", CollisionController.class);

        // drawers
        beanFactory.add("40.drawer0.SkyboxDrawer", SkyboxDrawer.class);
        beanFactory.add("40.drawer1.SceneDrawer", SceneDrawer.class);
        beanFactory.add("40.drawer2.boundingBoxDrawer", BoundingBoxDrawer.class);
        beanFactory.add("40.drawer3.lightBulb", Point.build(Constants.VECTOR_ZERO)
                .setId("light").setColor(Constants.COLOR_YELLOW));
        beanFactory.add("40.drawer3.lightBulbDrawer", LightBulbDrawer.class);
        beanFactory.add("40.drawer4.wireframeDrawer", WireframeDrawer.class);
        beanFactory.add("40.drawer5.skeletonDrawer", SkeletonDrawer.class);
        beanFactory.add("40.drawer6.shadowRenderer", ShadowDrawer.class);
        beanFactory.add("40.drawer7.collisionDrawer", CollisionDrawer.class);

        // renderer
        beanFactory.add("renderer.default", DefaultRenderer.class);
        beanFactory.add("renderer.anaglyph", AnaglyphRenderer.class);
        beanFactory.add("renderer.stereoscopic", StereoscopicRenderer.class);
    }

    private void initUserInterface() {

        // visualization
        beanFactory.add("gui.projection", OrthographicProjection.class);
        beanFactory.add("gui.camera", new Camera("gui", new float[]{0, 0, 10}));

        final GUIDrawer guiDrawer = new GUIDrawer();
        guiDrawer.setEnabled(true);
        beanFactory.add("gui.renderer", guiDrawer);

        final FontFactory fontFactory = FontFactory.getInstance();
        fontFactory.setScreen(beanFactory.find(Screen.class));
        beanFactory.add("gui.font_factory", fontFactory);
        beanFactory.add("gui.default", GUI.class);
        beanFactory.add("gui.axis", Axis.class);
    }

    /**
     * Initialize the engine. That is, instantiate all the engine components and invoke the initialization method.
     *
     * @throws Exception if initialization fails on any of the components
     */
    public void load() throws Exception {

        // debug
        Log.i(TAG, "Loading Engine... " + id);

        // init engine
        beanFactory.initialize();

        Log.d(TAG, "Engine loaded");

    }


    public void start() {

        // debug
        Log.i(TAG, "Starting up Engine...");

        // start
        beanFactory.start();

        // log success
        Log.i(TAG, "Engine started successfully");
    }

    /**
     * Add the specified bean to the engine, adding it to the bean factory and all the managed beans (injected as dependency).
     *
     * @param beanId the bean identifier
     * @param bean   the bean to add
     * @return <code>false</code> if the bean was not added, <code>true</code> otherwise
     */
    public boolean add(String beanId, Object bean) {
        return beanFactory.add(beanId, bean);
    }

    /**
     * Remove the specified bean from the engine, removing it from the bean factory and all the managed beans (injected as dependency).
     *
     * @param bean the bean to be removed
     * @return <code>true</code> if the bean was removed, <code>false</code> otherwise
     */
    public boolean remove(String beanId, Object bean) {
        return beanFactory.remove(beanId, bean);
    }

    public void close() {
        executor.shutdown();
        if (model != null) {
            for (Scene scene : model.getScenes()) {
                for (Object3D object : scene.getObjects()) {
                    object.dispose();
                }
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "ModelEngine{" +
                "id='" + id + '\'' +
                ", screen=" + screen +
                ", model=" + model +
                ", context=" + context +
                '}';
    }

}
