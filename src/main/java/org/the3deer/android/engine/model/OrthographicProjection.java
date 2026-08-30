package org.the3deer.android.engine.model;

import org.the3deer.android.util.Matrix;

import java.util.logging.Logger;

import javax.inject.Inject;

public final class OrthographicProjection implements Projection {

    private static final Logger logger = Logger.getLogger(OrthographicProjection.class.getSimpleName());

    // projection matrix
    private final float[] matrix = new float[16];

    @Inject
    private Screen screen;

    private float aspectRatio = -1;
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
        if (aspectRatio == -1 && screen != null) {
            return screen.getRatio();
        }
        return aspectRatio;
    }

    @Override
    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    @Override
    public void setScreen(Screen screen) {
        this.screen = screen;
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
        float ratio = this.aspectRatio;
        if (ratio == -1 && screen != null){
            ratio = screen.getRatio();
        }
        if (ratio == -1) ratio = 1.0f;

        //logger.info("OrthographicProjection ratio: " + ratio);

        Matrix.orthoM(matrix, 0,
                -xmag * ratio,
                xmag * ratio,
                -ymag,
                ymag,
                znear, zfar);

        return matrix;
    }
}
