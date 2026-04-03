package org.the3deer.engine.collision;

import org.the3deer.engine.model.Object3D;

public class Collision {

    private final Object3D object;
    private final float[] point;
    private final float dx;
    private final float dy;

    public Collision(Object3D object, float[] point, float dx, float dy) {
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
