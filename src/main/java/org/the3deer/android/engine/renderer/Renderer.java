package org.the3deer.android.engine.renderer;

import org.the3deer.android.engine.model.Camera;

public interface Renderer extends RenderListener {

    /**
     * Rendering configuration
     */
    class Config {
        public int viewPortX;
        public int viewPortY;
        public int viewPortWidth;
        public int viewPortHeigth;

        public Camera camera;
    }

    default void reset() { }

    default boolean isEnabled() { return false; };

    default void setEnabled(boolean enabled) { };

    default void onDrawFrame() { };

}