package org.the3deer.android.engine.renderer;

public interface Renderer extends RenderListener {

    default void reset() { }

    default boolean isEnabled() { return false; };

    default void setEnabled(boolean enabled) { };

    default void onDrawFrame() { };

}