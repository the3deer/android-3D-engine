package org.the3deer.android_3d_model_engine.model.impl;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.util.bean.BeanInit;

import javax.inject.Inject;

public final class OrthographicProjection implements Projection {

    @Inject
    private Screen screen;

    // projection matrix
    private final float[] matrix = new float[16];

    public OrthographicProjection(){
        Matrix.setIdentityM(matrix, 0);
    }

    @BeanInit
    public void setUp(){
        refresh();
    }

    @Override
    public void refresh(){

        if (screen == null) return;

        Matrix.orthoM(matrix, 0,
                -Constants.UNIT * screen.getRatio(),
                Constants.UNIT * screen.getRatio(),
                -Constants.UNIT,
                Constants.UNIT,
                Constants.near, Constants.far);
    }

    @Override
    public float[] getMatrix(){
        return matrix;
    }
}
