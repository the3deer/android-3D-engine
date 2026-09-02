package org.the3deer.android.engine.objects;

import android.opengl.GLES20;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

/**
 * Utility to generate Plane vertex data.
 */
public class Plane {

    public static Object3D build() {
        float[] data = generatePositionData();
        Object3D obj = new Object3D(IOUtils.createFloatBuffer(data.length).put(data));
        float[] normals = generateNormalData();
        obj.setVertexNormalsArrayBuffer(IOUtils.createFloatBuffer(normals.length).put(normals));
        obj.setDrawMode(GLES20.GL_TRIANGLES);
        return obj;
    }

    public static Object3D buildTextured() {
        Object3D obj = build();
        float[] texCoords = generateTextureCoordinateData();
        obj.setTextureCoordsArrayBuffer(IOUtils.createFloatBuffer(texCoords.length).put(texCoords));
        return obj;
    }

    public static float[] generatePositionData() {
        return new float[]{
                -0.5f, 0.0f, -0.5f,
                -0.5f, 0.0f, 0.5f,
                0.5f, 0.0f, 0.5f,
                -0.5f, 0.0f, -0.5f,
                0.5f, 0.0f, 0.5f,
                0.5f, 0.0f, -0.5f
        };
    }

    public static float[] generateNormalData() {
        return new float[]{
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f
        };
    }

    public static float[] generateTextureCoordinateData() {
        return new float[]{
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 0.0f,
                1.0f, 1.0f
        };
    }
}
