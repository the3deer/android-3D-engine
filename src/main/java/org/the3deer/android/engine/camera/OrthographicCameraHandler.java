package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.event.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.math.Math3DUtils;

import java.util.Arrays;
import java.util.EventObject;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

public class OrthographicCameraHandler implements Camera.Controller {

    private static final Logger logger = Logger.getLogger(OrthographicCameraHandler.class.getSimpleName());

    /**
     * The distance between the origin and the orthographic coordinate
     * This should be greater than the near view so it's not clipped
     */
    public static final float UNIT = Constants.UNIT * 2f; // Constants.UNIT_SIN_3;

    @Inject
    private Model model;

    @Inject
    @Named("orthographicProjection")
    private Projection projection;

    @Inject
    private Screen screen;

    private boolean initialized = false;

    // state
    private float[] savePos;
    private float[] saveView;
    private float[] saveUp;

    @Override
    public boolean onEvent(EventObject event) {
        if (!(event instanceof TouchEvent)) {
            return false;
        }

        final TouchEvent touchEvent = (TouchEvent) event;
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) {
            return false;
        }

        if (!initialized) {
            savePos = new float[3];
            saveView = new float[3];
            saveUp = new float[3];
            System.arraycopy(camera.getPos(), 0, savePos, 0, 3);
            System.arraycopy(camera.getView(), 0, saveView, 0, 3);
            System.arraycopy(camera.getUp(), 0, saveUp, 0, 3);
            initialized = true;
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
                zoom(touchEvent.getZoom() * camera.getDistance() * 0.01f);
                return true;
            case SPREAD:
                pan(-touchEvent.getdX(), touchEvent.getdY());
                return true;
        }
        return false;
    }

    @Override
    public synchronized void move(float dX, float dY) {

        logger.finest("translating..");
        float dXabs = Math.abs(dX);
        float dYabs = Math.abs(dY);
        if (dX < 0 && dXabs > dYabs) {  // right
            //float[] right = Math3DUtils.crossProduct(-getxPos(), -getyPos(), -getzPos(), getxUp(), getyUp(), getzUp());
            float[] right = Math3DUtils.crossProduct(-savePos[0], -savePos[1], -savePos[2], saveUp[0], saveUp[1], saveUp[2]);
            Math3DUtils.normalizeVector(right);
            Math3DUtils.snapToGrid(right);
            saveAndAnimate(right[0] * UNIT, right[1] * UNIT, right[2] * UNIT);
        } else if (dX > 0 && dXabs > dYabs) {
            // float[] left = Math3DUtils.crossProduct(getxUp(), getyUp(), getzUp(), -getxPos(), -getyPos(), -getzPos());
            float[] left = Math3DUtils.crossProduct(saveUp[0], saveUp[1], saveUp[2],-savePos[0], -savePos[1], -savePos[2]);
            Math3DUtils.normalizeVector(left);
            Math3DUtils.snapToGrid(left);
            saveAndAnimate(left[0] * UNIT, left[1] * UNIT, left[2] * UNIT);
        } else if (dY > 0 && dYabs > dXabs) {
            saveAndAnimate(saveUp[0] * UNIT, saveUp[1] * UNIT, saveUp[2] * UNIT);
        } else if (dY < 0 && dYabs > dXabs) {
            saveAndAnimate(-saveUp[0] * UNIT, -saveUp[1] * UNIT, -saveUp[2] * UNIT);
        }
    }

    @Override
    public synchronized void rotate(float angle) {

        if (angle < 0) {
            float[] right = Math3DUtils.crossProduct(-savePos[0], -savePos[1], -savePos[2], saveUp[0], saveUp[1], saveUp[2]);
            Math3DUtils.normalizeVector(right);
            Math3DUtils.snapToGrid(right);
            logger.finest("Rotating 90 right: " + Arrays.toString(right));
            saveAndAnimate(savePos[0], savePos[1], savePos[2], right[0], right[1], right[2]);
        } else {
            float[] left = Math3DUtils.crossProduct(saveUp[0], saveUp[1], saveUp[2], -savePos[0], -savePos[1], -savePos[2]);
            Math3DUtils.normalizeVector(left);
            Math3DUtils.snapToGrid(left);
            logger.finest("Rotating 90 left: " + Arrays.toString(left));
            saveAndAnimate(savePos[0], savePos[1], savePos[2], left[0], left[1], left[2]);
        }
    }

    private void saveAndAnimate(float xp, float yp, float zp) {

        // UP vector must be recalculated
        // cross
        float[] right = Math3DUtils.crossProduct(-savePos[0], -savePos[1], -savePos[2],
                saveUp[0], saveUp[1], saveUp[2]);
        Math3DUtils.normalizeVector(right);

        float[] cross = Math3DUtils.crossProduct(right[0], right[1], right[2], -xp, -yp, -zp);
        if (Math3DUtils.length(cross) > 0f){
            Math3DUtils.normalizeVector(cross);
            Math3DUtils.snapToGrid(cross);
            saveAndAnimate(xp,yp,zp, cross[0], cross[1], cross[2]);
        } else {
            saveAndAnimate(xp,yp,zp, saveUp[0], saveUp[1], saveUp[2]);
        }
    }

    private void saveAndAnimate(float xp, float yp, float zp, float xu, float yu, float zu) {
        this.saveAndAnimate(false, xp, yp, zp, xu, yu, zu);
    }

    private void saveAndAnimate(boolean force, float xp, float yp, float zp, float xu, float yu, float zu) {

        final Camera camera = model.getActiveScene().getActiveCamera();
        logger.finest("saveAndAnimate..."+ camera);

        synchronized (camera) {
            //if (camera.getAnimation() == null || camera.getAnimation().isFinished() || force) {




        /*delegate.setAnimation(new Object[]{"moveTo", getxPos(), getyPos(), getzPos(), getxUp(), getyUp(), getzUp(),
                savePos[0], savePos[1], savePos[2], saveUp[0], saveUp[1], saveUp[2]});*/
                Object[] args = new Object[]{"moveTo", camera.getxPos(), camera.getyPos(), camera.getzPos(),
                        camera.getxUp(), camera.getyUp(), camera.getzUp(),
                        xp, yp, zp, xu, yu, zu, camera.getxView(), camera.getyView(), camera.getzView(),
                        saveView[0], saveView[1], saveView[2]};

                savePos[0] = xp;
                savePos[1] = yp;
                savePos[2] = zp;
                saveUp[0] = xu;
                saveUp[1] = yu;
                saveUp[2] = zu;

                //delegate.setAnimation(new CameraAnimation(delegate, args));
           // }

        }
    }
}
