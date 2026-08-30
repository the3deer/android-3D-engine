package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.animation.AnimationController;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.util.Matrix;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.math.Math3DUtils;

import java.util.EventObject;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Orthographic camera implementation that support rotation in all 3 axis.
 * 
 * This implementation snaps to the 6 primary world faces: Front, Back, Top, Bottom, Left, Right.
 */
public class OrthographicCameraHandler implements Camera.Controller {

    private static final Logger logger = Logger.getLogger(OrthographicCameraHandler.class.getSimpleName());

    @Inject
    private Model model;

    @Inject
    private Projection projection = new OrthographicProjection();

    @Inject
    private Camera camera = new Camera("ortho", Constants.DEFAULT_CAMERA_POSITION);

    @Inject
    private AnimationController animationController;

    @Inject
    private Screen screen;

    private float[] target = new float[]{0, 0, 0};

    @BeanInit
    public void setUp() {

        // configure the default camera
        projection.setScreen(screen);

        if (projection instanceof OrthographicProjection) {
            final OrthographicProjection ortho = (OrthographicProjection) projection;
            // Initial magnification to match a ~60 deg FOV at the default distance
            final float distance = Math3DUtils.length(Constants.DEFAULT_CAMERA_POSITION);
            final float mag = (float) (distance * Math.tan(Math.toRadians(30)));
            ortho.setXmag(mag);
            ortho.setYmag(mag);
        }

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
    }

    // vars
    private CameraAnimation animation;

    @Override
    public boolean onEvent(EventObject event) {
        if (!(event instanceof TouchEvent)) {
            return false;
        }

        final TouchEvent touchEvent = (TouchEvent) event;
        if (camera == null) {
            getCamera();
        }
        if (camera == null) {
            return false;
        }

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
    public void zoom(float direction) {
        if (camera == null) getCamera();
        final Projection projection = camera.getProjection();
        if (projection instanceof OrthographicProjection) {
            final OrthographicProjection ortho = (OrthographicProjection) projection;
            // direction > 0 means spreading fingers (zoom in)
            final float sensitivity = 0.005f;
            final float factor = 1.0f - direction * sensitivity;
            if (factor > 0) {
                ortho.setXmag(ortho.getXmag() * factor);
                ortho.setYmag(ortho.getYmag() * factor);
            }
        } else {
            // For perspective projection, we move the camera
            final float[] relPos = Math3DUtils.substract(camera.getPos(), target);
            final float length = Math3DUtils.length(relPos);
            // direction > 0 means spreading fingers (zoom in)
            final float sensitivity = 0.005f;
            final float amount = direction * length * sensitivity;
            float newLength = length - amount;
            if (newLength < 0.01f) newLength = 0.01f;

            final float factor = newLength / length;
            camera.getPos()[0] = target[0] + relPos[0] * factor;
            camera.getPos()[1] = target[1] + relPos[1] * factor;
            camera.getPos()[2] = target[2] + relPos[2] * factor;
            camera.setChanged(true);
        }
    }

    @Override
    public synchronized void move(float dX, float dY) {
        if (camera == null) getCamera();

        // Enforce target-centric view for orthographic
        camera.getView()[0] = target[0];
        camera.getView()[1] = target[1];
        camera.getView()[2] = target[2];

        final float dXabs = Math.abs(dX);
        final float dYabs = Math.abs(dY);

        if (dXabs > dYabs) {
            // Horizontal swipe
            final float[] relPos = Math3DUtils.substract(camera.getPos(), target);
            final float[] up = camera.getUp();
            final float[] moveDir = Math3DUtils.crossProduct(up[0], up[1], up[2], -relPos[0], -relPos[1], -relPos[2]);
            if (dX < 0) {
                // Swipe left -> Pull surface left -> Rotate camera right
                Math3DUtils.invert(moveDir);
            }
            rotateWithMatrix(moveDir);
        } else {
            // Vertical swipe
            final float[] moveDir = camera.getUp().clone();
            if (dY > 0) {
                // Swipe down -> Pull surface down -> Rotate camera up
                Math3DUtils.invert(moveDir);
            }
            rotateWithMatrix(moveDir);
        }
    }

    @Override
    public synchronized void rotate(float angle) {
        if (camera == null) getCamera();
        final double rotationAngle = (angle > 0 ? -Math.PI : Math.PI) * 2 / 3;
        final float[] relPos = Math3DUtils.substract(camera.getPos(), target);
        final float[] posN = Math3DUtils.normalize2(relPos);
        final float[] rotMatrix = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, rotationAngle, posN[0], posN[1], posN[2]);

        float[] newUp = new float[4];
        Matrix.multiplyMV(newUp, 0, rotMatrix, 0, camera.getUp(), 0);
        Math3DUtils.normalizeVector(newUp);
        Math3DUtils.snapToGrid(newUp);

        animate(camera.getxPos(), camera.getyPos(), camera.getzPos(), newUp[0], newUp[1], newUp[2]);
    }

    private void rotateWithMatrix(float[] cross) {
        if (camera == null) getCamera();

        // 1. Determine the ideal rotation axis based on current position and desired movement direction
        final float[] relPos = Math3DUtils.substract(camera.getPos(), target);
        final float[] posN = Math3DUtils.normalize2(relPos);
        final float[] idealAxis = Math3DUtils.crossProduct(posN, cross);
        Math3DUtils.normalizeVector(idealAxis);

        // 2. Select the primary world axis that is most aligned with the ideal axis
        final float dotX = Math3DUtils.dotProduct(idealAxis, Math3DUtils.VECTOR_UNIT_X);
        final float dotY = Math3DUtils.dotProduct(idealAxis, Math3DUtils.VECTOR_UNIT_Y);
        final float dotZ = Math3DUtils.dotProduct(idealAxis, Math3DUtils.VECTOR_UNIT_Z);

        final float absX = Math.abs(dotX);
        final float absY = Math.abs(dotY);
        final float absZ = Math.abs(dotZ);

        final float[] worldAxis;
        final float alignment;
        if (absX > absY && absX > absZ) {
            worldAxis = Math3DUtils.VECTOR_UNIT_X;
            alignment = dotX;
        } else if (absY > absX && absY > absZ) {
            worldAxis = Math3DUtils.VECTOR_UNIT_Y;
            alignment = dotY;
        } else {
            worldAxis = Math3DUtils.VECTOR_UNIT_Z;
            alignment = dotZ;
        }

        // 3. Perform a 90-degree rotation around the selected world axis
        final double angle = Math.signum(alignment) * Math.PI / 2;
        final float[] rotMatrix = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, angle, worldAxis);

        // 4. Calculate new position (maintaining distance)
        final float[] newRelPos = new float[4];
        Matrix.multiplyMV(newRelPos, 0, rotMatrix, 0, new float[]{relPos[0], relPos[1], relPos[2], 1}, 0);
        newRelPos[3] = 1.0f;

        final float length = Math3DUtils.length(relPos);
        Math3DUtils.normalizeVector(newRelPos);
        Math3DUtils.snapToGrid(newRelPos);
        Math3DUtils.normalizeVector(newRelPos); // re-normalize after snap
        newRelPos[0] *= length;
        newRelPos[1] *= length;
        newRelPos[2] *= length;

        final float[] newPos = Math3DUtils.add(target, newRelPos);

        // 5. Calculate new UP vector
        final float[] newUp = new float[4];
        Matrix.multiplyMV(newUp, 0, rotMatrix, 0, camera.getUp(), 0);
        Math3DUtils.normalizeVector(newUp);
        Math3DUtils.snapToGrid(newUp);

        animate(newPos[0], newPos[1], newPos[2], newUp[0], newUp[1], newUp[2]);
    }

    private void animate(float xp, float yp, float zp, float xu, float yu, float zu) {
        if (camera == null) getCamera();
        synchronized (camera) {
            Object[] args = new Object[]{"moveTo",
                    camera.getxPos(), camera.getyPos(), camera.getzPos(),
                    camera.getxUp(), camera.getyUp(), camera.getzUp(),
                    xp, yp, zp,
                    xu, yu, zu,
                    camera.getxView(), camera.getyView(), camera.getzView(),
                    target[0], target[1], target[2]}; // Target view is always target for isometric/orthographic

            if (animationController != null && (animation == null || animation.isFinished())) {
                //logger.finest("New animation: " + Arrays.toString(args));
                animation = new CameraAnimation(camera, args);
                animationController.add(animation);
            }
        }
    }
}
