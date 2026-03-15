package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;

public final class OrthographicProjection implements Projection {

    // projection matrix
    private final float[] matrix = new float[16];

    private float aspectRatio = 1920 / 1080f;
    private float xmag = 1.0f;
    private float ymag = 1.0f;
    private float znear = Constants.near;
    private float zfar = Constants.far;

    private boolean custom = false;

    public OrthographicProjection(){
        Matrix.setIdentityM(matrix, 0);
    }

    public OrthographicProjection(float xmag, float ymag, float znear, float zfar) {
        this.xmag = xmag;
        this.ymag = ymag;
        this.znear = znear;
        this.zfar = zfar;
        this.custom = true;
    }

    @Override
    public float getNear() {
        return 0;
    }

    @Override
    public void setNear(float near) {

    }

    @Override
    public float getFar() {
        return 0;
    }

    @Override
    public void setFar(float far) {

    }

    @Override
    public float getFov() {
        return 0;
    }

    @Override
    public Projection clone() {
        return new OrthographicProjection();
    }

    @Override
    public void setAspectRatio(float halfRatio) {

    }

    @Override
    public void refresh() {
        // not used - we use dynamic update in #getMatrix()
    }

    @Override
    public float[] getMatrix(){
        if (custom) {
            Matrix.orthoM(matrix, 0, -xmag, xmag, -ymag, ymag, znear, zfar);
        } else {
            Matrix.orthoM(matrix, 0,
                    -aspectRatio,
                    aspectRatio,
                    -Constants.UNIT,
                    Constants.UNIT,
                    Constants.near, Constants.far);
        }
        return matrix;
    }
}
