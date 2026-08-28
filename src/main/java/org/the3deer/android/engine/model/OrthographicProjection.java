package org.the3deer.android.engine.model;

import org.the3deer.android.util.Matrix;

public final class OrthographicProjection implements Projection {

    // projection matrix
    private final float[] matrix = new float[16];

    private float aspectRatio = 1.0f;
    private float xmag = Constants.UNIT;
    private float ymag = Constants.UNIT;
    private float znear = Constants.near;
    private float zfar = Constants.far;

    public OrthographicProjection(){
        Matrix.setIdentityM(matrix, 0);
    }

    public OrthographicProjection(float xmag, float ymag, float znear, float zfar) {
        this.xmag = xmag;
        this.ymag = ymag;
        this.znear = znear;
        this.zfar = zfar;
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
    public float getAspectRatio() {
        return aspectRatio;
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public float getXmag() {
        return xmag;
    }

    public void setXmag(float xmag) {
        this.xmag = xmag;
    }

    public float getYmag() {
        return ymag;
    }

    public void setYmag(float ymag) {
        this.ymag = ymag;
    }

    @Override
    public void refresh() {
        // not used - we use dynamic update in #getMatrix()
    }

    @Override
    public float[] getMatrix(){
        Matrix.orthoM(matrix, 0,
                -xmag * aspectRatio,
                xmag * aspectRatio,
                -ymag,
                ymag,
                znear, zfar);
        return matrix;
    }
}
