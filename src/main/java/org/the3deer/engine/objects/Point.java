package org.the3deer.engine.objects;

import android.opengl.GLES20;

import org.the3deer.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

public final class Point {

    public static Object3D build(float[] location) {
        float[] point = new float[]{location[0], location[1], location[2]};
        return new Object3D(IOUtils.createFloatBuffer(point.length).put(point))
                .setDrawMode(GLES20.GL_POINTS).setId("Point");
    }
}
