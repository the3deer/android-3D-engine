package org.the3deer.android_3d_model_engine.renderer;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.view.RenderListener;

/**
 * App generic drawer
 */
public interface Drawer extends RenderListener {

    /**
     * Rendering configuration
     */
    class Config {
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