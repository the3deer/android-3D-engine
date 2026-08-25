package org.the3deer.android.engine.objects;

import android.opengl.GLES20;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to generate Cylinder vertex data.
 */
public class Cylinder {

    public static Object3D build() {
        float[] data = generatePositionData(20, 1.0f, 1.0f);
        return new Object3D(IOUtils.createFloatBuffer(data.length).put(data))
                .setDrawMode(GLES20.GL_TRIANGLES);
    }

    public static float[] generatePositionData(int sectors, float radius, float height) {
        final List<Float> vertices = new ArrayList<>();
        
        // Generate vertices for top and bottom circles
        for (int s = 0; s < sectors; s++) {
            float angle = (float) (2 * Math.PI * s / sectors);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            
            // Top circle
            vertices.add(x); vertices.add(height / 2); vertices.add(z);
            // Bottom circle
            vertices.add(x); vertices.add(-height / 2); vertices.add(z);
        }
        
        // Center points for caps
        int topCenterIdx = vertices.size() / 3;
        vertices.add(0f); vertices.add(height / 2); vertices.add(0f);
        int bottomCenterIdx = vertices.size() / 3;
        vertices.add(0f); vertices.add(-height / 2); vertices.add(0f);

        final float[] data = new float[sectors * 12 * 3]; // 2 triangles for side + 1 for top cap + 1 for bottom cap
        int i = 0;
        for (int s = 0; s < sectors; s++) {
            int next = (s + 1) % sectors;
            
            int topS = s * 2;
            int botS = s * 2 + 1;
            int topNext = next * 2;
            int botNext = next * 2 + 1;

            // Side - Triangle 1
            data[i++] = vertices.get(topS * 3); data[i++] = vertices.get(topS * 3 + 1); data[i++] = vertices.get(topS * 3 + 2);
            data[i++] = vertices.get(botS * 3); data[i++] = vertices.get(botS * 3 + 1); data[i++] = vertices.get(botS * 3 + 2);
            data[i++] = vertices.get(topNext * 3); data[i++] = vertices.get(topNext * 3 + 1); data[i++] = vertices.get(topNext * 3 + 2);

            // Side - Triangle 2
            data[i++] = vertices.get(topNext * 3); data[i++] = vertices.get(topNext * 3 + 1); data[i++] = vertices.get(topNext * 3 + 2);
            data[i++] = vertices.get(botS * 3); data[i++] = vertices.get(botS * 3 + 1); data[i++] = vertices.get(botS * 3 + 2);
            data[i++] = vertices.get(botNext * 3); data[i++] = vertices.get(botNext * 3 + 1); data[i++] = vertices.get(botNext * 3 + 2);

            // Top cap
            data[i++] = vertices.get(topCenterIdx * 3); data[i++] = vertices.get(topCenterIdx * 3 + 1); data[i++] = vertices.get(topCenterIdx * 3 + 2);
            data[i++] = vertices.get(topNext * 3); data[i++] = vertices.get(topNext * 3 + 1); data[i++] = vertices.get(topNext * 3 + 2);
            data[i++] = vertices.get(topS * 3); data[i++] = vertices.get(topS * 3 + 1); data[i++] = vertices.get(topS * 3 + 2);

            // Bottom cap
            data[i++] = vertices.get(bottomCenterIdx * 3); data[i++] = vertices.get(bottomCenterIdx * 3 + 1); data[i++] = vertices.get(bottomCenterIdx * 3 + 2);
            data[i++] = vertices.get(botS * 3); data[i++] = vertices.get(botS * 3 + 1); data[i++] = vertices.get(botS * 3 + 2);
            data[i++] = vertices.get(botNext * 3); data[i++] = vertices.get(botNext * 3 + 1); data[i++] = vertices.get(botNext * 3 + 2);
        }
        
        return data;
    }
}
