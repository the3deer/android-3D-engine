package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.animation.AnimationController;
import org.the3deer.android.engine.event.CameraEvent;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.math.Math3DUtils;

import java.util.EventObject;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Discrete Isometric Camera Handler.
 * 
 * Logic:
 * 1. Has 8 fixed base positions (corners of a cube).
 * 2. Camera position = Target + (BasePosition * Distance).
 * 3. Camera view = Target.
 * 4. Swipe interactions cycle through the 8 fixed positions.
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
public class IsometricCameraHandler implements Camera.Controller {

    private static final Logger logger = Logger.getLogger(IsometricCameraHandler.class.getSimpleName());

    // 8 fixed corners of a cube (normalized)
    private static final float s = Constants.UNIT_SIN_1; // ~0.577
    private static final float[][] BASE_POSITIONS = {
        { s,  s,  s}, // 0: Top Front Right
        {-s,  s,  s}, // 1: Top Front Left
        {-s,  s, -s}, // 2: Top Back Left
        { s,  s, -s}, // 3: Top Back Right
        { s, -s,  s}, // 4: Bottom Front Right
        {-s, -s,  s}, // 5: Bottom Front Left
        {-s, -s, -s}, // 6: Bottom Back Left
        { s, -s, -s}  // 7: Bottom Back Right
    };

    @Inject
    private Model model;

    @Inject
    private Projection projection = new OrthographicProjection();

    @Inject
    private Camera camera = new Camera("iso", 100, 100, 100, 0, 0, 0, 0, 1, 0);

    @Inject
    private AnimationController animationController;

    @Inject
    private Screen screen;

    private float[] target = new float[]{0, 0, 0};
    private int posIndex = 0; // Top Front Right
    private float distance = 100f;

    // Accumulators for discrete movement
    private float accumX = 0;
    private float accumY = 0;
    private static final float MOVE_THRESHOLD = 0.15f; // 15% of screen

    @BeanInit
    public void setUp() {
        projection.setScreen(screen);
        if (projection instanceof OrthographicProjection) {
            final OrthographicProjection ortho = (OrthographicProjection) projection;
            // Initial magnification to match a ~60 deg FOV at the default distance
            final float distance = 100f;
            final float mag = (float) (distance * Math.tan(Math.toRadians(30)));
            ortho.setXmag(mag);
            ortho.setYmag(mag);
            
            // Tighten clipping planes for better depth precision in ortho mode
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
                // External update (e.g. framing). Synchronize internal state.
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
            int currentLayer = posIndex / 4;
            int currentOffset = posIndex % 4;
            int newOffset;
            
            // Logic: Slide LEFT (dX > 0) -> Rotate camera RIGHT (offset -1)
            // Logic: Slide RIGHT (dX < 0) -> Rotate camera LEFT (offset +1)
            if (accumX > 0) newOffset = (currentOffset + 3) % 4;
            else newOffset = (currentOffset + 1) % 4;
            
            accumX = 0; // reset
            animateTo(currentLayer * 4 + newOffset);
        } else if (Math.abs(accumY) > MOVE_THRESHOLD) {
            int currentLayer = posIndex / 4;
            
            // Logic: Slide DOWN (dY > 0) -> See TOP (Layer 0)
            // Logic: Slide UP (dY < 0) -> See BOTTOM (Layer 1)
            int targetLayer = (accumY > 0) ? 0 : 1;
            
            accumY = 0; // reset
            if (targetLayer != currentLayer) {
                animateTo(targetLayer * 4 + (posIndex % 4));
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

        float upY = (posIndex < 4) ? 1.0f : -1.0f; 

        Object[] args = new Object[]{"moveTo",
                camera.getxPos(), camera.getyPos(), camera.getzPos(),
                camera.getxUp(), camera.getyUp(), camera.getzUp(),
                xp, yp, zp,
                0f, upY, 0f, // Up vector
                camera.getxView(), camera.getyView(), camera.getzView(),
                target[0], target[1], target[2]};

        if (animationController != null) {
            animationController.add(new CameraAnimation(camera, args));
        }
    }

    private void updateCameraImmediate() {
        float[] base = BASE_POSITIONS[posIndex];
        camera.set(
            target[0] + base[0] * distance,
            target[1] + base[1] * distance,
            target[2] + base[2] * distance,
            target[0], target[1], target[2],
            0, (posIndex < 4 ? 1f : -1f), 0
        );
    }
}
