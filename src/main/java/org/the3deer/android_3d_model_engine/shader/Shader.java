package org.the3deer.android_3d_model_engine.shader;

import org.the3deer.android_3d_model_engine.model.Object3DData;

public interface Shader {

	void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawType, int drawSize);

    int getProgram();

    void useProgram();

    void setAutoUseProgram(boolean autoUseProgram);
}