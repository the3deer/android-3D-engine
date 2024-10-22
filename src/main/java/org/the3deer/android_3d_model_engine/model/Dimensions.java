package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import java.text.DecimalFormat;
import java.util.Arrays;

public class Dimensions {

    // edge coordinates
    private float leftPt = Float.MAX_VALUE, rightPt = -Float.MAX_VALUE; // on x-axis
    private float topPt = -Float.MAX_VALUE, bottomPt = Float.MAX_VALUE; // on y-axis
    private float farPt = Float.MAX_VALUE, nearPt = -Float.MAX_VALUE; // on z-axis

    // min max center
    private final float[] center = new float[]{0, 0, 0, 1};
    private float[] min = new float[]{0, 0, 0, 1};
    private float[] max = new float[]{0, 0, 0, 1};
    private final float[] middle = new float[]{0, 0, 0, 1};

    // for reporting
    private static final DecimalFormat df = new DecimalFormat("0.##"); // 2 dp

    public Dimensions() {
        //();
    }

    public Dimensions(Dimensions original, float[] matrix) {

        float[] newMin = new float[4];
        float[] newMax = new float[4];
        Matrix.multiplyMV(newMin, 0, matrix, 0, original.getMin(), 0);
        Matrix.multiplyMV(newMax, 0, matrix, 0, original.getMax(), 0);
        float[][] points = new float[2][4];
        points[0] = new float[]{newMin[0], newMin[1], newMin[2], newMin[3]};
        points[1] = new float[]{newMax[0], newMax[1], newMax[2], newMax[3]};
        for (int i = 0; i < points.length; i++) {
            update(points[i][0], points[i][1], points[i][2]);
        }
    }

    public Dimensions(float[] min, float[] max){
        this.min = min;
        this.max = max;
        this.leftPt = min[0];
        this.rightPt = max[0];
        this.topPt = max[1];
        this.bottomPt = min[1];
        this.nearPt = max[2];
        this.farPt = min[2];
        refresh();;
    }

    public Dimensions(float leftPt, float rightPt, float topPt, float bottomPt, float nearPt, float farPt) {
        this.leftPt = leftPt;
        this.rightPt = rightPt;
        this.topPt = topPt;
        this.bottomPt = bottomPt;
        this.nearPt = nearPt;
        this.farPt = farPt;
        refresh();
    }

    public float[] getMin() {
        return min;
    }

    public float[] getMax() {
        return max;
    }

    public void update(float x, float y, float z) {
        if (x > rightPt)
            rightPt = x;
        if (x < leftPt)
            leftPt = x;

        if (y > topPt)
            topPt = y;
        if (y < bottomPt)
            bottomPt = y;

        if (z > nearPt)
            nearPt = z;
        if (z < farPt)
            farPt = z;

        refresh();
    }

    private void refresh() {
        this.min[0] = leftPt;
        this.min[1] = bottomPt;
        this.min[2] = farPt;

        this.max[0] = rightPt;
        this.max[1] = topPt;
        this.max[2] = nearPt;

        this.center[0] = (this.max[0] + this.min[0]) / 2.0f;
        this.center[1] = (this.max[1] + this.min[1]) / 2.0f;
        this.center[2] = (this.max[2] + this.min[2]) / 2.0f;

        this.middle[0] = this.center[0] - min[0];
        this.middle[1] = this.center[1] - min[1];
        this.middle[2] = this.center[2] - min[2];
    }

    // ------------- use the edge coordinates ----------------------------

    public float getWidth() {
        return Math.abs(this.max[0] - this.min[0]);
    }

    public float getHeight() {
        return Math.abs(this.max[1] - this.min[1]);
    }

    public float getDepth() {
        return Math.abs(this.max[2] - this.min[2]);
    }

    public float getLargest() {
        float height = getHeight();
        float depth = getDepth();

        float largest = getWidth();
        if (height > largest)
            largest = height;
        if (depth > largest)
            largest = depth;

        return largest;
    }

    /**
     * @return the center of the bounding box
     */
    public float[] getCenter() {
        return center;
    }

    /**
     * This is the same as center()-min()
     * @return the distance vector from min() to center()
     */
    public float[] getMiddle() {
        return middle;
    }

    public float[] getCornerLeftTopNearVector() {
        return new float[]{leftPt, topPt, nearPt, 1};
    }

    public float[] getCornerRightBottomFar() {
        return new float[]{rightPt, bottomPt, farPt, 1};
    }

    public Dimensions translate(float[] diff) {
        return new Dimensions(leftPt + diff[0], rightPt + diff[0],
                topPt + diff[1], bottomPt + diff[1],
                nearPt + diff[2], farPt + diff[2]);
    }

    public Dimensions scale(float scale) {
        return new Dimensions(leftPt * scale, rightPt * scale,
                topPt * scale, bottomPt * scale,
                nearPt * scale, farPt * scale);
    }

    @Override
    public String toString() {
        return "Dimensions{" +
                "min=" + Arrays.toString(min) +
                ", max=" + Arrays.toString(max) +
                ", center=" + Arrays.toString(center) +
                ", width=" + getWidth() +
                ", height=" + getHeight() +
                ", depth=" + getDepth() +
                '}';
    }

    public float getRelationTo(Dimensions other) {
        return this.getLargest() / other.getLargest();
    }


}
