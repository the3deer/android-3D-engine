
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
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
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
                
                Log.i(TAG, "GL Surface Created. Resetting GPU assets...");

                // 1. Reset Shaders (Clears Shader Cache and GpuManager VBOs/VAOs)
                shaderFactory.reset();

                // 2. Reset Textures (Mark them as not uploaded)
                final Scene currentScene = sceneManager.getCurrentScene();
                if (currentScene != null && currentScene.getObjects() != null) {
                    for (Object3DData obj : currentScene.getObjects()) {
                        if (obj.getMaterial() != null) {
                            if (obj.getMaterial().getColorTexture() != null) obj.getMaterial().getColorTexture().setId(-1);
                            if (obj.getMaterial().getNormalTexture() != null) obj.getMaterial().getNormalTexture().setId(-1);
                            if (obj.getMaterial().getEmissiveTexture() != null) obj.getMaterial().getEmissiveTexture().setId(-1);
                            if (obj.getMaterial().getTransmissionTexture() != null) obj.getMaterial().getTransmissionTexture().setId(-1);
                        }
                    }
                }

            } else if (rev.getCode() == GLEvent.Code.SURFACE_CHANGED) {

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
            if (guiSystem != null && guiSystem.onEvent(event)) {
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

            // forward event to current scene
            scene.onEvent(event);

        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return false;
    }

}
