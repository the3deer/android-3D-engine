package org.the3deer.android_3d_model_engine.services.collada.entities;

public class VertexWeights {

    // You can add more fields here if needed later
    private final int[] jointIndices;
    private final float[] weights;

    public VertexWeights(int[] jointIndices, float[] weights) {
        this.jointIndices = jointIndices;
        this.weights = weights;
    }

    public float[] getWeights() {
        return weights;
    }

    public int[] getJointIndices() {
        return jointIndices;
    }
}
