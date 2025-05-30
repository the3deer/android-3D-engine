package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Screen;

import javax.inject.Inject;

/**
 * Regular standard perspective projection.
 */
public final class PerspectiveProjection implements Projection {

    @Inject
    private Screen screen;

    // projection matrix
    private final float[] matrix = new float[16];

    public PerspectiveProjection(){
        Matrix.setIdentityM(matrix, 0);
    }

    public void setUp(){
        refresh();
    }

    @Override
    public void refresh(){

        if (screen == null) return;

        Matrix.frustumM(matrix, 0,
                -screen.getRatio(), screen.getRatio(),
                -1f, 1f, Constants.near, Constants.far);
    }

    @Override
    public float[] getMatrix(){
        return matrix;
    }
}
