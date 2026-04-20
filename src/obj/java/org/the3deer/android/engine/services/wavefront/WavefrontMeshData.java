package org.the3deer.android.engine.services.wavefront;

import org.the3deer.android.engine.model.Element;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WavefrontMeshData {

    private static final Logger logger = Logger.getLogger(WavefrontMeshData.class.getSimpleName());
    private static final float[] WRONG_NORMAL = {0, -1, 0};

    private final String id;
    private final String name;
    private final String materialFile;

    // Temporary storage for parsing
    private FloatBuffer vertices;
    private FloatBuffer normals;
    private FloatBuffer textures;
    private IntBuffer vertexAttributes;
    private List<Element> elements;
    private Map<String, List<Integer>> smoothingGroups;

    // Final buffers
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalsBuffer;
    private FloatBuffer textureBuffer;

    public static class Builder {
        private String id;
        private String name;
        private FloatBuffer vertices;
        private FloatBuffer normals;
        private FloatBuffer textures;
        private IntBuffer vertexAttributes;
        private List<Element> elements = new ArrayList<>();
        private String materialFile;
        private Map<String, List<Integer>> smoothingGroups;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder vertices(FloatBuffer vertices) { this.vertices = vertices; return this; }
        public Builder normals(FloatBuffer normals) { this.normals = normals; return this; }
        public Builder textures(FloatBuffer textures) { this.textures = textures; return this; }
        public Builder vertexAttributes(IntBuffer vertexAttributes) { this.vertexAttributes = vertexAttributes; return this; }
        public Builder addElement(Element element) { this.elements.add(element); return this; }
        public Builder materialFile(String materialFile) { this.materialFile = materialFile; return this; }
        public Builder smoothingGroups(Map<String, List<Integer>> smoothingGroups) { this.smoothingGroups = smoothingGroups; return this; }

        public WavefrontMeshData build() {
            return new WavefrontMeshData(id, name, vertices, normals, textures, vertexAttributes, elements, materialFile, smoothingGroups);
        }
    }

    private WavefrontMeshData(String id, String name, FloatBuffer vertices, FloatBuffer normals, FloatBuffer textures,
                             IntBuffer vertexAttributes, List<Element> elements, String materialFile,
                             Map<String, List<Integer>> smoothingGroups) {
        this.id = id;
        this.name = name;
        this.vertices = vertices;
        this.normals = normals;
        this.textures = textures;
        this.vertexAttributes = vertexAttributes;
        this.elements = elements;
        this.materialFile = materialFile;
        this.smoothingGroups = smoothingGroups;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getMaterialFile() { return materialFile; }
    public List<Element> getElements() { return elements; }

    public void fixNormals() {
        if (this.normals == null || this.normals.capacity() == 0) {
            generateNormals();
        }
    }

    private void generateNormals() {
        if (vertexAttributes == null || vertices == null) return;
        
        int count = vertexAttributes.limit() / 3;
        this.normals = IOUtils.createFloatBuffer(count * 3);
        
        float[] v1 = new float[3];
        float[] v2 = new float[3];
        float[] v3 = new float[3];

        for (Element element : elements) {
            IntBuffer indices = (IntBuffer) element.getIndexBuffer();
            for (int i = 0; i < indices.limit(); i += 3) {
                final int idx1 = indices.get(i);
                final int idx2 = indices.get(i + 1);
                final int idx3 = indices.get(i + 2);

                vertices.position(vertexAttributes.get(idx1 * 3) * 3);
                vertices.get(v1);

                vertices.position(vertexAttributes.get(idx2 * 3) * 3);
                vertices.get(v2);

                vertices.position(vertexAttributes.get(idx3 * 3) * 3);
                vertices.get(v3);

                float[] normal = WRONG_NORMAL;
                if (!Arrays.equals(v1, v2) && !Arrays.equals(v2, v3) && !Arrays.equals(v1, v3)) {
                    normal = Math3DUtils.calculateNormal(v1, v2, v3);
                    try {
                        Math3DUtils.normalizeVector(normal);
                    } catch (Exception e) {
                        normal = WRONG_NORMAL;
                    }
                }

                // Map normal to the corner
                vertexAttributes.put(idx1 * 3 + 2, idx1);
                vertexAttributes.put(idx2 * 3 + 2, idx2);
                vertexAttributes.put(idx3 * 3 + 2, idx3);
                
                this.normals.position(idx1 * 3);
                this.normals.put(normal);
                this.normals.position(idx2 * 3);
                this.normals.put(normal);
                this.normals.position(idx3 * 3);
                this.normals.put(normal);
            }
        }
        if (this.normals != null) this.normals.flip();
    }

    public void validate() {
        // Basic validation if needed
    }

    public FloatBuffer getVertexBuffer() {
        if (vertexBuffer == null && vertexAttributes != null && vertices != null) {
            int count = vertexAttributes.limit() / 3;
            vertexBuffer = IOUtils.createFloatBuffer(count * 3);
            float[] v = new float[3];
            for (int i = 0; i < count; i++) {
                int vertIdx = vertexAttributes.get(i * 3);
                if (vertIdx >= 0 && (vertIdx * 3 + 2) < vertices.limit()) {
                    vertices.position(vertIdx * 3);
                    vertices.get(v);
                    vertexBuffer.put(v);
                } else {
                    vertexBuffer.put(0).put(0).put(0);
                }
            }
            vertexBuffer.flip();
        }
        return vertexBuffer;
    }

    public FloatBuffer getNormalsBuffer() {
        if (normalsBuffer == null && vertexAttributes != null && normals != null) {
            int count = vertexAttributes.limit() / 3;
            normalsBuffer = IOUtils.createFloatBuffer(count * 3);
            float[] n = new float[3];
            for (int i = 0; i < count; i++) {
                int idx = vertexAttributes.get(i * 3 + 2);
                if (idx >= 0 && (idx * 3 + 2) < normals.limit()) {
                    normals.position(idx * 3);
                    normals.get(n);
                    normalsBuffer.put(n);
                } else {
                    normalsBuffer.put(WRONG_NORMAL);
                }
            }
            normalsBuffer.flip();
        }
        return normalsBuffer;
    }

    public FloatBuffer getTextureBuffer() {
        if (textureBuffer == null && vertexAttributes != null && textures != null) {
            int count = vertexAttributes.limit() / 3;
            textureBuffer = IOUtils.createFloatBuffer(count * 2);
            float[] t = new float[2];
            for (int i = 0; i < count; i++) {
                int idx = vertexAttributes.get(i * 3 + 1);
                if (idx >= 0 && (idx * 2 + 1) < textures.limit()) {
                    textures.position(idx * 2);
                    textures.get(t);
                    textureBuffer.put(t[0]);
                    textureBuffer.put(1 - t[1]);
                } else {
                    textureBuffer.put(0).put(0);
                }
            }
            textureBuffer.flip();
        }
        return textureBuffer;
    }

    public void clearTemporaryData() {
        this.vertices = null;
        this.normals = null;
        this.textures = null;
        this.vertexAttributes = null;
        this.smoothingGroups = null;
    }
}
