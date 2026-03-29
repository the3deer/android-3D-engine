
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
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.PerspectiveProjection;
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
import org.the3deer.android.util.ContentUtils;
import org.the3deer.util.bean.BeanFactory;

import java.net.URL;

/**
 * This is the 3D Engine Viewer Model implementation.
 * It loads a 3D model into the screen.
 * <p>
 * It creates all the basic engine components to interact with the model.
 * It relays on the {@link BeanFactory} class to manage all the beans.
 * It propagates the events using {@link java.util.EventListener}.
 *
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

    // attributes
    public final String id;

    // Model
    private final Model model;
    private Context context;

    // variables
    private final Handler handler;
    private final BeanFactory beanFactory;
    //private final GLSurfaceView surface;
    private boolean initialized = false;


    public ModelEngine(String id, Model model, Context context) {
        this.id = id;
        this.model = model;
        this.context = context;
        this.beanFactory = BeanFactory.getInstance();
        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
        //init();
    }

    /**
     * Initialize the engine. That is, instantiate all the engine components and invoke the initialization method.
     *
     * @throws Exception if initialization fails on any of the components
     */
    public void init() throws Exception {

        // check
        if (initialized) return;

        // debug
        Log.i(TAG, "Initializing Engine... "+id);

        // check
        //if (activity == null) throw new Exception("Activity is null");

        try {
            // init resources
            //ContentUtils.setContext(activity);

            initEngine();

            initGUI();

            // init engine
            beanFactory.init();

            initialized = true;

            Log.d(TAG, "BeanFactory initialized");

        } catch (Exception ex) {
            Log.e(TAG, "BeanFactory initialization issue", ex);

            // clear resources
            ContentUtils.clearDocumentsProvided();

            throw ex;
        }
    }

    public Model getModel(){
        return model;
    }

    @NonNull
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void start(){
        try {

            // debug
            Log.d(TAG, "Starting up Engine...");

            //beanFactory.find(ShaderFactory.class).reset();

            // start
            beanFactory.start();

            // log
            Log.i(TAG, "Engine started successfully");

        } catch (Exception ex) {
            Log.e(TAG, "BeanFactory refresh issue", ex);

            // clear resources
            //ContentUtils.clearDocumentsProvided();
        }
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
        /*if (beanFactory.get(Constants.BEAN_ID_SCREEN) == null) {
            beanFactory.add(Constants.BEAN_ID_SCREEN, new Screen(640, 480));
        }*/
        beanFactory.add("engine", ModelEngine.this);
        beanFactory.add("controller", ModelController.class);

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

    private void initGUI() {

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
     * Add the specified bean to the engine, adding it to the bean factory and all the managed beans (injected as dependency).
     *
     * @param beanId the bean identifier
     * @param bean the bean to add
     * @return <code>false</code> if the bean was not added, <code>true</code> otherwise
     */
    public boolean add(String beanId, Object bean){
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

    @Override
    public String toString() {
        return "ModelEngine{" +
                "id='" + id + '\'' +
                ", model=" + model +
                ", activity=" + context +
                ", beanFactory=" + beanFactory +
                '}';
    }

}
