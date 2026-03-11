package org.the3deer.android_3d_model_engine.camera;


import android.util.Log;

import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.event.EventListener;

import java.util.EventObject;

import javax.inject.Inject;

public final class CameraController implements Camera.Controller, EventListener {

    interface CameraHandler extends Camera.Controller {

        void enable();
    }

    private final static String TAG = CameraController.class.getSimpleName();

    // dependencies
    @Inject
    private BeanFactory beanFactory;
    @Inject
    private Screen screen;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private CameraHandler cameraHandler;

    @Inject
    private Projection projection;


    public CameraController() {
    }

    @BeanInit
    public void setUp(){
    }

    @Override
    public boolean onEvent(EventObject event) {

        if (sceneManager == null || sceneManager.getCurrentScene() == null
                || !sceneManager.getCurrentScene().isEnabled()) {
            return false;
        }

        final Camera activeCamera = sceneManager.getCurrentScene().getCamera();
        if (activeCamera == null) {
            Log.w("CameraController", "No active camera found in the current scene.");
            return false;
        }

        if (event instanceof TouchEvent) {
            //Log.v("CameraController","event: "+event);
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
