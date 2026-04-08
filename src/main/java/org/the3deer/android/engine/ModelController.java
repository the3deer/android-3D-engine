
package org.the3deer.android.engine;

import android.util.Log;

import org.the3deer.android.engine.camera.CameraManager;
import org.the3deer.android.engine.collision.CollisionController;
import org.the3deer.android.engine.collision.CollisionEvent;
import org.the3deer.android.engine.event.GLEvent;
import org.the3deer.android.engine.event.MotionEvent;
import org.the3deer.android.engine.event.SceneEvent;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.gui.GUI;
import org.the3deer.android.engine.gui.GUIDrawer;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.renderer.TouchHandler;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.android.engine.touch.TouchController;
import org.the3deer.android.util.AndroidUtils;
import org.the3deer.util.bean.BeanManager;
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
 * It relays on the {@link BeanManager} class to manage all the beans.
 * It propagates the events using {@link java.util.EventListener}.
 *
 * The engine is designed using different architectural patterns.
 *
 */
public class ModelController implements EventManager, TouchHandler {

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
    private CameraManager cameraManager;
    @Inject
    private List<EventListener> listeners;
    @Inject
    private ShaderManager shaderFactory;
    @Inject
    private EventManager eventManager;

    public ModelController() {
    }

    @Override
    public boolean onSurfaceTouchEvent(MotionEvent event) {
        // wrap into app event
        return propagate(event);
    }

    @Override
    public boolean propagate(EventObject event) {
        if (event instanceof GLEvent) {
            final GLEvent rev = (GLEvent) event;
            //logger.finest("propagate. RenderEvent:" + rev.getCode());
            if (rev.getCode() == GLEvent.Code.SURFACE_CREATED) {

                // Note: Shader and Texture reset logic moved to ModelEngine.reset(),
                // which is called by GLRenderer.onSurfaceCreated on the GL Thread.

            } else if (rev.getCode() == GLEvent.Code.SURFACE_CHANGED) {

                // update aspect ratio of the default projection
                if (projection != null) {
                    projection.setAspectRatio((float) rev.getWidth() / rev.getHeight());
                }

                // update aspect ratio of all cameras in the scene
                if (sceneManager != null) {
                    final Scene scene = sceneManager.getActiveScene();
                    if (scene != null) {
                        for (Camera camera : scene.getCameras()) {
                            camera.setProjection(rev.getWidth(), rev.getHeight());
                        }
                    }
                }

                if (gui != null){
                    gui.onEvent(event);
                }
            }
            //logger.finest("onEvent. RenderEvent: listeners: " + listeners);
            AndroidUtils.fireEvent(listeners, event);
            //logger.finest("onEvent. RenderEvent: finished");
        } else if (event instanceof MotionEvent) {
            if (touchController != null) {  // event coming from glview
                return touchController.onEvent(event);
            }
        } else if (event instanceof TouchEvent) {

            //logger.finest("Processing event... "+event);

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
                cameraManager.onEvent(event);
            } else if (cameraManager != null) {
                cameraManager.onEvent(event);
                /*scene.onEvent(event);
                if (((TouchEvent) event).getAction() == TouchEvent.Action.PINCH) {
                    //surface.onEvent(event);
                }*/
            }
        } else if (event instanceof CollisionEvent) {


            // check
            if (sceneManager.getActiveScene() == null) return false;

            // get hit
            final Object3D hit = ((CollisionEvent) event).getObject();
            final float[] point = ((CollisionEvent) event).getPoint();

            // get current selected object
            final Object3D selected = sceneManager.getActiveScene().getSelectedObject();

            // unselect if needed
            if (selected != null) {


                // FIXME: remove
                Log.d("ModelController", "CollisionEvent: " + ((CollisionEvent) event).getObject().getId()+", hit: "+hit.getId());


                if (selected.equals(hit)) {
                    // select object
                    sceneManager.getActiveScene().setSelectedObject(null);

                    // fire event
                    AndroidUtils.fireEvent(listeners, new SceneEvent(this, SceneEvent.Code.OBJECT_UNSELECTED).setData("object", hit).setData("point", point));
                }
                else {

                    // select object
                    sceneManager.getActiveScene().setSelectedObject(hit);

                    // fire event
                    AndroidUtils.fireEvent(listeners, new SceneEvent(this, SceneEvent.Code.OBJECT_SELECTED).setData("object", hit).setData("point", point));

                }
            } else {

                // select object
                sceneManager.getActiveScene().setSelectedObject(hit);

                // fire event
                AndroidUtils.fireEvent(listeners, new SceneEvent(this, SceneEvent.Code.OBJECT_SELECTED).setData("object", hit).setData("point", point));

            }

            AndroidUtils.fireEvent(listeners, event);

        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return false;
    }


}
