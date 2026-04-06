package org.the3deer.android.engine.camera;


import android.util.Log;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.event.CameraEvent;
import org.the3deer.android.engine.event.SceneEvent;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.bean.BeanInit;
import org.the3deer.bean.BeanProperty;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.math.Math3DUtils;

import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;


public final class CameraManager implements EventListener {

    private static final Logger logger = Logger.getLogger(CameraManager.class.getSimpleName());

    // dependencies
    @Inject
    private Model model;
    @Inject @Named("camera.default")
    private Camera defaultCamera;
    @Inject @Named("camera.fps")
    private Camera fpsCamera;
    @Inject
    private Map<String, Camera.Controller> controllersMap;
    @Inject
    private EventManager eventManager;
    private Camera.Controller activeController;
    
    public CameraManager() {
    }

    public List<Camera.Controller> getControllersMap() {
        return List.copyOf(controllersMap.values());
    }

    public Camera.Controller getActiveController() {
        return activeController;
    }



    @BeanInit
    public void setUp(){

        // check
        if (this.controllersMap == null || this.controllersMap.isEmpty()){
            logger.severe("No CameraController implementations found. Please ensure at least one is defined in the BeanManager.");
            return;
        }

        this.activeController = this.controllersMap.values().iterator().next(); // Set the first controller as active by default
    }

    /**
     * Toggles between Normal mode (ArcBall) and Game mode (First Person).
     */
    public void toggleController() {
        if (model == null || model.getActiveScene() == null) return;

        // Determine if we are moving TO First Person or BACK to Normal
        final boolean isMovingToFPV = !(activeController instanceof FirstPersonCameraHandler);

        if (isMovingToFPV) {
            // 1. Switch to FPS Handler
            activeController = controllersMap.get("camera.2firstPerson");
            fpsCamera.setController(activeController);

            // 2. Teleport FPS camera to current camera's position to avoid jump
            final Camera current = model.getActiveScene().getActiveCamera();
            if (current != null) {
                System.arraycopy(current.getPos(), 0, fpsCamera.getPos(), 0, 3);
                System.arraycopy(current.getView(), 0, fpsCamera.getView(), 0, 3);
            }

            // 3. Set FPS camera as active
            model.getActiveScene().setActiveCamera(fpsCamera);
        } else {
            // 1. Switch back to ArcBall Handler
            activeController = controllersMap.get("camera.1arcBall");
            defaultCamera.setController(activeController);

            // 2. Set Default camera as active
            model.getActiveScene().setActiveCamera(defaultCamera);
        }

        // Notify UI to update buttons/joysticks
        eventManager.propagate(new CameraEvent(this, null, CameraEvent.Code.HANDLER_UPDATED));
    }

    @BeanProperty(name = "controller")
    public String[] getControllerValues() {
        return controllersMap.keySet().toArray(new String[0]);
    }

    @BeanProperty(name = "controller")
    public void setActiveController(String controllerId) {

        if (controllerId == null) throw new IllegalArgumentException("Controller ID cannot be null.");

        final Camera.Controller controller = controllersMap.get(controllerId);
        if (controller == null) throw new IllegalArgumentException("No CameraController found with ID: " + controllerId);

        this.activeController = controller;
    }

    @Override
    public boolean onEvent(EventObject event) {

        if (model == null || model.getActiveScene() == null) {
            return false;
        }

        // get active camera
        Camera activeCamera = model.getActiveScene().getActiveCamera();
        if (activeCamera == null) {
            activeCamera = defaultCamera;
        }

        // check
        if (defaultCamera == null) {
            logger.warning("No active camera found in the current scene.");
            return false;
        }

        if (event instanceof TouchEvent) {
            if (activeController != null && activeController.onEvent(event)) {
                eventManager.propagate(new CameraEvent(this, activeCamera, CameraEvent.Code.CAMERA_UPDATED));
                return true;
            }
        }
        else if (event instanceof SceneEvent) {

            // get object hit
            final Object3D objectHit = ((SceneEvent) event).getData("object", Object3D.class);

            // FIXME: poc mode:
            //  check which controller is active

            if (activeController instanceof FirstPersonCameraHandler){
                // game controller

                Log.i("CameraController", "Teleporting to: " + objectHit);

                if (objectHit != null) {
                    float[] point3D = ((SceneEvent) event).getData("point", float[].class);

                    if (point3D != null) {
                        Log.i("CameraController", "Teleporting to: " + point3D[0] + ", " + point3D[1] + ", " + point3D[2]);

                        // Teleport the camera to the hit point, slightly raised (eye level)
                        // We keep the current look direction but move the position
                        final float eyeHeight = model.getActiveScene().getDimensions().getLargest() * 0.01f;
                        final float[] lookDir = Math3DUtils.substract(activeCamera.getView(), activeCamera.getPos());

                        activeCamera.getPos()[0] = point3D[0];
                        activeCamera.getPos()[1] = point3D[1] + eyeHeight;
                        activeCamera.getPos()[2] = point3D[2];

                        activeCamera.getView()[0] = activeCamera.getPos()[0] + lookDir[0];
                        activeCamera.getView()[1] = activeCamera.getPos()[1] + lookDir[1];
                        activeCamera.getView()[2] = activeCamera.getPos()[2] + lookDir[2];

                        activeCamera.setChanged(true);
                    }
                }
            }
        }
        return false;
    }
}
