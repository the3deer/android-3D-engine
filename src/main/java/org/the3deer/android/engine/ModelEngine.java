
package org.the3deer.android.engine;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android.engine.camera.CameraController;
import org.the3deer.android.engine.camera.DefaultCameraHandler;
import org.the3deer.android.engine.collision.CollisionController;
import org.the3deer.android.engine.controller.TouchController;
import org.the3deer.android.engine.decorator.BoundingBoxDrawer;
import org.the3deer.android.engine.decorator.LightBulbDrawer;
import org.the3deer.android.engine.decorator.SkeletonDrawer;
import org.the3deer.android.engine.decorator.SkyBoxDrawer;
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
 * It manages the the state of the {@link androidx.preference.Preference}
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
    private final String id;

    // Model
    private final Model model;
    private final Activity activity;

    // variables
    private final Handler handler;
    private final BeanFactory beanFactory;
    //private final GLSurfaceView surface;
    private boolean initialized = false;


    public ModelEngine(String id, Model model, Activity activity) {
        this.id = id;
        this.model = model;
        this.activity = activity;
        this.beanFactory = BeanFactory.getInstance();
        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
        //init();
    }

    @NonNull
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Initialize the engine. That is, instantiate all the engine components and invoke the initialization method.
     *
     * @throws Exception if initialization fails on any of the components
     */
    public void init() throws Exception {

        // check
        if (initialized) return;

        // check
        if (activity == null) throw new Exception("Activity is null");

        try {
            // init resources
            ContentUtils.setThreadActivity(activity);

            initEngine();

            initGUI();

            initialized = true;

            // start
            //beanFactory.init();
            Log.d(TAG, "BeanFactory initialized");

        } catch (Exception ex) {
            Log.e(TAG, "BeanFactory initialization issue", ex);

            // clear resources
            ContentUtils.clearDocumentsProvided();
            ContentUtils.setThreadActivity(null);

            throw ex;
        }
    }

    public void start(){
        try {

            // init resources
            ContentUtils.setThreadActivity(activity);

            //beanFactory.find(ShaderFactory.class).reset();

            // start
            beanFactory.init();

            // log
            Log.d(TAG, "BeanFactory initialized");

        } catch (Exception ex) {
            Log.e(TAG, "BeanFactory refresh issue", ex);

            // clear resources
            ContentUtils.clearDocumentsProvided();
            ContentUtils.setThreadActivity(null);
        }

    }

    private void initEngine() {
        // init
        Log.i(TAG, "Setting up engine...");

        // model
        beanFactory.add("model", this.model);
        // activity
        beanFactory.add("activity", this.activity);
        // FIXME: remove this
        beanFactory.add("handler", this.handler);

        // core
        beanFactory.add(Constants.BEAN_ID_SCREEN, new Screen(640, 480));
        beanFactory.add("engine", ModelEngine.this);
        beanFactory.add("controller", ModelController.class);

        // system
        beanFactory.add("10.gpuManager", GpuManager.class);
        beanFactory.add("10.shaderFactory", ShaderFactory.class);
        beanFactory.add("10.sceneLoader", SceneLoader.class);
        beanFactory.add("10.cameraHandler", DefaultCameraHandler.class);
        //beanFactory.add("10.settings", PreferenceFragment.class);


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
        beanFactory.add("40.drawer0.SkyBoxDrawer", SkyBoxDrawer.class);
        beanFactory.add("40.drawer1.SceneDrawer", SceneDrawer.class);
        beanFactory.add("40.drawer2.boundingBoxDrawer", BoundingBoxDrawer.class);
        beanFactory.add("40.drawer3.lightBulb", Point.build(Constants.VECTOR_ZERO)
                .setId("light").setColor(Constants.COLOR_YELLOW));
        beanFactory.add("40.drawer3.lightBulbDrawer", LightBulbDrawer.class);
        beanFactory.add("40.drawer4.wireframeDrawer", WireframeDrawer.class);
        beanFactory.add("40.drawer5.skeletonDrawer", SkeletonDrawer.class);
        beanFactory.add("40.drawer6.shadowRenderer", ShadowDrawer.class);

        // renderer
        beanFactory.add("50.renderer.defaultRenderer", DefaultRenderer.class);
        beanFactory.add("50.renderer.anaglyphRenderer", AnaglyphRenderer.class);
        beanFactory.add("50.renderer.stereoscopicRenderer", StereoscopicRenderer.class);
    }

    private void initGUI() {

        // visualization
        beanFactory.add("80.gui.projection", OrthographicProjection.class);
        beanFactory.add("80.gui.camera", new Camera("gui", new float[]{0, 0, 10}));

        final GUIDrawer guiDrawer = new GUIDrawer();
        guiDrawer.setEnabled(true);
        beanFactory.add("80.gui.renderer", guiDrawer);

        final FontFactory fontFactory = FontFactory.getInstance();
        fontFactory.setScreen(beanFactory.find(Screen.class));
        beanFactory.add("80.gui.font_factory", fontFactory);
        beanFactory.add("80.gui.default", GUI.class);
        beanFactory.add("80.gui.axis", Axis.class);
    }

    @Override
    public String toString() {
        return "ModelEngine{" +
                "id='" + id + '\'' +
                ", model=" + model +
                ", activity=" + activity +
                ", beanFactory=" + beanFactory +
                '}';
    }
}
