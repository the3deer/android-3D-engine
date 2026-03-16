
package org.the3deer.android_3d_model_engine;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.camera.CameraController;
import org.the3deer.android_3d_model_engine.camera.DefaultCameraHandler;
import org.the3deer.android_3d_model_engine.collision.CollisionController;
import org.the3deer.android_3d_model_engine.controller.TouchController;
import org.the3deer.android_3d_model_engine.decorator.BoundingBoxDrawer;
import org.the3deer.android_3d_model_engine.decorator.LightBulbDrawer;
import org.the3deer.android_3d_model_engine.decorator.SkeletonDrawer;
import org.the3deer.android_3d_model_engine.decorator.SkyBoxDrawer;
import org.the3deer.android_3d_model_engine.decorator.WireframeDrawer;
import org.the3deer.android_3d_model_engine.gui.Axis;
import org.the3deer.android_3d_model_engine.gui.FontFactory;
import org.the3deer.android_3d_model_engine.gui.GUIDefault;
import org.the3deer.android_3d_model_engine.gui.GUISystem;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.model.impl.OrthographicProjection;
import org.the3deer.android_3d_model_engine.model.impl.PerspectiveProjection;
import org.the3deer.android_3d_model_engine.objects.Point;
import org.the3deer.android_3d_model_engine.preferences.PreferenceFragment;
import org.the3deer.android_3d_model_engine.preferences.Preferences;
import org.the3deer.android_3d_model_engine.renderer.AnaglyphRenderer;
import org.the3deer.android_3d_model_engine.renderer.DefaultRenderer;
import org.the3deer.android_3d_model_engine.renderer.StereoscopicRenderer;
import org.the3deer.android_3d_model_engine.shader.v3.GpuManager;
import org.the3deer.android_3d_model_engine.renderer.RendererPreferences;
import org.the3deer.android_3d_model_engine.scene.SceneDrawer;
import org.the3deer.android_3d_model_engine.scene.SceneLoader;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.android_3d_model_engine.shader.ShaderPreferences;
import org.the3deer.android_3d_model_engine.shadow.ShadowDrawer;
import org.the3deer.util.android.AndroidURLStreamHandlerFactory;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.bean.BeanFactory;

import java.net.URL;

/**
 * This is the 3D Engine Viewer Model implementation.
 * It loads a 3D model into the screen.
 *
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
        System.setProperty("java.protocol.handler.pkgs", "org.the3deer.util.android");
        try {
            URL.setURLStreamHandlerFactory(new AndroidURLStreamHandlerFactory());
        } catch (Error ex) {
            Log.e(TAG, "Exception registering the android:// protocol", ex);
        }
    }

    // params
    private final String id;
    private final Bundle bundle;
    private final Bundle extras;
    private final Activity activity;

    // variables
    private final Handler handler;
    private final BeanFactory beanFactory;
    //private final GLSurfaceView surface;
    private boolean initialized = false;


    public ModelEngine(String id, Activity activity, Bundle bundle, Bundle extras) {
        this.id = id;
        this.activity = activity;
        this.bundle = bundle;
        this.extras = extras;
        this.beanFactory = BeanFactory.getInstance();
        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
        //init();
    }

    public static ModelEngine newInstance(String id, Activity activity, Bundle bundle, Bundle extras){
        return new ModelEngine(id, activity, bundle, extras);
    }

    public void defaultInit(){

    }

    @NonNull
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public PreferenceFragment getPreferenceFragment(){
        return beanFactory.find(PreferenceFragment.class);
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

    private void initEngine() throws Exception {
        // init
        Log.i(TAG, "Setting up engine...");

        // activity
        beanFactory.add("activity", this.activity);
        // FIXME: remove this
        beanFactory.add("handler", this.handler);

        // state + arguments
        beanFactory.add("bundle", this.bundle);
        beanFactory.add("extras", this.extras);

        // core
        beanFactory.add(Constants.BEAN_ID_SCREEN, new Screen(640, 480));
        beanFactory.add("model", ModelEngine.this);
        beanFactory.add("controller", ModelController.class);

        // system
        beanFactory.add("10.gpuManager", GpuManager.class);
        beanFactory.add("10.shaderFactory", ShaderFactory.class);
        beanFactory.add("10.sceneLoader", SceneLoader.class);
        beanFactory.add("10.cameraHandler", DefaultCameraHandler.class);
        //beanFactory.add("10.settings", PreferenceFragment.class);

        beanFactory.add("10.shaderPreferences", ShaderPreferences.class);
        beanFactory.add("10.touchController", TouchController.class);

        // FIXME: this bean can be merged into renderer
        beanFactory.add("10.renderer0.drawerController", RendererPreferences.class);

        // objects
        //beanFactory.add("20.scene_0.scene", SceneImpl.class);
        beanFactory.add("20.scene.sceneManager", SceneManager.class);
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
        beanFactory.add("50.renderer0.defaultRenderer", DefaultRenderer.class);
        beanFactory.add("50.renderer1.anaglyphRenderer", AnaglyphRenderer.class);
        beanFactory.add("50.renderer2.stereoscopicRenderer", StereoscopicRenderer.class);

        // preferences
        beanFactory.add("100.preferences", Preferences.class);
    }

    private void initGUI() {

        // visualization
        beanFactory.add("80.gui.projection", OrthographicProjection.class);
        beanFactory.add("80.gui.camera", new Camera("gui", new float[]{0, 0, 10}));

        final GUISystem guiSystem = new GUISystem();
        guiSystem.setEnabled(true);
        beanFactory.add("80.gui.renderer", guiSystem);

        final FontFactory fontFactory = FontFactory.getInstance();
        fontFactory.setScreen((Screen) beanFactory.find(Screen.class));
        beanFactory.add("80.gui.font_factory", fontFactory);
        beanFactory.add("80.gui.default", GUIDefault.class);
        beanFactory.add("80.gui.axis", Axis.class);
    }
}
