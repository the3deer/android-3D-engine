package org.the3deer.android_3d_model_engine.renderer;

/**
 * Called whenever there is a draw event
 */
public interface RenderListener {

    default void onSurfaceChanged(int width, int height){}
    default void onPrepareFrame(){}
}
