
package org.the3deer.engine;

import org.the3deer.engine.android.shader.ShaderFactory;
import org.the3deer.engine.android.util.AndroidUtils;
import org.the3deer.engine.camera.CameraController;
import org.the3deer.engine.collision.CollisionController;
import org.the3deer.engine.collision.CollisionEvent;
import org.the3deer.engine.android.touch.TouchController;
import org.the3deer.engine.event.TouchEvent;
import org.the3deer.engine.event.GLEvent;
import org.the3deer.engine.event.MotionEvent;
import org.the3deer.engine.event.SelectedObjectEvent;
import org.the3deer.engine.gui.GUI;
import org.the3deer.engine.gui.GUIDrawer;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Projection;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.renderer.TouchHandler;
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
    private CameraController cameraController;
    @Inject
    private List<EventListener> listeners;
    @Inject
    private ShaderFactory shaderFactory;
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
                cameraController.onEvent(event);
            } else if (cameraController != null) {
                cameraController.onEvent(event);
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

            // get current selected object
            final Object3D selected = sceneManager.getActiveScene().getSelectedObject();

            // unselect if needed
            if (selected != null && selected == hit) {

                // select object
                sceneManager.getActiveScene().setSelectedObject(null);

                // fire event
                AndroidUtils.fireEvent(listeners, new SelectedObjectEvent(this, null));


            } else {

                // select object
                sceneManager.getActiveScene().setSelectedObject(hit);

                // fire event
                AndroidUtils.fireEvent(listeners, new SelectedObjectEvent(this, hit));

            }

            AndroidUtils.fireEvent(listeners, event);

        } else {
            AndroidUtils.fireEvent(listeners, event);
        }
        return false;
    }


}
