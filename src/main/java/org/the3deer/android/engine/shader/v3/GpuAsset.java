package org.the3deer.android.engine.shader.v3;

import android.opengl.GLES30;

import java.util.List;

/**
 * Container for OpenGL ES 3.x GPU resources.
 * Supports multi-element objects with per-element index buffers.
 * 
 * @author Gemini AI
 */
public class GpuAsset {

    /**
     * Represents a sub-mesh (element) on the GPU.
     */
    public static class GpuElement {
        private final int eboId;
        private final int count;
        private final int type;

        public GpuElement(int eboId, int count, int type) {
            this.eboId = eboId;
            this.count = count;
            this.type = type;
        }

        public int getEboId() { return eboId; }
        public int getCount() { return count; }
        public int getType() { return type; }
    }

    private final int vaoId;
    private final int[] vboIds;
    private final List<GpuElement> gpuElements;
    private final int vertexCount;
    private final int drawMode;
    private final int jointTextureId;
    
    // Metadata to track what's baked into this asset
    private final boolean hasSkin;
    private final boolean hasNormals;
    private final boolean hasTexCoords;
    private final boolean hasColors;
    private final boolean hasTangents;

    public GpuAsset(int vaoId, int[] vboIds, List<GpuElement> gpuElements, int vertexCount, int drawMode,
                    int jointTextureId, boolean hasSkin, boolean hasNormals, boolean hasTexCoords, 
                    boolean hasColors, boolean hasTangents) {
        this.vaoId = vaoId;
        this.vboIds = vboIds;
        this.gpuElements = gpuElements;
        this.vertexCount = vertexCount;
        this.drawMode = drawMode;
        this.jointTextureId = jointTextureId;
        this.hasSkin = hasSkin;
        this.hasNormals = hasNormals;
        this.hasTexCoords = hasTexCoords;
        this.hasColors = hasColors;
        this.hasTangents = hasTangents;
    }

    public int getJointTextureId() {
        return jointTextureId;
    }

    public int[] getVboIds() {
        return vboIds;
    }

    public void bind() {
        GLES30.glBindVertexArray(vaoId);
    }

    public void unbind() {
        GLES30.glBindVertexArray(0);
    }

    public List<GpuElement> getGpuElements() {
        return gpuElements;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public boolean hasSkin() { return hasSkin; }
    public boolean hasNormals() { return hasNormals; }
    public boolean hasTexCoords() { return hasTexCoords; }
    public boolean hasColors() { return hasColors; }
    public boolean hasTangents() { return hasTangents; }

    /**
     * Deletes all resources from GPU memory.
     */
    public void dispose() {
        GLES30.glDeleteVertexArrays(1, new int[]{vaoId}, 0);
        if (vboIds != null && vboIds.length > 0) {
            GLES30.glDeleteBuffers(vboIds.length, vboIds, 0);
        }
        if (gpuElements != null) {
            int[] ids = new int[1];
            for (GpuElement element : gpuElements) {
                ids[0] = element.eboId;
                GLES30.glDeleteBuffers(1, ids, 0);
            }
        }
        if (jointTextureId != 0) {
            GLES30.glDeleteTextures(1, new int[]{jointTextureId}, 0);
        }
    }
}
