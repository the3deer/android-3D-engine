
package org.the3deer.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import org.the3deer.engine.android.shader.ShaderFactory;
import org.the3deer.engine.android.shader.v3.GpuManager;
import org.the3deer.engine.android.shadow.ShadowDrawer;
import org.the3deer.engine.android.util.AndroidURLStreamHandlerFactory;
import org.the3deer.engine.camera.CameraController;
import org.the3deer.engine.camera.DefaultCameraHandler;
import org.the3deer.engine.collision.CollisionController;
import org.the3deer.engine.collision.CollisionDrawer;
import org.the3deer.engine.controller.TouchController;
import org.the3deer.engine.decorator.BoundingBoxDrawer;
import org.the3deer.engine.decorator.LightBulbDrawer;
import org.the3deer.engine.decorator.SkeletonDrawer;
import org.the3deer.engine.decorator.SkyboxDrawer;
import org.the3deer.engine.decorator.WireframeDrawer;
import org.the3deer.engine.gui.Axis;
import org.the3deer.engine.gui.FontFactory;
import org.the3deer.engine.gui.GUI;
import org.the3deer.engine.gui.GUIDrawer;
import org.the3deer.engine.logger.LogInterceptor;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Light;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.OrthographicProjection;
import org.the3deer.engine.model.PerspectiveProjection;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.model.Screen;
import org.the3deer.engine.objects.Point;
import org.the3deer.engine.renderer.AnaglyphRenderer;
import org.the3deer.engine.renderer.DefaultRenderer;
import org.the3deer.engine.renderer.StereoscopicRenderer;
import org.the3deer.engine.scene.SceneDrawer;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventManager;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(ModelEngine.class.getSimpleName());


    // Custom handler: org/the3deer/util/android/assets/Handler.class
    static {
        System.setProperty("java.protocol.handler.pkgs", "org.the3deer.engine.android.util");
        try {
            URL.setURLStreamHandlerFactory(new AndroidURLStreamHandlerFactory());
        } catch (Error ex) {
            logger.log(Level.SEVERE, "Exception registering the android:// protocol", ex);
        }

        // Register the LogInterceptor to intercept java.util.logging and redirect to Logcat + Model.messages
        final Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(new LogInterceptor());
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum Status {
        UNKNOWN, LOADING, OK, WARNING, ERROR
    }

    // attributes
    public final String id;

    private Status status = Status.UNKNOWN;

    private String message;

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

    private boolean initialized;
    private boolean started;

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

    public boolean isInitialized() {
        return initialized;
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

    public boolean isStarted() {
        return started;
    }

    /**
     * Initialize the engine. That is, add all the engine components to the bean factory
     */
    private void initialize() {

        // debug
        logger.info("Initializing BeanFactory... hello tu " + id);


        initEngine();

        initUserInterface();

        logger.config("BeanFactory initialized");

    }

    private void initEngine() {
        // init
        logger.info("Setting up engine...");

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
        logger.info("Loading Engine... " + id);

        // init engine
        beanFactory.initialize();

        // update status
        this.initialized = true;

        logger.config("Engine loaded");
    }


    public void start() {

        // debug
        logger.info("Starting up Engine...");

        // start
        beanFactory.start();

        // update status
        this.started = true;

        // log success
        logger.info("Engine started successfully");
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

    public boolean addOrReplace(String beanId, Object bean) {
        return beanFactory.addOrReplace(beanId, bean) != null;
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

    /**
     * Reset the engine. This method should be called when the GL Surface is recreated.
     * It must be called from the GL Thread.
     */
    public void reset() {
        logger.info("Resetting engine... " + id);

        // 1. Reset Shaders
        final ShaderFactory shaderFactory = beanFactory.find(ShaderFactory.class);
        if (shaderFactory != null) {
            shaderFactory.reset();
        }

        // 2. Reset GPU Manager (VBOs/VAOs)
        final GpuManager gpuManager = beanFactory.find(GpuManager.class);
        if (gpuManager != null) {
            gpuManager.clear();
        }

        // 3. Reset Textures and Object IDs
        if (model != null && model.getScenes() != null) {
            for (Scene scene : model.getScenes()) {
                if (scene.getObjects() != null) {
                    for (Object3D obj : scene.getObjects()) {
                        if (obj.getMaterial() != null) {
                            if (obj.getMaterial().getColorTexture() != null)
                                obj.getMaterial().getColorTexture().setId(-1);
                            if (obj.getMaterial().getNormalTexture() != null)
                                obj.getMaterial().getNormalTexture().setId(-1);
                            if (obj.getMaterial().getEmissiveTexture() != null)
                                obj.getMaterial().getEmissiveTexture().setId(-1);
                            if (obj.getMaterial().getTransmissionTexture() != null)
                                obj.getMaterial().getTransmissionTexture().setId(-1);
                        }
                    }
                }
            }
        }

        logger.info("Engine reset finished");
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
