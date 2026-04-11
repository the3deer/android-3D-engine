package org.the3deer.android.engine.services.collada.entities;

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

    public float[] getJointIndicesAsFloats() {
        // check
        if (jointIndices == null) return null;

        // The shader expects a vec4 (floats), so we must convert our integer indices to floats.
        // Let's create a new float array to hold the converted indices.
        float[] jointIndicesAsFloats = new float[jointIndices.length];
        for (int i = 0; i < jointIndices.length; i++) {
            jointIndicesAsFloats[i] = (float) jointIndices[i];
        }
        return jointIndicesAsFloats;
    }
}
