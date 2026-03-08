package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.util.bean.BeanInit;

import javax.inject.Inject;

public final class OrthographicProjection implements Projection {

    // default camera - the inject only works for managed beans
    @Inject
    private Screen screen = new Screen(640, 480);

    // projection matrix
    private final float[] matrix = new float[16];

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
    public Screen getScreen() {
        return this.screen;
    }

    @Override
    public void setScreen(Screen screen) {
        if (screen == null) throw new IllegalArgumentException("Screen cannot be null");
        this.screen = screen;
        refresh();
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
                    -Constants.UNIT * screen.getRatio(),
                    Constants.UNIT * screen.getRatio(),
                    -Constants.UNIT,
                    Constants.UNIT,
                    Constants.near, Constants.far);
        }
        return matrix;
    }
}
