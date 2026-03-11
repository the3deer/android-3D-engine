package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import androidx.annotation.NonNull;

import org.the3deer.util.math.Math3DUtils;

/**
 * @author andresoviedo
 */
public final class BoundingBox {

    private final String id;
    private final float[] min;
    private final float[] max;

    // dynamic bounding box
    private final float[] modelMatrix;
    private final float[] actualMin;
    private final float[] actualMax;

    public BoundingBox(String id, float xMin, float xMax, float yMin, float yMax, float zMin, float zMax) {
        this(id, new float[]{xMin, yMin, zMin, 1}, new float[]{xMax, yMax, zMax, 1}, Math3DUtils.IDENTITY_MATRIX);
    }

    public static BoundingBox create(String id, Dimensions d, float[] modelMatrix) {
        return new BoundingBox(id, d.getMin(), d.getMax(), modelMatrix);
    }

    private BoundingBox(String id, float min[], float max[], float[] modelMatrix) {
        this.id = id;
        this.min = new float[]{min[0],min[1],min[2],1};
        this.max = new float[]{max[0],max[1],max[2],1};
        this.modelMatrix = modelMatrix;
        this.actualMin = new float[4];
        this.actualMax = new float[4];
        refresh();
    }

    private void refresh() {
        if (Math3DUtils.isIdentity(modelMatrix)) {
            System.arraycopy(min, 0, actualMin, 0, 4);
            System.arraycopy(max, 0, actualMax, 0, 4);
            return;
        }

        // To correctly find the world-space AABB of a transformed box,
        // we must transform all 8 corners and find the new min/max.
        float[][] localCorners = new float[][]{
                {min[0], min[1], min[2], 1},
                {min[0], min[1], max[2], 1},
                {min[0], max[1], min[2], 1},
                {min[0], max[1], max[2], 1},
                {max[0], min[1], min[2], 1},
                {max[0], min[1], max[2], 1},
                {max[0], max[1], min[2], 1},
                {max[0], max[1], max[2], 1}
        };

        float[] worldCorner = new float[4];
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (float[] corner : localCorners) {
            Matrix.multiplyMV(worldCorner, 0, modelMatrix, 0, corner, 0);
            minX = Math.min(minX, worldCorner[0]);
            minY = Math.min(minY, worldCorner[1]);
            minZ = Math.min(minZ, worldCorner[2]);
            maxX = Math.max(maxX, worldCorner[0]);
            maxY = Math.max(maxY, worldCorner[1]);
            maxZ = Math.max(maxZ, worldCorner[2]);
        }

        actualMin[0] = minX; actualMin[1] = minY; actualMin[2] = minZ; actualMin[3] = 1;
        actualMax[0] = maxX; actualMax[1] = maxY; actualMax[2] = maxZ; actualMax[3] = 1;
    }

    public float[] getMin() {
        return actualMin;
    }

    public float[] getMax() {
        return actualMax;
    }

    public float getxMin() {
        return actualMin[0];
    }

    public float getxMax() {
        return actualMax[0];
    }

    public float getyMin() {
        return actualMin[1];
    }

    public float getyMax() {
        return actualMax[1];
    }

    public float getzMin() {
        return actualMin[2];
    }

    public float getzMax() {
        return actualMax[2];
    }

    public float[][] getCorners() {
        return new float[][]{
                {actualMin[0], actualMin[1], actualMin[2]},
                {actualMin[0], actualMin[1], actualMax[2]},
                {actualMin[0], actualMax[1], actualMin[2]},
                {actualMin[0], actualMax[1], actualMax[2]},
                {actualMax[0], actualMin[1], actualMin[2]},
                {actualMax[0], actualMin[1], actualMax[2]},
                {actualMax[0], actualMax[1], actualMin[2]},
                {actualMax[0], actualMax[1], actualMax[2]}
        };
    }

    public boolean insideBounds(float x, float y, float z) {
        return !outOfBound(x, y, z);
    }

    public boolean outOfBound(float x, float y, float z) {
        return x > getxMax() || x < getxMin() || y < getyMin() || y > getyMax() || z < getzMin() || z > getzMax();
    }

    @NonNull
    @Override
    public String toString() {
        return "BoundingBox{" +
                "id='" + id + '\'' +
                ", xMin=" + getxMin() +
                ", xMax=" + getxMax() +
                ", yMin=" + getyMin() +
                ", yMax=" + getyMax() +
                ", zMin=" + getzMin() +
                ", zMax=" + getzMax() +
                '}';
    }
}
