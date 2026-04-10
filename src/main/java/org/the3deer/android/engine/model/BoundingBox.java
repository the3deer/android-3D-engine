package org.the3deer.android.engine.model;

/**
 * @author andresoviedo
 */
public final class BoundingBox extends Dimensions {


    public BoundingBox(String s, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        super(xMin, xMax, yMin, yMax, zMin, zMax);
    }

    public BoundingBox(Dimensions dimensions, float[] modelMatrix) {
        super(dimensions, modelMatrix);
    }

    public static BoundingBox create(String s, Dimensions dimensions, float[] modelMatrix) {
        return new BoundingBox(dimensions, modelMatrix);
    }

    public boolean insideBounds(float x, float y, float z) {
        return !outOfBound(x, y, z);
    }

    public boolean outOfBound(float x, float y, float z) {
        return x > getMax()[0] || x < getMin()[0] || y < getMin()[1] || y > getMax()[1] || z < getMin()[2] || z > getMax()[2];
    }
}
