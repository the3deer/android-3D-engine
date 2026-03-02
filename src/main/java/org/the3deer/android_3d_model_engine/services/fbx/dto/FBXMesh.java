package org.the3deer.android_3d_model_engine.services.fbx.dto;

import java.nio.Buffer;

public class FBXMesh {

    private long nativeHandler;

    // meshes
    private Buffer verticesBuffer;
    private Buffer normalsBuffer;
    private Buffer indicesBuffer;
    private Buffer colorsBuffer;
    private Buffer texCoordsBuffer;
    private String texturePath;

    public void setVerticesBuffer(Buffer verticesBuffer) {
        this.verticesBuffer = verticesBuffer;
    }

    public Buffer getVerticesBuffer() {
        return verticesBuffer;
    }

    public void setNormalsBuffer(Buffer normalsBuffer) {
        this.normalsBuffer = normalsBuffer;
    }

    public Buffer getNormalsBuffer() {
        return normalsBuffer;
    }

    public void setIndicesBuffer(Buffer indicesBuffer) {
        this.indicesBuffer = indicesBuffer;
    }

    public Buffer getIndicesBuffer() {
        return indicesBuffer;
    }

    public void setColorsBuffer(Buffer colorsBuffer) {
        this.colorsBuffer = colorsBuffer;
    }

    public Buffer getColorsBuffer() {
        return colorsBuffer;
    }

    public Buffer getTexCoordsBuffer() {
        return texCoordsBuffer;
    }

    public void setTexCoordsBuffer(Buffer texCoordsBuffer) {
        this.texCoordsBuffer = texCoordsBuffer;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }
}
