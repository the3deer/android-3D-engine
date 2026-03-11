package org.the3deer.android_3d_model_engine.model;

public interface Projection {

    void refresh();

    float[] getMatrix();

    Screen getScreen();

    void setScreen(Screen screen);

    float getNear();

    void setNear(float near);

    float getFar();

    void setFar(float far);

    float getFov();
}
