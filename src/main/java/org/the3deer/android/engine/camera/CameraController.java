package org.the3deer.android.engine.camera;


import org.the3deer.bean.BeanInit;
import org.the3deer.bean.BeanManager;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.util.EventObject;
import java.util.logging.Logger;

import javax.inject.Inject;

public final class CameraController implements Camera.Controller, EventListener {

    interface CameraHandler extends Camera.Controller {

        void enable();
    }

    private static final Logger logger = Logger.getLogger(CameraController.class.getSimpleName());

    // dependencies
    @Inject
    private BeanManager beanManager;
    @Inject
    private Screen screen;
    @Inject
    private Model sceneManager;
    @Inject
    private CameraHandler cameraHandler;
    @Inject
    private Camera defaultCamera;

    @Inject
    private Projection projection;


    public CameraController() {
    }

    @BeanInit
    public void setUp(){
    }

    @Override
    public boolean onEvent(EventObject event) {

        if (sceneManager == null || sceneManager.getActiveScene() == null) {
            return false;
        }

        // get active camera
        Camera activeCamera = sceneManager.getActiveScene().getActiveCamera();
        if (activeCamera == null) {
            activeCamera = defaultCamera;
        }

        // check
        if (defaultCamera == null) {
            logger.warning("No active camera found in the current scene.");
            return false;
        }

        if (event instanceof TouchEvent) {
            //logger.finest("event: "+event);
            TouchEvent touchEvent = (TouchEvent) event;
            switch (touchEvent.getAction()) {
                case MOVE:
                    float dx1 = touchEvent.getdX();
                    float dy1 = touchEvent.getdY();
                    float max = Math.max(screen.getWidth(), screen.getHeight());
                    
                    // X axis: Invert to match "Object Drag" (swipe right = model rotates right)
                    dx1 = (float) (-dx1 / max * Math.PI * 2);
                    // Y axis: Keep sign to match "Object Drag" (swipe up = model rotates up/back)
                    dy1 = (float) (dy1 / max * Math.PI * 2);
                    
                    cameraHandler.move(dx1, dy1);
                    break;
                case ROTATE:
                    float rotation = touchEvent.getAngle();
                    cameraHandler.rotate(rotation);
                    break;
                case PINCH:
                    final float zoomFactor = ((TouchEvent) event).getZoom();
                    // Linear zoom proportional to distance
                    cameraHandler.zoom(zoomFactor * activeCamera.getDistance() * 0.01f);
                    break;
                case SPREAD:
                    final float dx = ((TouchEvent) event).getdX();
                    final float dy = ((TouchEvent) event).getdY();
                    // Pan logic: -dx moves camera left (object moves right), dy moves camera down (object moves up)
                    cameraHandler.pan(-dx, dy);
                    break;
                case CLICK:
                    break;

            }
            return true;
        }
        return false;
    }
}
