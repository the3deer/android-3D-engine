package org.the3deer.android.engine.renderer;

import org.the3deer.android.engine.model.Camera;

/**
 * App generic drawer
 */
public interface Drawer extends RenderListener {

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

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Called whenever there is a draw event.
     */
    void onDrawFrame();

    /**
     * Called whenever there is a draw event.
     *
     * @param config preferred rendering configuration
     */
    void onDrawFrame(Config config);
}