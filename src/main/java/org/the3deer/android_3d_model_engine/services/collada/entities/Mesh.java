package org.the3deer.android_3d_model_engine.services.collada.entities;

public class Mesh {

    private String id;
    private String materialId;

    private float[] vertices;
    private float[] normals;
    private int[] indices;

    private float[] textureCoords;
    private float[] colors;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public void setVertices(float[] finalPositions) {
        this.vertices = finalPositions;
    }

    public void setNormals(float[] finalNormals) {
        this.normals = finalNormals;
    }

    public void setTextureCoords(float[] finalTexCoords) {
        this.textureCoords = finalTexCoords;
    }

    public void setColors(float[] finalColors) {
        this.colors = finalColors;
    }

    public void setIndices(int[] vertexJointIndices) {
        this.indices = vertexJointIndices;
    }

    public float[] getVertices() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }

    public float[] getNormals() {
        return normals;
    }

    public float[] getTextureCoords() {
        return textureCoords;
    }

    public float[] getColors() {
        return colors;
    }
}
