package org.the3deer.android_3d_model_engine.renderer;

public interface Renderer extends RenderListener {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    void onDrawFrame();

    //List<? extends Object3DData> getObjects();
    // void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawType, int drawSize);

}