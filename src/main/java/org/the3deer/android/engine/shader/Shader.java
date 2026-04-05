package org.the3deer.android.engine.shader;

import org.the3deer.android.engine.model.Object3D;

/**
 * This interface connects to the OpenGL underlying shader implementations.
 * It is directly linked to 1 Program
 */
public interface Shader {

    int getOpenGLVersion();

    String getName();

	void draw(Object3D obj, float[] pMatrix, float[] vMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawType, int drawSize);

    int getProgram();

    void useProgram();

    void setAutoUseProgram(boolean autoUseProgram);

    void reset();

    void setLightingEnabled(boolean on);

    void setTexturesEnabled(boolean on);

    /**
     * Set the current time in seconds for dynamic effects.
     * @param time time in seconds
     */
    void setTime(float time);
}