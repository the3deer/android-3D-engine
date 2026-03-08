
package org.the3deer.android_3d_model_engine;

import android.util.Log;
import android.view.MotionEvent;

import androidx.preference.Preference;

import org.the3deer.android_3d_model_engine.camera.CameraController;
import org.the3deer.android_3d_model_engine.collision.CollisionController;
import org.the3deer.android_3d_model_engine.collision.CollisionEvent;
import org.the3deer.android_3d_model_engine.controller.TouchController;
import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.gui.GUIDefault;
import org.the3deer.android_3d_model_engine.gui.GUISystem;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.android_3d_model_engine.view.GLEvent;
import org.the3deer.android_3d_model_engine.view.GLTouchHandler;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;

import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

/**
 * This is the 3D Engine Viewer Model implementation.
 * It loads a 3D model into the screen.
 *
 * It creates all the basic engine components to interact with the model.
 * It relays on the {@link BeanFactory} class to manage all the beans.
 * It propagates the events using {@link java.util.EventListener}.
 * It manages the the state of the {@link Preference}
 *
 * The engine is designed using different architectural patterns.
 *
 */
public class ModelController implements EventManager, GLTouchHandler {

    private final static String TAG = ModelController.class.getSimpleName();

    // dependencies
    @Inject
    private Screen screen;
    @Inject
    private Projection projection;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private TouchController touchController;
    @Inject
    private GUIDefault gui;
    @Inject
    private GUISystem guiSystem;
    @Inject
    private CollisionController collisionController;

    @Inject
    private CameraController cameraController;
    @Inject
    private List<EventListener> listeners;
    @Inject
    private ShaderFactory shaderFactory;

    // other variables
    private long startTime;
    private boolean immersiveMode;

    public ModelController() {
    }

    @Override
    public boolean onSurfaceTouchEvent(MotionEvent event) {
        // wrap into app event
        return propagate(new EventObject(event));
    }

    @Override
    public boolean propagate(EventObject event) {
        if (event instanceof GLEvent) {
            final GLEvent rev = (GLEvent) event;
            //Log.v(TAG, "propagate. RenderEvent:" + rev.getCode());
            if (rev.getCode() == GLEvent.Code.SURFACE_CREATED) {
                shaderFactory.reset();

            } else if (rev.getCode() == GLEvent.Code.SURFACE_CHANGED) {
                // assert
                if (screen == null) {
                    Log.e(TAG, "screen or camera is null. can't update model");
                    return false;
                }

                // Update model
                Log.d(TAG, "Updating screen and camera projection... size: "
                        + rev.getWidth() + " width, "
                        + rev.getHeight() + " height");
                screen.setSize(rev.getWidth(), rev.getHeight());

                if (gui != null){
                    gui.onEvent(event);
                }
            }
            //Log.v(TAG, "onEvent. RenderEvent: listeners: " + listeners);
            AndroidUtils.fireEvent(listeners, event);
            //Log.v(TAG, "onEvent. RenderEvent: finished");
        } else if (event.getSource() instanceof MotionEvent) {
            if (touchController != null) {  // event coming from glview
                return touchController.onEvent(event);
            }
        } else if (event instanceof TouchEvent) {

            //Log.v(TAG,"Processing event... "+event);

/*            if (gui.onEvent(event)) {
                return true;
            }*/
            if (collisionController != null && collisionController.onEvent(event)) {
                return true;
            }
            if (guiSystem.onEvent(event)) {
                return true;
            }
            /*if (scene.onEvent(event)) {
                return true;
            }*/
            final Scene scene = sceneManager.getCurrentScene();
            if (scene != null && scene.getSelectedObject() != null) {
                //scene.onEvent(event);
                cameraController.onEvent(event);
            } else if (cameraController != null) {
                cameraController.onEvent(event);
                /*scene.onEvent(event);
                if (((TouchEvent) event).getAction() == TouchEvent.Action.PINCH) {
                    //surface.onEvent(event);
                }*/
            }
        } else if (event instanceof CollisionEvent){

            // check
            final Scene scene = sceneManager.getCurrentScene();
            if (scene == null) return false;

            // get selected object
            final Object3DData object = ((CollisionEvent) event).getObject();

            if (scene.getSelectedObject() == null || scene.getSelectedObject() != object) {

                // select new object
                scene.setSelectedObject(object);

            } else if (scene.getSelectedObject() == object){

                // unselect object
                scene.setSelectedObject(null);
            }
        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return false;
    }



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
