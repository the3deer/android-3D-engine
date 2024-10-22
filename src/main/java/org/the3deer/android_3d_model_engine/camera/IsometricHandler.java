package org.the3deer.android_3d_model_engine.camera;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.AnimationController;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.util.math.Math3DUtils;

import java.util.Arrays;

import javax.inject.Inject;

/**
 * Isometric camera implementation that support rotation in all 3 axis
 * This implementation supports a memory for saving and restoring the last location
 *
 * This implementation support all possible isometric positions,
 * that is 8 (points) * 3 (directions) = 24 total
 *
 * Every position corresponds to 1 isometric key point (i.e. 1x,1y,1z)
 * Every position is facing the world origin (0,0,0)
 *
 * For every rotation, the camera will land on the next isometric key point
 *
 */
public class IsometricHandler implements CameraController.Handler {

    /**
     * The distance between the origin and the isometric coordinate
     * This should be greater than the near view so it's not clipped
     */
    public static final float UNIT = Constants.UNIT; // Constants.UNIT_SIN_3;

    @Inject
    private Camera camera;
    @Inject
    private AnimationController animationController;

    private boolean initialized = false;

    // state
    private float[] savePos = new float[3];
    private float[] saveView = new float[3];
    private float[] saveUp = new float[3];

    // vars
    private CameraAnimation animation;

    public void setUp() {
        this.savePos[0] = UNIT;
        this.savePos[1] = UNIT;
        this.savePos[2] = UNIT;
        this.saveUp[0] = -Constants.UNIT_SIN_1;
        this.saveUp[1] = Constants.UNIT_SIN_1;
        this.saveUp[2] = -Constants.UNIT_SIN_1;
        this.saveView[0] = 0;
        this.saveView[1] = 0;
        this.saveView[2] = 0;

/*        this.savePos = camera.getPos().clone();
        this.saveUp = camera.getUp().clone();
        this.saveView = camera.getView().clone();*/
    }

    @Override
    public void enable(){
        camera.setController(this);
        camera.setProjection(Projection.ORTHOGRAPHIC);
        camera.setChanged(true);
        saveAndAnimate(this.savePos[0], this.savePos[1], this.savePos[2], this.saveUp[0], this.saveUp[1], this.saveUp[2]);
    }

    @Override
    public synchronized void move(float dX, float dY) {
        float dXabs2 = Math.abs(dX);
        float dYabs2 = Math.abs(dY);
        if (dXabs2 > dYabs2 && dX < 0) {
            translateCameraIsometricRight();
        } else if (dXabs2 > dYabs2 && dX > 0) {
            translateCameraIsometricLeft();
        } else if (dXabs2 < dYabs2 && dY > 0) {
            translateCameraIsometricUp();
        } else if (dXabs2 < dYabs2 && dY < 0) {
            translateCameraIsometricDown();
        }
    }

    @Override
    public synchronized void rotate(float angle) {
        final double rotationAngle = (angle > 0 ? -Math.PI : Math.PI) * 2 / 3;
        final float[] posN = Math3DUtils.normalize2(savePos);
        final float[] rotMatrix = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, rotationAngle, posN[0], posN[1], posN[2]);

        float[] newUp = new float[4];
        Matrix.multiplyMV(newUp, 0, rotMatrix, 0, this.saveUp, 0);
        Math3DUtils.normalizeVector(newUp);
        Math3DUtils.snapToGrid(newUp);

        //saveAndAnimate(getxPos(), getyPos(), getzPos(), newUp[0], newUp[1], newUp[2]);
        saveAndAnimate(savePos[0], savePos[1], savePos[2], newUp[0], newUp[1], newUp[2]);
    }

    private void translateCameraIsometricDown() {
        // saveAndAnimate(-getxUp(), -getyUp(), -getzUp(), getxPos(), getyPos(), getzPos());
        float length = Math3DUtils.length(savePos);
        saveAndAnimate(-saveUp[0] * UNIT, -saveUp[1] * UNIT, -saveUp[2] * UNIT,
                savePos[0]/length, savePos[1]/length, savePos[2]/length);
    }

    private void translateCameraIsometricUp() {
        // saveAndAnimate(getxUp(), getyUp(), getzUp(), -getxPos(), -getyPos(), -getzPos());
        // saveAndAnimate(saveUp[0], saveUp[1], saveUp[2],-savePos[0], -savePos[1], -savePos[2]);
        float length = Math3DUtils.length(savePos);
        saveAndAnimate(saveUp[0] * UNIT, saveUp[1] * UNIT, saveUp[2] * UNIT,
                -savePos[0]/length, -savePos[1]/length, -savePos[2]/length);
    }

    private void translateCameraIsometricRight() {
        //float[] right = Math3DUtils.crossProduct(getxUp(), getyUp(), getzUp(), -getxPos(), -getyPos(), -getzPos());
        float[] right = Math3DUtils.crossProduct(saveUp[0], saveUp[1], saveUp[2], -savePos[0], -savePos[1], -savePos[2]);
        Math3DUtils.normalizeVector(right);
        Math3DUtils.snapToGrid(right);
        rotateWithMatrix(right);
    }

    private void translateCameraIsometricLeft() {
        // float[] left = Math3DUtils.crossProduct(-getxPos(), -getyPos(), -getzPos(), getxUp(), getyUp(), getzUp());
        float[] left = Math3DUtils.crossProduct(-savePos[0], -savePos[1], -savePos[2], saveUp[0], saveUp[1], saveUp[2]);
        Math3DUtils.normalizeVector(left);
        Math3DUtils.snapToGrid(left);
        rotateWithMatrix(left);
    }

    /**
     * @param cross the perpendicular vector to the axis rotational vector
     */
    private void rotateWithMatrix(float[] cross) {
        final float dotProductX = Math3DUtils.dotProduct(cross, Math3DUtils.VECTOR_UNIT_X);
        final float dotProductY = Math3DUtils.dotProduct(cross, Math3DUtils.VECTOR_UNIT_Y);
        final float dotProductZ = Math3DUtils.dotProduct(cross, Math3DUtils.VECTOR_UNIT_Z);

        float[] axis;
        if (Math.round(dotProductX) == 0f) {
            axis = Math3DUtils.VECTOR_UNIT_X;
        } else if (Math.round(dotProductY) == 0f) {
            axis = Math3DUtils.VECTOR_UNIT_Y;
        } else if (Math.round(dotProductZ) == 0f) {
            axis = Math3DUtils.VECTOR_UNIT_Z;
        } else {
            // this should never happen
            Log.w("IsometricCamera", "rotateWithMatrix() coding issue. ignoring action...");
            return;
        }

        final float[] posN = Math3DUtils.normalize2(camera.getPos());
        final float[] cross2 = Math3DUtils.crossProduct(posN, cross);
        final double dot = Math3DUtils.dotProduct(axis, cross2);

        /*Log.v("IsometricCamera", "rotateWithMatrix. dot: " + dot
                + ", axis: " + Arrays.toString(axis)
                + ", cross: " + Arrays.toString(cross)
                + ", angle: " + Math3DUtils.calculateAngleBetween(axis, cross));*/
        final double angle = Math.signum(dot) * Math.PI / 2;

        final float[] rotMatrix = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, angle, axis);

        float[] newPos = new float[4];
        Matrix.multiplyMV(newPos, 0, rotMatrix, 0, camera.getPos(), 0);
        //Math3DUtils.normalize(newPos);
        //Math3DUtils.mult(newPos, UNIT);
        Math3DUtils.snapToGrid(newPos);

        float[] newUp = new float[4];
        Matrix.multiplyMV(newUp, 0, rotMatrix, 0, camera.getUp(), 0);
        Math3DUtils.normalizeVector(newUp);
        Math3DUtils.snapToGrid(newUp);

        saveAndAnimate(newPos[0], newPos[1], newPos[2], newUp[0], newUp[1], newUp[2]);

        //Log.v("IsometricCamera", "Rotating... action: " + delegate.getAnimation());
    }

    private void saveAndAnimate(float xp, float yp, float zp, float xu, float yu, float zu) {
        this.saveAndAnimate(false, xp, yp, zp, xu, yu, zu);
    }

    private void saveAndAnimate(boolean force, float xp, float yp, float zp, float xu, float yu, float zu) {

        synchronized (camera) {
            //if (camera.getAnimation() == null || camera.getAnimation().isFinished() || force) {


                savePos[0] = xp;
                savePos[1] = yp;
                savePos[2] = zp;
                saveUp[0] = xu;
                saveUp[1] = yu;
                saveUp[2] = zu;

        /*delegate.setAnimation(new Object[]{"moveTo", getxPos(), getyPos(), getzPos(), getxUp(), getyUp(), getzUp(),
                savePos[0], savePos[1], savePos[2], saveUp[0], saveUp[1], saveUp[2]});*/

                Object[] args = new Object[]{"moveTo",
                        camera.getxPos(), camera.getyPos(), camera.getzPos(),
                        camera.getxUp(), camera.getyUp(), camera.getzUp(),
                        savePos[0], savePos[1], savePos[2],
                        saveUp[0], saveUp[1], saveUp[2],
                        camera.getxView(), camera.getyView(), camera.getzView(),
                        saveView[0], saveView[1], saveView[2]};
                if (animationController != null && (animation == null || animation.isFinished())) {
                    Log.v("IsometricCamera", "New animation: " + Arrays.toString(args));
                    animation = new CameraAnimation(camera, args);
                    animationController.add(animation);
                }
           // }
        }
    }
}
