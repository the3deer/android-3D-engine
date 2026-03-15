package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.util.bean.BeanInit;

/**
 * Regular standard perspective projection.
 */
public final class PerspectiveProjection implements Projection {

    // projection matrix
    private final float[] matrix = new float[16];

    private float yfov = 60.0f;
    private float aspectRatio = 1920 / 1080f;
    private float znear = Constants.near;
    private float zfar = Constants.far;
    /**
     * This flag to indicate if the perspective is coming from the 3d model (true = yes)
     */
    private boolean custom = false;

    public PerspectiveProjection(){
        Matrix.setIdentityM(matrix, 0);
    }

    public PerspectiveProjection(float yfov, float aspectRatio, float znear, float zfar) {
        this.yfov = (float) Math.toDegrees(yfov);
        this.aspectRatio = aspectRatio;
        this.znear = znear;
        this.zfar = zfar;
        this.custom = true;
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @BeanInit
    public void setUp(){
        refresh();
    }

    @Override
    public void refresh() {

    }

    @Override
    public float[] getMatrix(){
        if (custom) {
            Matrix.perspectiveM(matrix, 0, yfov, aspectRatio, znear, zfar);
        } else {
            // Calculate frustum based on yfov and znear to maintain consistent FOV
            float ratio = aspectRatio;
            float top = (float) Math.tan(Math.toRadians(yfov / 2.0)) * znear;
            float bottom = -top;
            float left = -top * ratio;
            float right = top * ratio;
            Matrix.frustumM(matrix, 0, left, right, bottom, top, znear, zfar);
        }
        return matrix;
    }

    @Override
    public float getNear() {
        return znear;
    }

    @Override
    public void setNear(float near) {
        this.znear = near;
    }

    @Override
    public float getFar() {
        return zfar;
    }

    @Override
    public void setFar(float far) {
        this.zfar = far;
    }

    @Override
    public float getFov() {
        return yfov;
    }

    @Override
    public Projection clone() {
        return new PerspectiveProjection(yfov, aspectRatio, znear, zfar);
    }
}
