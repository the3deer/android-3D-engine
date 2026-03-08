package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.util.bean.BeanInit;

import javax.inject.Inject;

/**
 * Regular standard perspective projection.
 */
public final class PerspectiveProjection implements Projection {

    @Inject
    private Screen screen;

    // projection matrix
    private final float[] matrix = new float[16];

    private float yfov = 60.0f;
    private float aspectRatio = 1.0f;
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

    @BeanInit
    public void setUp(){
        refresh();
    }

    @Override
    public Screen getScreen() {
        return screen;
    }

    @Override
    public void setScreen(Screen screen) {

        // check
        if (screen == null) throw new IllegalArgumentException("screen cannot be null");

        // update
        this.screen = screen;
    }

    @Override
    public void refresh() {

    }

    @Override
    public float[] getMatrix(){
        if (custom) {
            Matrix.perspectiveM(matrix, 0, yfov, aspectRatio, znear, zfar);
        } else {
            Matrix.frustumM(matrix, 0,
                    -screen.getRatio(), screen.getRatio(),
                    -1f, 1f, Constants.near, Constants.far);
        }
        return matrix;
    }
}
