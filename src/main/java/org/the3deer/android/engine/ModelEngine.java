package org.the3deer.android.engine;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.the3deer.android.engine.camera.ArcBallCameraHandler;
import org.the3deer.android.engine.camera.CameraManager;
import org.the3deer.android.engine.camera.FirstPersonCameraHandler;
import org.the3deer.android.engine.collision.CollisionController;
import org.the3deer.android.engine.collision.CollisionDrawer;
import org.the3deer.android.engine.decorator.BoundingBoxDrawer;
import org.the3deer.android.engine.decorator.LightBulbDrawer;
import org.the3deer.android.engine.decorator.SkeletonDrawer;
import org.the3deer.android.engine.decorator.SkyboxDrawer;
import org.the3deer.android.engine.decorator.WireframeDrawer;
import org.the3deer.android.engine.gui.Axis;
import org.the3deer.android.engine.gui.FontFactory;
import org.the3deer.android.engine.gui.GUI;
import org.the3deer.android.engine.gui.GUIDrawer;
import org.the3deer.android.engine.logger.LogInterceptor;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Light;
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
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.android.engine.shader.v3.GpuManager;
import org.the3deer.android.engine.shadow.ShadowDrawer;
import org.the3deer.android.engine.touch.TouchController;
import org.the3deer.android.util.AndroidURLStreamHandlerFactory;
import org.the3deer.android.util.ContentUtils;
import org.the3deer.util.bean.BeanManager;
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
 * It relays on the {@link BeanManager} class to manage all the beans.
 * It propagates the events using {@link java.util.EventListener}.
 * <p>
 * The engine is designed using different architectural patterns.
 *
 */
public final class ModelEngine {

    private static final Logger logger = Logger.getLogger(ModelEngine.class.getSimpleName());


    // Custom handler: org/the3deer/util/android/assets/Handler.class
    static {
        System.setProperty("java.protocol.handler.pkgs", "org.the3deer.android.engine.util");
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
    private final BeanManager beanManager = BeanManager.getInstance();;

    private final EventManager eventManager = new ModelController();

    //private final GLSurfaceView surface;
    /**
     * Background executor for heavy loading operations.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private boolean initialized;
    private boolean started;

    public ModelEngine(String id, Screen screen, Model model, Context context) {
        this.id = id;
        this.screen = screen;
        this.model = model;
        this.context = context;

        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());

        initialize();
    }


    public Model getModel() {
        return model;
    }

    public EventManager controller() {
        return eventManager;
    }

    public BeanManager getBeanFactory() {
        return beanManager;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isLoaded() {
        return beanManager.isInitialized();
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
        beanManager.add("model", this.model);
        // activity
        beanManager.add(Constants.BEAN_ID_CONTEXT, this.context);
        // FIXME: remove this
        beanManager.add("handler", this.handler);

        // core
        beanManager.add(Constants.BEAN_ID_SCREEN, this.screen);
        beanManager.add("engine", ModelEngine.this);
        beanManager.add("shader.factory", ShaderFactory.class);
        beanManager.add("shader.manager", ShaderManager.class);
        beanManager.add(Constants.BEAN_ID_CONTROLLER, eventManager);

        // system
        beanManager.add("10.gpuManager", GpuManager.class);
        beanManager.add("10.touchController", TouchController.class);

        // camera
        beanManager.add("camera.projection", PerspectiveProjection.class);
        beanManager.add("camera.default", new Camera("default", Constants.DEFAULT_CAMERA_POSITION));
        beanManager.add("camera.fps", new Camera("fps", Constants.DEFAULT_CAMERA_POSITION));
        beanManager.add("camera.1arcBall", ArcBallCameraHandler.class);
        beanManager.add("camera.2firstPerson", FirstPersonCameraHandler.class);

        // objects
        beanManager.add("20.scene.light", new Light(new float[]{50f, 100f, 50f}));

        // projections
        beanManager.add("20.scene.projections.orthographic", OrthographicProjection.class);

        // controllers
        //beanFactory.add("20.controller.animationController", AnimationController.class);
        beanManager.add("30.controller.cameraController", CameraManager.class);
        beanManager.add("30.controller.collisionController", CollisionController.class);

        // drawers
        beanManager.add("40.drawer0.SkyboxDrawer", SkyboxDrawer.class);
        beanManager.add("40.drawer1.SceneDrawer", SceneDrawer.class);
        beanManager.add("40.drawer2.boundingBoxDrawer", BoundingBoxDrawer.class);
        beanManager.add("40.drawer3.lightBulb", Point.build(Constants.VECTOR_ZERO)
                .setId("light").setColor(Constants.COLOR_YELLOW));
        beanManager.add("40.drawer3.lightBulbDrawer", LightBulbDrawer.class);
        beanManager.add("40.drawer4.wireframeDrawer", WireframeDrawer.class);
        beanManager.add("40.drawer5.skeletonDrawer", SkeletonDrawer.class);
        beanManager.add("40.drawer6.shadowRenderer", ShadowDrawer.class);
        beanManager.add("40.drawer7.collisionDrawer", CollisionDrawer.class);

        // renderer
        beanManager.add("renderer.default", DefaultRenderer.class);
        beanManager.add("renderer.anaglyph", AnaglyphRenderer.class);
        beanManager.add("renderer.stereoscopic", StereoscopicRenderer.class);
    }

    private void initUserInterface() {

        // visualization
        beanManager.add("gui.projection", OrthographicProjection.class);
        beanManager.add("gui.camera", new Camera("gui", new float[]{0, 0, 10}));

        final GUIDrawer guiDrawer = new GUIDrawer();
        beanManager.add("gui.renderer", guiDrawer);

        beanManager.add("gui.1font_factory", FontFactory.class);
        beanManager.add("gui.2axis", Axis.class);
        beanManager.add("gui.3default", GUI.class);
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
        beanManager.initialize();

        // update status
        this.initialized = true;

        logger.config("Engine loaded");
    }


    public void start() {

        // debug
        logger.info("Starting up Engine...");

        // start
        beanManager.start();

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
        return beanManager.add(beanId, bean);
    }

    public boolean addOrReplace(String beanId, Object bean) {
        return beanManager.addOrReplace(beanId, bean) != null;
    }

    /**
     * Remove the specified bean from the engine, removing it from the bean factory and all the managed beans (injected as dependency).
     *
     * @param bean the bean to be removed
     * @return <code>true</code> if the bean was removed, <code>false</code> otherwise
     */
    public boolean remove(String beanId, Object bean) {
        return beanManager.remove(beanId, bean);
    }

    /**
     * Reset the engine. This method should be called when the GL Surface is recreated.
     * It must be called from the GL Thread.
     */
    public void reset() {
        logger.info("Resetting engine... " + id);

        // 1. Reset Shaders
        final ShaderManager shaderFactory = beanManager.find(ShaderManager.class);
        if (shaderFactory != null) {
            shaderFactory.reset();
        }

        // 2. Reset GPU Manager (VBOs/VAOs)
        final GpuManager gpuManager = beanManager.find(GpuManager.class);
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
            beanManager.close();
        }
        ContentUtils.clearDocumentsProvided();
    }

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
