package org.the3deer.android.engine.collision;

import org.the3deer.android.engine.model.Object3D;

import java.util.EventObject;

public class CollisionEvent extends EventObject {

    private final Object3D object;
    private final float x;
    private final float y;
    private final float[] point;

    public CollisionEvent(Object source, Object3D object, float x, float y, float[] point) {
        super(source);
        this.object = object;
        this.x = x;
        this.y = y;
        this.point = point;
    }

    public Object3D getObject() {
        return object;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float[] getPoint() {
        return point;
    }
}
