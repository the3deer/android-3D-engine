package org.the3deer.android.engine.renderer;

public interface Renderer extends RenderListener {

    default boolean isEnabled() { return false; };

    default void setEnabled(boolean enabled) { };

    default void onDrawFrame() { };

}