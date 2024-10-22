
package org.the3deer.android_3d_model_engine;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.camera.CameraController;
import org.the3deer.android_3d_model_engine.collision.CollisionController;
import org.the3deer.android_3d_model_engine.controller.TouchController;
import org.the3deer.android_3d_model_engine.drawer.BoundingBoxRenderer;
import org.the3deer.android_3d_model_engine.drawer.LightBulbRenderer;
import org.the3deer.android_3d_model_engine.drawer.SceneRenderer;
import org.the3deer.android_3d_model_engine.drawer.SkeletonRenderer;
import org.the3deer.android_3d_model_engine.drawer.SkyBoxRenderer;
import org.the3deer.android_3d_model_engine.drawer.WireframeRenderer;
import org.the3deer.android_3d_model_engine.gui.Axis;
import org.the3deer.android_3d_model_engine.gui.FontFactory;
import org.the3deer.android_3d_model_engine.gui.GUIDefault;
import org.the3deer.android_3d_model_engine.gui.GUISystem;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.objects.Point;
import org.the3deer.android_3d_model_engine.preferences.PreferenceFragment;
import org.the3deer.android_3d_model_engine.renderer.RendererController;
import org.the3deer.android_3d_model_engine.renderer.RendererImpl;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.android_3d_model_engine.view.GLFragment;
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
    private final Bundle bundle;
    private final Bundle extras;
    private final Activity activity;

    // variables
    private final Handler handler;
    private final BeanFactory beanFactory;
    //private final GLSurfaceView surface;
    private boolean initialized = false;


    public ModelEngine(Activity activity, Bundle bundle, Bundle extras) {
        this.activity = activity;
        this.bundle = bundle;
        this.extras = extras;
        this.beanFactory = BeanFactory.getInstance();
        //this.surface = new GLSurfaceView(activity);

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
        //init();
    }

    public static ModelEngine newInstance(Activity activity, Bundle bundle, Bundle extras){
        return new ModelEngine(activity, bundle, extras);
    }

    @NonNull
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public GLFragment getGLFragment() {
        return beanFactory.find(GLFragment.class);
    }

    public PreferenceFragment getPreferenceFragment(){
        return beanFactory.find(PreferenceFragment.class);
    }

    public void init(){

        // check
        if (initialized) return;

        try {

            initEngine();

            initGUI();

            initialized = true;

            // start
            //beanFactory.init();
            Log.i(TAG, "BeanFactory initialized");

        } catch (Exception ex) {
            Log.e(TAG, "BeanFactory initialization issue", ex);

            // clear resources
            ContentUtils.clearDocumentsProvided();
            ContentUtils.setThreadActivity(null);
        }
    }

    public void refresh(){
        try {

            // init resources
            ContentUtils.setThreadActivity(activity);

            beanFactory.find(ShaderFactory.class).reset();

            // start
            beanFactory.refresh();

            // log
            Log.i(TAG, "BeanFactory initialized");

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

        // android

        beanFactory.add("activity", this.activity);
        beanFactory.add("handler", this.handler);
        beanFactory.add("bundle", this.bundle);
        //beanFactory.add("extras", this.extras);

        // system
        beanFactory.add("10.model", ModelEngine.this);
        beanFactory.add("10.controller", ModelController.class);
        //beanFactory.add("surface", this.surface);
        //beanFactory.add("fragment_gl", GLFragment.class);
        beanFactory.add("10.shaderFactory", new ShaderFactory(activity));
        beanFactory.add("10.screen", new Screen(640, 480));
        beanFactory.add("10.renderer", RendererImpl.class);
        beanFactory.add("10.settings", PreferenceFragment.class);

        beanFactory.add("10.touchController", TouchController.class);

        // FIXME: this bean can be merged into renderer
        beanFactory.add("10.renderer0.drawerController", RendererController.class);

        // objects
        beanFactory.add("20.scene_0.scene", Scene.class);
        //beanFactory.add("scene_0.loader", SceneLoader.class);
        beanFactory.add("20.scene_0.camera", new Camera(Constants.DEFAULT_CAMERA_POSITION));
        beanFactory.add("20.scene_0.light", new Light(Constants.DEFAULT_LIGHT_LOCATION));

        // controllers
        //beanFactory.add("20.controller.animationController", AnimationController.class);
        beanFactory.add("20.controller.cameraController", CameraController.class);
        beanFactory.add("20.controller.collisionController", CollisionController.class);

        // renderers
        beanFactory.add("30.renderer1.SkyBoxDrawer", SkyBoxRenderer.class);
        beanFactory.add("30.renderer2.lightBulb", Point.build(Constants.VECTOR_ZERO)
                .setId("light").setColor(Constants.COLOR_YELLOW));
        beanFactory.add("30.renderer2.lightBulbDrawer", LightBulbRenderer.class);
        beanFactory.add("30.renderer3.sceneRenderer", SceneRenderer.class);

        // debuggers
        beanFactory.add("50.renderer4.boundingBoxDrawer", BoundingBoxRenderer.class);
        beanFactory.add("50.renderer5.wireframeDrawer", WireframeRenderer.class);
        beanFactory.add("50.renderer6.skeleton", SkeletonRenderer.class);
        //beanFactory.add("50.renderer7.shadowRenderer", ShadowRenderer.class);

    }

    private void initGUI() {
        final Camera camera = new Camera();
        camera.setProjection(Projection.ORTHOGRAPHIC);
        //camera.setScreenSize(screenWidth, screenHeight);
        beanFactory.add("80.gui.camera", camera);

        final GUISystem guiSystem = new GUISystem();
        guiSystem.setEnabled(true);
        beanFactory.add("80.gui.renderer", guiSystem);

        final FontFactory fontFactory = FontFactory.getInstance();
        fontFactory.setScreen((Screen) beanFactory.find(Screen.class));
        beanFactory.add("80.gui.font_factory", fontFactory);
        beanFactory.add("80.gui.default", GUIDefault.class);
        beanFactory.add("80.gui.axis", Axis.class);
    }

    /*@Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {

        SwitchPreference immersiveSwitch = new SwitchPreference(context);
        immersiveSwitch.setKey("activity.immersive");
        immersiveSwitch.setTitle("Immersive View");
        immersiveSwitch.setIconSpaceReserved(screen.isIconSpaceReserved());
        immersiveSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                toggleImmersive();
                return true;
            }
        });
        screen.addPreference(immersiveSwitch);
    }*/

 /*   @Override
    public boolean onSurfaceTouchEvent(MotionEvent event) {
        return propagate(new EventObject(event));
    }*/

    /*@Override
    public void onSaveInstanceState(Bundle outState) {

        Log.v(TAG, "Saving state... ");

        // assert
        if (outState == null || this.preferenceAdapters == null) return;

        // inform listeners
        for (PreferenceAdapter l : this.preferenceAdapters) {
            if (l == this) continue;
            l.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {

        Log.v(TAG,"Restoring state... "+state);

        // assert
        if (state == null || this.preferenceAdapters == null) return;

        // inform listeners
        for (PreferenceAdapter l : this.preferenceAdapters){
            if (l == this) continue;
            l.onRestoreInstanceState(state);
        }
    }*/

    //@Override
    /*public boolean propagate(EventObject event) {
        if (event instanceof RenderEvent) {
            final RenderEvent rev = (RenderEvent) event;
            //Log.v(TAG, "onEvent. RenderEvent:" + rev.getCode());
            if (rev.getCode() == RenderEvent.Code.SURFACE_CHANGED) {
                // assert
                if (screen == null || cameras == null) {
                    Log.e(TAG, "screen or camera is null. can't update model");
                    return true;
                }

                // Update model
                Log.v(TAG, "Updating screen and camera... size: "
                        + rev.getWidth() + " width, "
                        + rev.getHeight() + " height");
                screen.setSize(rev.getWidth(), rev.getHeight());
                for (Camera camera : cameras) {
                    camera.setChanged(true);
                }
            }
            //Log.v(TAG, "onEvent. RenderEvent: listeners: " + listeners);
            AndroidUtils.fireEvent(listeners, event);
            //Log.v(TAG, "onEvent. RenderEvent: finished");
        } else if (event.getSource() instanceof MotionEvent) {
            if (touchController != null) {  // event coming from glview
                touchController.onEvent(event);
            }
        } else if (event instanceof TouchEvent) {

            //Log.v(TAG,"Processing event... "+event);

*//*            if (gui.onEvent(event)) {
                return true;
            }*//*
            if (collisionController.onEvent(event)) {
                return true;
            }
            if (guiSystem.onEvent(event)) {
                return true;
            }
            if (scene.onEvent(event)) {
                return true;
            }
            if (scene.getSelectedObject() != null) {
                scene.onEvent(event);
            } else {
                cameraController.onEvent(event);
                *//*scene.onEvent(event);
                if (((TouchEvent) event).getAction() == TouchEvent.Action.PINCH) {
                    //surface.onEvent(event);
                }*//*
            }
        } else if (event instanceof CollisionEvent) {
            return scene.onEvent(event);
        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return true;
    }*/



    /*@Override
    public boolean onEvent(EventObject event) {
        if (gui != null && event instanceof FPSEvent) {
            gui.onEvent(event);
        } else if (gui != null && event instanceof SelectedObjectEvent) {
            gui.onEvent(event);
        } else if (event.getSource() instanceof MotionEvent) {
            // event coming from glview
        *//*if (touchController != null) {
            touchController.onMotionEvent((MotionEvent) event.getSource());
        }*//*
        } else if (event instanceof CollisionEvent) {
            scene.onEvent(event);
        } else if (event instanceof TouchEvent) {
            if (!gui.onEvent(event)) {
                return false;
            } else if (!collisionController.onEvent(event)) {
                scene.onEvent(event);
            }
            if (scene.getSelectedObject() != null) {
                scene.onEvent(event);
            } else {
                // cameraController.onEvent(event);
                scene.onEvent(event);
                if (((TouchEvent) event).getAction() == TouchEvent.Action.PINCH) {
                    surface.onEvent(event);
                }
            }
        }
        return true;
    }*/

    /*private void toggleImmersive() {
        this.immersiveMode = !this.immersiveMode;
        if (this.immersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
        Toast.makeText(activity, "Fullscreen " + this.immersiveMode, Toast.LENGTH_SHORT).show();
    }

    void hideSystemUI() {
        if (!this.immersiveMode) {
            return;
        }
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        final View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    void showSystemUI() {
        handler.removeCallbacksAndMessages(null);
        final View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }*/
}
