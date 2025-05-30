package org.the3deer.android_3d_model_engine.view;

public interface Renderer extends RenderListener {

    default boolean isEnabled() { return false; };

    default void setEnabled(boolean enabled) { };

    default void onDrawFrame() { };

}