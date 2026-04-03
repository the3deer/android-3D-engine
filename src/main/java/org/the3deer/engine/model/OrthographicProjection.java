package org.the3deer.engine.model;

import org.the3deer.util.math.Matrix;

public final class OrthographicProjection implements Projection {

    // projection matrix
    private final float[] matrix = new float[16];

    private float aspectRatio = 1.0f;
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
        return 0;
    }

    @Override
    public Projection clone() {
        return new OrthographicProjection(xmag, ymag, znear, zfar);
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
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
