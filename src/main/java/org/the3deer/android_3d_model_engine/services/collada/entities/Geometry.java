package org.the3deer.android_3d_model_engine.services.collada.entities;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

// A simple container for the data parsed from a <geometry> tag in a DAE file.
public class Geometry {
    private final String id;
    private FloatBuffer positions;
    private FloatBuffer colors;
    private FloatBuffer normals;
    private FloatBuffer texCoords;
    private IntBuffer indices;
    private String materialId;
    private int[] vertexJointIndices; // This maps an unrolled vertex back to its original vertex index for skinning

    public Geometry(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public FloatBuffer getPositions() {
        return positions;
    }

    public void setPositions(FloatBuffer positions) {
        this.positions = positions;
    }

    public FloatBuffer getColors() {
        return colors;
    }

    public void setColors(FloatBuffer colors) {
        this.colors = colors;
    }

    public FloatBuffer getNormals() {
        return normals;
    }

    public void setNormals(FloatBuffer normals) {
        this.normals = normals;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public FloatBuffer getTexCoords() {
        return texCoords;
    }

    public void setTexCoords(FloatBuffer texCoords) {
        this.texCoords = texCoords;
    }

    public IntBuffer getIndices() {
        return indices;
    }

    public void setIndices(IntBuffer indices) {
        this.indices = indices;
    }

    public void setVertexJointIndices(int[] vertexJointIndices) {
        this.vertexJointIndices = vertexJointIndices;
    }

    public int[] getVertexJointIndices() {
        return vertexJointIndices;
    }
}