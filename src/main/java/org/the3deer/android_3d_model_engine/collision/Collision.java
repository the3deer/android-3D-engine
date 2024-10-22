package org.the3deer.android_3d_model_engine.collision;

import org.the3deer.android_3d_model_engine.model.Object3DData;

public class Collision {

    private final Object3DData object;
    private final float[] point;
    private final float dx;
    private final float dy;

    public Collision(Object3DData object, float[] point, float dx, float dy) {
        this.object = object;
        this.point = point;
        this.dx = dx;
        this.dy = dy;
    }

    public Object getObject() {
        return object;
    }

    public float[] getPoint() {
        return point;
    }

    public float getDx() {
        return dx;
    }

    public float getDy() {
        return dy;
    }


}
