package org.the3deer.android.engine.services.stl;

import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * STL Mesh Data to avoid using intermediate arrays
 *
 * @author andresoviedo
 */
public class STLMeshData {

    private final static Logger logger = Logger.getLogger(STLMeshData.class.getSimpleName());
    private final static float[] WRONG_NORMAL = {0, -1, 0};

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer normalsBuffer;

    public STLMeshData(FloatBuffer vertexBuffer, FloatBuffer normalsBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.normalsBuffer = normalsBuffer;
    }

    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public FloatBuffer getNormalsBuffer() {
        return normalsBuffer;
    }

    public void fixNormals() {
        if (vertexBuffer == null || normalsBuffer == null) return;

        vertexBuffer.rewind();
        normalsBuffer.rewind();

        int counter = 0;
        final float[] v1 = new float[3];
        final float[] v2 = new float[3];
        final float[] v3 = new float[3];
        final float[] n1 = new float[3];

        for (int i = 0; i < vertexBuffer.capacity(); i += 9) {
            vertexBuffer.get(v1);
            vertexBuffer.get(v2);
            vertexBuffer.get(v3);

            int normalPos = normalsBuffer.position();
            normalsBuffer.get(n1);

            if (Math3DUtils.length(n1) < 0.1f) {
                float[] calculatedNormal = Math3DUtils.calculateNormal(v1, v2, v3);
                if (calculatedNormal == null) {
                    calculatedNormal = WRONG_NORMAL;
                } else {
                    try {
                        Math3DUtils.normalizeVector(calculatedNormal);
                    } catch (Exception e) {
                        calculatedNormal = WRONG_NORMAL;
                    }
                }

                normalsBuffer.position(normalPos);
                normalsBuffer.put(calculatedNormal);
                normalsBuffer.put(calculatedNormal);
                normalsBuffer.put(calculatedNormal);
                counter++;
            } else {
                normalsBuffer.position(normalPos + 9);
            }
        }
        vertexBuffer.rewind();
        normalsBuffer.rewind();

        logger.info("Fixed normals: " + counter);
    }

    /**
     * Smooth the mesh by averaging normals of shared vertices.
     * This converts the flat shading (one normal per face) to Gouraud/Phong shading.
     */
    public void smooth() {
        if (vertexBuffer == null || normalsBuffer == null) return;

        logger.info("Smoothing mesh...");

        // Map to group vertex indices by their spatial position
        final Map<VertexKey, List<Integer>> vertexGroups = new HashMap<>(vertexBuffer.capacity() / 3);
        
        vertexBuffer.rewind();
        for (int i = 0; i < vertexBuffer.capacity() / 3; i++) {
            float x = vertexBuffer.get();
            float y = vertexBuffer.get();
            float z = vertexBuffer.get();
            
            VertexKey key = new VertexKey(x, y, z);
            List<Integer> group = vertexGroups.get(key);
            if (group == null) {
                group = new ArrayList<>();
                vertexGroups.put(key, group);
            }
            group.add(i);
        }

        // Calculate average normal for each group
        final float[] avgNormal = new float[3];
        final float[] tempNormal = new float[3];
        
        for (List<Integer> group : vertexGroups.values()) {
            if (group.size() <= 1) continue; // No sharing, no need to smooth

            avgNormal[0] = 0; avgNormal[1] = 0; avgNormal[2] = 0;
            
            // Sum all normals in the group
            for (Integer index : group) {
                normalsBuffer.position(index * 3);
                normalsBuffer.get(tempNormal);
                avgNormal[0] += tempNormal[0];
                avgNormal[1] += tempNormal[1];
                avgNormal[2] += tempNormal[2];
            }
            
            // Normalize the sum
            float length = (float) Math.sqrt(avgNormal[0] * avgNormal[0] + avgNormal[1] * avgNormal[1] + avgNormal[2] * avgNormal[2]);
            if (length > 0) {
                avgNormal[0] /= length;
                avgNormal[1] /= length;
                avgNormal[2] /= length;
            } else {
                avgNormal[0] = WRONG_NORMAL[0];
                avgNormal[1] = WRONG_NORMAL[1];
                avgNormal[2] = WRONG_NORMAL[2];
            }

            // Apply the average normal to all vertices in the group
            for (Integer index : group) {
                normalsBuffer.position(index * 3);
                normalsBuffer.put(avgNormal);
            }
        }

        vertexBuffer.rewind();
        normalsBuffer.rewind();
        logger.info("Smoothing finished.");
    }

    /**
     * Simple key for vertex positions to use in HashMap.
     */
    private static class VertexKey {
        private final float x, y, z;

        VertexKey(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VertexKey vertexKey = (VertexKey) o;
            return Float.compare(vertexKey.x, x) == 0 &&
                    Float.compare(vertexKey.y, y) == 0 &&
                    Float.compare(vertexKey.z, z) == 0;
        }

        @Override
        public int hashCode() {
            int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
            result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
            result = 31 * result + (z != +0.0f ? Float.floatToIntBits(z) : 0);
            return result;
        }
    }
}
