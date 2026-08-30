package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.animation.AnimationController;
import org.the3deer.android.engine.event.CameraEvent;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.math.Math3DUtils;

import java.util.EventObject;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Discrete Orthographic Camera Handler.
 * 
 * Logic:
 * 1. Has 6 fixed primary base positions (Front, Right, Back, Left, Top, Bottom).
 * 2. Camera position = Target + (BasePosition * Distance).
 * 3. Camera view = Target.
 * 4. Swipe interactions cycle through the 6 fixed positions.
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
public class OrthographicCameraHandler implements Camera.Controller {

    private static final Logger logger = Logger.getLogger(OrthographicCameraHandler.class.getSimpleName());

    // 6 fixed primary directions
    private static final float[][] BASE_POSITIONS = {
        { 0,  0,  1}, // 0: Front
        { 1,  0,  0}, // 1: Right
        { 0,  0, -1}, // 2: Back
        {-1,  0,  0}, // 3: Left
        { 0,  1,  0}, // 4: Top
        { 0, -1,  0}  // 5: Bottom
    };

    @Inject
    private Model model;

    @Inject
    private Projection projection = new OrthographicProjection();

    @Inject
    private Camera camera = new Camera("ortho", 0, 0, 100, 0, 0, 0, 0, 1, 0);

    @Inject
    private AnimationController animationController;

    @Inject
    private Screen screen;

    private float[] target = new float[]{0, 0, 0};
    private int posIndex = 0; // Front
    private float distance = 100f;
    private int lastEquatorIndex = 0;

    // Accumulators for discrete movement
    private float accumX = 0;
    private float accumY = 0;
    private static final float MOVE_THRESHOLD = 0.15f; // 15% of screen

    @BeanInit
    public void setUp() {
        projection.setScreen(screen);
        if (projection instanceof OrthographicProjection) {
            final OrthographicProjection ortho = (OrthographicProjection) projection;
            // Initial magnification
            final float mag = (float) (distance * Math.tan(Math.toRadians(30)));
            ortho.setXmag(mag);
            ortho.setYmag(mag);
            
            // Tighten clipping planes for better depth precision
            ortho.setNear(0.1f);
            ortho.setFar(1000f);
        }

        camera.setProjection(projection);
        camera.setController(this);
        camera.addListener(this);
        model.addCamera(camera);

        updateCameraImmediate();
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
        updateCameraImmediate();
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof CameraEvent) {
            final CameraEvent cameraEvent = (CameraEvent) event;
            if (cameraEvent.getCode() == CameraEvent.Code.CAMERA_UPDATED && cameraEvent.getSource() != this) {
                syncFromCamera();
            }
            return false;
        }

        if (!(event instanceof TouchEvent)) return false;

        final TouchEvent touchEvent = (TouchEvent) event;
        switch (touchEvent.getAction()) {
            case MOVE:
                final float max = Math.max(screen.getWidth(), screen.getHeight());
                move(-touchEvent.getdX() / max, touchEvent.getdY() / max);
                return true;
            case PINCH:
                zoom(touchEvent.getZoom());
                return true;
            case UP:
                accumX = 0;
                accumY = 0;
                return true;
        }
        return false;
    }

    private void syncFromCamera() {
        if (camera == null) return;
        this.target[0] = camera.getxView();
        this.target[1] = camera.getyView();
        this.target[2] = camera.getzView();
        this.distance = Math3DUtils.length(Math3DUtils.substract(camera.getPos(), camera.getView()));
    }

    @Override
    public synchronized void move(float dX, float dY) {
        accumX += dX;
        accumY += dY;

        if (Math.abs(accumX) > MOVE_THRESHOLD) {
            // Horizontal move: cycle equator Front (0), Right (1), Back (2), Left (3)
            int current = posIndex;
            if (current >= 4) current = lastEquatorIndex;

            int next;
            if (accumX > 0) next = (current + 3) % 4; // Right turn
            else next = (current + 1) % 4; // Left turn
            
            accumX = 0;
            lastEquatorIndex = next;
            animateTo(next);
        } else if (Math.abs(accumY) > MOVE_THRESHOLD) {
            // Vertical move: Snap to Top (4) or Bottom (5)
            int targetIndex = (accumY > 0) ? 4 : 5;
            
            accumY = 0;
            if (posIndex != targetIndex) {
                animateTo(targetIndex);
            } else {
                // If already at Top/Bottom, vertical swipe in opposite direction returns to equator
                animateTo(lastEquatorIndex);
            }
        }
    }

    @Override
    public void zoom(float direction) {
        final float sensitivity = 0.01f;
        float factor = 1.0f - direction * sensitivity;
        if (factor > 0) {
            distance *= factor;
            if (projection instanceof OrthographicProjection) {
                OrthographicProjection ortho = (OrthographicProjection) projection;
                ortho.setXmag(ortho.getXmag() * factor);
                ortho.setYmag(ortho.getYmag() * factor);
            }
            updateCameraImmediate();
        }
    }

    private void animateTo(int newIndex) {
        if (posIndex == newIndex) return;
        posIndex = newIndex;

        float[] base = BASE_POSITIONS[posIndex];
        float xp = target[0] + base[0] * distance;
        float yp = target[1] + base[1] * distance;
        float zp = target[2] + base[2] * distance;

        // Up vector logic for orthographic faces
        float xu = 0, yu = 1, zu = 0; // Default: Y-UP
        if (posIndex == 4) { // Top
            yu = 0; zu = -1; // Look from top -> Z is back (up in screen)
        } else if (posIndex == 5) { // Bottom
            yu = 0; zu = 1; // Look from bottom -> Z is front (up in screen)
        }

        Object[] args = new Object[]{"moveTo",
                camera.getxPos(), camera.getyPos(), camera.getzPos(),
                camera.getxUp(), camera.getyUp(), camera.getzUp(),
                xp, yp, zp,
                xu, yu, zu,
                camera.getxView(), camera.getyView(), camera.getzView(),
                target[0], target[1], target[2]};

        if (animationController != null) {
            animationController.add(new CameraAnimation(camera, args));
        }
    }

    private void updateCameraImmediate() {
        float[] base = BASE_POSITIONS[posIndex];
        float xu = 0, yu = (posIndex == 4 || posIndex == 5) ? 0 : 1, zu = (posIndex == 4) ? -1 : (posIndex == 5) ? 1 : 0;
        
        camera.set(
            target[0] + base[0] * distance,
            target[1] + base[1] * distance,
            target[2] + base[2] * distance,
            target[0], target[1], target[2],
            xu, yu, zu
        );
    }
}
