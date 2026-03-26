
package org.the3deer.android.engine;

import android.util.Log;
import android.view.MotionEvent;

import org.the3deer.android.engine.camera.CameraController;
import org.the3deer.android.engine.collision.CollisionController;
import org.the3deer.android.engine.collision.CollisionEvent;
import org.the3deer.android.engine.controller.TouchController;
import org.the3deer.android.engine.controller.TouchEvent;
import org.the3deer.android.engine.event.SelectedObjectEvent;
import org.the3deer.android.engine.gui.GUI;
import org.the3deer.android.engine.gui.GUIDrawer;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.objects.Point;
import org.the3deer.android.engine.renderer.GLEvent;
import org.the3deer.android.engine.renderer.GLTouchHandler;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.util.AndroidUtils;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.util.Arrays;
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
    private Model sceneManager;
    @Inject
    private TouchController touchController;
    @Inject
    private GUI gui;
    @Inject
    private GUIDrawer guiDrawer;
    @Inject
    private CollisionController collisionController;
    @Inject
    private CameraController cameraController;
    @Inject
    private List<EventListener> listeners;
    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private EventManager eventManager;

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

                // 1. Reset Shaders (Clears Shader Cache and GpuManager VBOs/VAOs)
                shaderFactory.reset();

                // 2. Reset Textures (Mark them as not uploaded)
                final Scene currentScene = sceneManager.getActiveScene();
                if (currentScene != null && currentScene.getObjects() != null) {
                    for (Object3D obj : currentScene.getObjects()) {
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
            if (guiDrawer != null && guiDrawer.onEvent(event)) {
                return true;
            }
            /*if (scene.onEvent(event)) {
                return true;
            }*/
            final Scene scene = sceneManager.getActiveScene();
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
            final Scene scene = sceneManager.getActiveScene();
            if (scene == null) return false;

            // forward event to current scene
            return onCollisionEvent(event);

        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return false;
    }
    
    private boolean onCollisionEvent(EventObject event){
        Object3D selectedObject = sceneManager.getActiveScene().getSelectedObject();
        if (event instanceof TouchEvent) {
            Camera camera = sceneManager.getActiveScene().getActiveCamera();
            float[] right = camera.getRight();
            float[] up = camera.getUp();
            float[] pos = camera.getPos().clone();
            Math3DUtils.normalizeVector(pos);
            TouchEvent touch = (TouchEvent) event;
            if (touch.getAction() == TouchEvent.Action.ROTATE && selectedObject != null) {

                float angle = touch.getAngle();
                float factor = 1f; //1/360f * touch.getLength();


                // Log.v(TAG, "Q: Quaternion angle: " + Math.toDegrees(angle) + " ,dx:" + touch.getdX() + ", dy:" + -touch.getdY());
                Quaternion q0 = Quaternion.getQuaternion(pos, angle * factor);
                //q0.normalize();

                Quaternion multiply = Quaternion.multiply(selectedObject.getOrientation(), q0);
                selectedObject.setOrientation(multiply);

                return true;
            } else if (touch.getAction() == TouchEvent.Action.MOVE && selectedObject != null) {

                float angle = (float) (Math.atan2(-touch.getdY(), touch.getdX()));
                Log.v(TAG, "Rotating (axis:var): " + Math.toDegrees(angle) + " ,dx:" + touch.getdX() + ", dy:" + -touch.getdY());

                float[] rightd = Math3DUtils.multiply(right, touch.getdY());
                float[] upd = Math3DUtils.multiply(up, touch.getdX());
                float[] rot = Math3DUtils.add(rightd, upd);
                if (Math3DUtils.length(rot) > 0) {
                    rot = Math3DUtils.normalize2(rot);
                } else {
                    rot = new float[]{1, 0, 0};
                }

                float angle1 = touch.getLength() / 360;
                Quaternion q1 = Quaternion.getQuaternion(rot, angle1);
                //q1.normalize();

                Quaternion multiply = Quaternion.multiply(selectedObject.getOrientation(), q1);
                //multiply.normalize();

                selectedObject.setOrientation(multiply);

                return true;
            }
        } else if (event instanceof CollisionEvent) {
            Log.v(TAG, "Processing collision... " + event);
            Object3D objectToSelect = ((CollisionEvent) event).getObject();
            float[] point = ((CollisionEvent) event).getPoint();
            if (point != null) {
                Log.d(TAG, "Adding collision point " + Arrays.toString(point));

/*                // Transform collision point from local space to world space
                // For hierarchical models (GLTF with node structure), use the object's model matrix (which combines node hierarchy transforms)
                // For simple models (OBJ), use the scene's world matrix
                float[] transformMatrix = objectToSelect.getModelMatrix();
                if (transformMatrix == null || Math3DUtils.isIdentity(transformMatrix)) {
                    // Fallback to world matrix for non-animated objects or OBJ models
                    transformMatrix = this.worldMatrix;
                    Log.d(TAG, "Using scene world matrix for collision point");
                } else {
                    Log.d(TAG, "Using object's model matrix for collision point");
                }

                float[] worldPoint = new float[4];
                Matrix.multiplyMV(worldPoint, 0, transformMatrix, 0, point, 0);
                Log.d(TAG, "Collision point (world space): " + Arrays.toString(worldPoint));

                // Apply a small depth offset toward the camera (negative Z) to prevent z-fighting
                // This ensures the collision point is always visible on top of the model surface
                float depthBias = 0.1f;  // Small offset toward camera
                worldPoint[2] -= depthBias;*/


                Object3D point3D = Point.build(point).setColor(new float[]{1.0f, 0f, 0f, 1f});
                // Don't set parent node - add directly to scene at world coordinates
                //point3D.setParentNode(objectToSelect.getParentNode());
                sceneManager.getActiveScene().getObjects().add(point3D);
            }
            if (selectedObject == objectToSelect) {
                Log.v(TAG, "Unselected object " + objectToSelect);
                sceneManager.getActiveScene().setSelectedObject((null));
            } else {
                Log.i(TAG, "Selected object " + objectToSelect.getId());
                Log.d(TAG, "Selected object " + objectToSelect);
                sceneManager.getActiveScene().setSelectedObject(objectToSelect);
                if (eventManager != null) {
                    eventManager.propagate(new SelectedObjectEvent(this, objectToSelect));
                }
            }
            return true;
        }
        return false;
    }

}
