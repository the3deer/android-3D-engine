package org.the3deer.android.engine.objects;

import android.opengl.GLES20;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to generate Sphere vertex data.
 */
public class Sphere {

    public static Object3D build() {
        float[] data = generatePositionData(20, 20, 1.0f);
        return new Object3D(IOUtils.createFloatBuffer(data.length).put(data))
                .setDrawMode(GLES20.GL_TRIANGLES);
    }

    public static float[] generatePositionData(int rings, int sectors, float radius) {
        final List<Float> vertices = new ArrayList<>();
        final float R = 1.0f / (float) (rings - 1);
        final float S = 1.0f / (float) (sectors - 1);

        for (int r = 0; r < rings; r++) {
            for (int s = 0; s < sectors; s++) {
                // Modified to sit on Y=0. Original y was sin(-PI/2...PI/2) which is -1 to 1.
                // Shifted and scaled to 0 to 1 range.
                float rawY = (float) Math.sin(-Math.PI / 2 + Math.PI * r * R); // -1 to 1
                float y = (rawY + 1.0f) / 2.0f; // 0 to 1
                
                float x = (float) (Math.cos(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));
                float z = (float) (Math.sin(2 * Math.PI * s * S) * Math.sin(Math.PI * r * R));

                vertices.add(x * radius);
                vertices.add(y * (radius * 2)); // Height is 2 * radius
                vertices.add(z * radius);
            }
        }

        final float[] data = new float[rings * sectors * 6 * 3];
        int i = 0;
        for (int r = 0; r < rings - 1; r++) {
            for (int s = 0; s < sectors - 1; s++) {
                int cur = r * sectors + s;
                int next = r * sectors + (s + 1);
                int nextRing = (r + 1) * sectors + s;
                int nextRingNext = (r + 1) * sectors + (s + 1);

                // Triangle 1
                data[i++] = vertices.get(cur * 3); data[i++] = vertices.get(cur * 3 + 1); data[i++] = vertices.get(cur * 3 + 2);
                data[i++] = vertices.get(nextRing * 3); data[i++] = vertices.get(nextRing * 3 + 1); data[i++] = vertices.get(nextRing * 3 + 2);
                data[i++] = vertices.get(next * 3); data[i++] = vertices.get(next * 3 + 1); data[i++] = vertices.get(next * 3 + 2);

                // Triangle 2
                data[i++] = vertices.get(next * 3); data[i++] = vertices.get(next * 3 + 1); data[i++] = vertices.get(next * 3 + 2);
                data[i++] = vertices.get(nextRing * 3); data[i++] = vertices.get(nextRing * 3 + 1); data[i++] = vertices.get(nextRing * 3 + 2);
                data[i++] = vertices.get(nextRingNext * 3); data[i++] = vertices.get(nextRingNext * 3 + 1); data[i++] = vertices.get(nextRingNext * 3 + 2);
            }
        }
        
        float[] result = new float[i];
        System.arraycopy(data, 0, result, 0, i);
        return result;
    }
}
