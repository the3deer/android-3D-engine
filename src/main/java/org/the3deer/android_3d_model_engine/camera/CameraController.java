package org.the3deer.android_3d_model_engine.camera;


import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
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
    private Scene scene;
    @Inject
    private Screen screen;
    @Inject
    private CameraManager cameraManager;
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

        if (scene == null){
            return false;
        }

        final Camera activeCamera = cameraManager.getActiveCamera();
        if (activeCamera == null) return false;

        if (event instanceof TouchEvent) {
            //Log.v("CameraController","event: "+event);
            TouchEvent touchEvent = (TouchEvent) event;
            switch (touchEvent.getAction()) {
                case MOVE:
                    float dx1 = touchEvent.getdX();
                    float dy1 = touchEvent.getdY();
                    float max = Math.max(screen.getWidth(), screen.getHeight());
                    dx1 = (float) (dx1 / max * Math.PI * 2);
                    dy1 = (float) (dy1 / max * Math.PI * 2);
                    cameraHandler.move(dx1, dy1);
                    break;
                case ROTATE:
                    float rotation = touchEvent.getAngle();
                    cameraHandler.rotate(rotation);
                    break;
                case PINCH:
                    final float zoomFactor = ((TouchEvent) event).getZoom();
                    cameraHandler.zoom((float) (-zoomFactor/4 * Constants.near * Math.log(scene.getCamera().getDistance())));
                    break;
                case SPREAD:
                    // TODO:
                case CLICK:
                    break;

            }
            return true;
        }
        return false;
    }
}
