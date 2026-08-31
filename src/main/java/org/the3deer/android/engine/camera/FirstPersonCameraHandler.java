package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.PerspectiveProjection;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.android.engine.event.CameraEvent;

import java.util.EventObject;

import javax.inject.Inject;

/**
 * FirstPersonCameraHandler implements a first-person perspective control.
 * It allows looking around (rotation in place) and walking (translation).
 */
public class FirstPersonCameraHandler implements Camera.Controller {

    @Inject
    private Model model;

    @Inject
    private Screen screen;

    @Inject
    private Projection projection = new PerspectiveProjection();

    @Inject
    private Camera camera = new Camera("fps", org.the3deer.android.engine.model.Constants.DEFAULT_CAMERA_POSITION);

    @Inject
    private EventManager eventManager;

    private float[] target = new float[]{0, 0, 0};

    private boolean gravity = true;

    @BeanInit
    public void setUp() {

        // configure the default camera
        projection.setScreen(screen);
        camera.setProjection(projection);
        camera.setController(this);

        // register camera
        model.addCamera(camera);
    }

    @Override
    public Camera getCamera() {
        return camera;
    }

    @Override
    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    @Override
    public float[] getTarget() {
        return target;
    }

    @Override
    public void setTarget(float[] target) {
        this.target = target;
        if (eventManager != null) {
            eventManager.propagate(new CameraEvent(this, camera, CameraEvent.Code.CAMERA_UPDATED));
        }
    }

    public boolean isGravity() {
        return gravity;
    }

    public void setGravity(final boolean gravity) {
        this.gravity = gravity;
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (!(event instanceof TouchEvent)) {
            return false;
        }

        final TouchEvent touchEvent = (TouchEvent) event;
        switch (touchEvent.getAction()) {
            case MOVE:
                final float max = Math.max(screen.getWidth(), screen.getHeight());
                final float dx = (float) (-touchEvent.getdX() / max * Math.PI * 2);
                final float dy = (float) (touchEvent.getdY() / max * Math.PI * 2);
                move(dx, dy);
                return true;
            case ROTATE:
                rotate(touchEvent.getAngle());
                return true;
            case PINCH:
                zoom(touchEvent.getZoom());
                return true;
            case SPREAD:
                pan(-touchEvent.getdX(), touchEvent.getdY());
                return true;
        }
        return false;
    }

    @Override
    public void move(final float dX, final float dY) {

        // Apply sensitivity scaling to rotation (Look Around).
        // We factor in the scene dimensions to ensure the rotation "grooves" with the world's scale.
        // For larger environments, we dampen the speed slightly to maintain precision and avoid dizziness.
        float baseSensitivity = 0.05f;
        if (model.getActiveScene() != null && model.getActiveScene().getDimensions() != null) {
            final float sceneScale = model.getActiveScene().getDimensions().getLargest();
            if (sceneScale > 0) {
                baseSensitivity /= (1.0f + (float) Math.log10(Math.max(1.0f, sceneScale)));
            }
        }
        final float sensitivity = baseSensitivity;
        final float sDX = dX * sensitivity;
        final float sDY = dY * sensitivity;

        // Force UP to be world-up (0,1,0) to keep the horizon level (No Roll)
        final float[] worldUp = new float[]{0, 1, 0};

        // Rotation logic: Dragging moves the "view" point around the "pos" point
        final float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);

        // Calculate RIGHT vector based on World Up to prevent tilting
        final float[] right = Math3DUtils.crossProduct(worldUp, look);
        Math3DUtils.normalizeVector(right);

        // Rotate around Y axis (horizontal look - YAW)
        final float[] rotationMatrixY = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotationMatrixY, 0, sDX, 0, 1, 0);

        // Rotate around Right axis (vertical look - PITCH)
        final float[] rotationMatrixX = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotationMatrixX, 0, sDY, right[0], right[1], right[2]);

        final float[] combinedRotation = new float[16];
        Math3DUtils.multiplyMM(combinedRotation, 0, rotationMatrixY, 0, rotationMatrixX, 0);

        final float[] newLook = new float[4];
        Math3DUtils.multiplyMV(newLook, 0, combinedRotation, 0, new float[]{look[0], look[1], look[2], 0}, 0);

        // Update the view point relative to current position
        camera.getView()[0] = camera.getPos()[0] + newLook[0];
        camera.getView()[1] = camera.getPos()[1] + newLook[1];
        camera.getView()[2] = camera.getPos()[2] + newLook[2];

        // Reset UP vector to prevent any accumulated tilt
        camera.getUp()[0] = 0;
        camera.getUp()[1] = 1;
        camera.getUp()[2] = 0;

        camera.refresh();

        if (eventManager != null) {
            eventManager.propagate(new CameraEvent(this, camera, CameraEvent.Code.CAMERA_UPDATED));
        }
    }

    @Override
    public void zoom(final float direction) {
        // Pinch can still be used for fast-forward/backward movement (Walk)
        float sensitivity = 0.01f;
        if (model.getActiveScene() != null && model.getActiveScene().getDimensions() != null) {
            final float sceneRadius = model.getActiveScene().getDimensions().getRadius();
            final float baseSensitivity = Math.max(0.001f, sceneRadius / 500.0f);

            // Proximity adaptation: Slow down as we approach the target for higher precision.
            final float distToTarget = (float) Math.sqrt(
                    Math.pow(camera.getPos()[0] - target[0], 2) +
                    Math.pow(camera.getPos()[1] - target[1], 2) +
                    Math.pow(camera.getPos()[2] - target[2], 2)
            );
            final float proximityModifier = Math.max(0.1f, Math.min(1.0f, distToTarget / Math.max(1.0f, sceneRadius)));
            sensitivity = baseSensitivity * proximityModifier;
        }
        walk(direction * sensitivity);
    }

    @Override
    public void rotate(final float angle) {
        // Rolling is explicitly disabled in first person to keep things grounded
    }

    /**
     * Implementation of Walk/Strafe movement relative to camera orientation.
     * @param dX Strafe distance (Left/Right)
     * @param dY Walk distance (Forward/Backward)
     */
    @Override
    public void pan(final float dX, final float dY) {

        // Calculate current orientation vectors
        final float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);

        // Always use world-up for movement calculations
        final float[] right = Math3DUtils.crossProduct(new float[]{0, 1, 0}, look);
        Math3DUtils.normalizeVector(right);

        // Sensitivity scaling based on scene size
        float sensitivity = 0.1f;
        if (model.getActiveScene() != null && model.getActiveScene().getDimensions() != null) {
            final float sceneRadius = model.getActiveScene().getDimensions().getRadius();
            final float baseSensitivity = Math.max(0.01f, sceneRadius / 100.0f);

            // Proximity adaptation: Slow down as we approach the target for higher precision.
            final float distToTarget = (float) Math.sqrt(
                    Math.pow(camera.getPos()[0] - target[0], 2) +
                    Math.pow(camera.getPos()[1] - target[1], 2) +
                    Math.pow(camera.getPos()[2] - target[2], 2)
            );
            final float proximityModifier = Math.max(0.1f, Math.min(1.0f, distToTarget / Math.max(1.0f, sceneRadius)));
            sensitivity = baseSensitivity * proximityModifier;
        }

        final float moveX;
        final float moveY;
        final float moveZ;

        if (gravity) {
            // "Walk" mode: Move along the floor (XZ plane) at eye level
            final float[] walkDir = new float[]{look[0], 0, look[2]};
            Math3DUtils.normalizeVector(walkDir);

            // Set a fixed eye level (e.g., 1.7 units above ground level)
            // Assuming ground is at model's min Y or 0.
            final float eyeLevel = 1.7f; 
            moveX = (right[0] * dX + walkDir[0] * dY) * sensitivity;
            moveY = (eyeLevel - camera.getPos()[1]) * 0.1f; // Smoothly transition to eye level
            moveZ = (right[2] * dX + walkDir[2] * dY) * sensitivity;
        } else {
            // "Fly" mode: Move exactly where you are looking
            moveX = (right[0] * dX + look[0] * dY) * sensitivity;
            moveY = (right[1] * dX + look[1] * dY) * sensitivity;
            moveZ = (right[2] * dX + look[2] * dY) * sensitivity;
        }

        // Apply translation to both camera position and look-at point
        camera.getPos()[0] += moveX;
        camera.getPos()[1] += moveY;
        camera.getPos()[2] += moveZ;

        camera.getView()[0] += moveX;
        camera.getView()[1] += moveY;
        camera.getView()[2] += moveZ;

        // Ensure UP is always world-up
        camera.getUp()[0] = 0;
        camera.getUp()[1] = 1;
        camera.getUp()[2] = 0;

        camera.refresh();

        if (eventManager != null) {
            eventManager.propagate(new CameraEvent(this, camera, CameraEvent.Code.CAMERA_UPDATED));
        }
    }

    @Override
    public void joystick(final float dX, final float dY) {
        // Joystick provides continuous movement input.
        // dX = Strafe (Left/Right), dY = Walk (Forward/Backward)
        pan(-dX, dY);
    }

    @Override
    public void joystickLook(final float dX, final float dY) {
        // Right joystick provides continuous rotation input.
        move(dX, dY);
    }

    private void walk(final float distance) {

        final float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);

        final float moveX;
        final float moveY;
        final float moveZ;

        if (gravity) {
            // Project to XZ plane to keep height constant
            final float[] walkDir = new float[]{look[0], 0, look[2]};
            Math3DUtils.normalizeVector(walkDir);
            moveX = walkDir[0] * distance;
            moveY = 0;
            moveZ = walkDir[2] * distance;
        } else {
            // Fly in look direction
            moveX = look[0] * distance;
            moveY = look[1] * distance;
            moveZ = look[2] * distance;
        }

        camera.getPos()[0] += moveX;
        camera.getPos()[1] += moveY;
        camera.getPos()[2] += moveZ;

        camera.getView()[0] += moveX;
        camera.getView()[1] += moveY;
        camera.getView()[2] += moveZ;

        camera.refresh();
    }
}
