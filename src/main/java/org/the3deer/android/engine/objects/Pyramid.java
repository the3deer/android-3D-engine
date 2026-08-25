package org.the3deer.android.engine.objects;

import android.opengl.GLES20;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

/**
 * Utility to generate Pyramid vertex data.
 */
public class Pyramid {

    public static final float[] pyramidPositionData = {
            // Front face
            0.0f, 0.5f, 0.0f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f,
            0.0f, 0.5f, 0.0f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f,
            0.0f, 0.5f, 0.0f, 0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f,
            0.0f, 0.5f, 0.0f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f,
            -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f
    };

    public static Object3D build() {
        return new Object3D(IOUtils.createFloatBuffer(pyramidPositionData.length).put(pyramidPositionData))
                .setDrawMode(GLES20.GL_TRIANGLES);
    }
}
