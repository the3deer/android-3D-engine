package org.the3deer.android_3d_model_engine.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import org.the3deer.android_3d_model_engine.model.Constants;

/**
 * This is the actual OpenGL surface.
 * It requires a @{@link android.opengl.GLSurfaceView.Renderer} implementation.
 * It requires a @{@link GLTouchHandler} to listen for OpenGL screen touch events
 */
public class GLSurfaceView extends android.opengl.GLSurfaceView {

    private final static String TAG = GLSurfaceView.class.getSimpleName();

    private GLTouchHandler glTouchHandler;

    private final Renderer glRenderer = new GLRendererImpl();

    /**
     * Construct a new renderer for the specified surface view
     */
    public GLSurfaceView(Context parent, AttributeSet attributeSet) {
        super(parent, attributeSet);

        Log.i(TAG, "Creating OpenGL 3 surface... " + System.identityHashCode(this));

        // Create an OpenGL ES context.
        setEGLContextClientVersion(Constants.DEFAULT_SHADER_VERSION);

        // set renderer
        setRenderer(glRenderer);
    }

    public Renderer getRenderer() {
        return glRenderer;
    }

    @Override
    public void onResume() {
        if (glRenderer != null)
            super.onResume();
    }

    @Override
    public void onPause() {
        if (glRenderer != null)
            super.onPause();
    }

    /**
     * Allows the engine to handle the event from this sub-view
     *
     * @param handler
     */
    public void setTouchEventHandler(GLTouchHandler handler) {
        this.glTouchHandler = handler;
        Log.d(TAG, "Registered touch handler: " + System.identityHashCode(handler));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // send the event to the manager
        if (this.glTouchHandler != null) {
            return glTouchHandler.onSurfaceTouchEvent(event);
        } else {
            Log.e(TAG, "Touch handler not found!");
        }

        return false;
    }
}