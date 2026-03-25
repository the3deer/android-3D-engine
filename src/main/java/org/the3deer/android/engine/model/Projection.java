package org.the3deer.android.engine.model;

public interface Projection {

    void refresh();

    float[] getMatrix();

    float getNear();

    void setNear(float near);

    float getFar();

    void setFar(float far);

    float getFov();
    
    Projection clone();

    void setAspectRatio(float halfRatio);
}
