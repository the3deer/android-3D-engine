// in /.../entities/Skin.java
package org.the3deer.android.engine.services.collada.entities;

import java.util.List;

public class Skin {

    // --- ADD THIS FIELD ---
    private String source;

    private float[] bindShapeMatrix;
    private VertexWeights weights;
    private List<String> jointNames;
    private float[] inverseBindMatrices;

    public Skin() {
        // Default constructor is correct
    }

    // --- ADD THIS GETTER AND SETTER ---
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public float[] getBindShapeMatrix() {
        return bindShapeMatrix;
    }

    public void setBindShapeMatrix(float[] bindShapeMatrix) {
        this.bindShapeMatrix = bindShapeMatrix;
    }

    public VertexWeights getWeights() {
        return weights;
    }

    public void setWeights(VertexWeights weights) {
        this.weights = weights;
    }
    public void setJointNames(List<String> jointNames) {
        this.jointNames = jointNames;
    }

    public List<String> getJointNames() {
        return this.jointNames;
    }

    public float[] getInverseBindMatrices() {
        return inverseBindMatrices;
    }

    public void setInverseBindMatrices(float[] inverseBindMatrices) {
        this.inverseBindMatrices = inverseBindMatrices;
    }
}
