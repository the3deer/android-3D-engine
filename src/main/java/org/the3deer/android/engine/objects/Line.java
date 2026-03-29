package org.the3deer.android.engine.objects;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

public final class Line {

    public static Object3D build(float[] line) {
        return new Object3D(IOUtils.createFloatBuffer(line.length).put(line))
                .setDrawMode(GLES20.GL_LINES).setId("Line");
    }
}
