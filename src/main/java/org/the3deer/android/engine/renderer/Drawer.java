package org.the3deer.android.engine.renderer;

/**
 * App generic drawer
 */
public interface Drawer extends RenderListener {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Called whenever there is a draw event.
     */
    default void onDrawFrame() {
        onDrawFrame(null);
    }

    /**
     * Called whenever there is a draw event.
     *
     * @param config preferred rendering configuration
     */
    void onDrawFrame(Renderer.Config config);
}
