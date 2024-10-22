package org.the3deer.android_3d_model_engine.view;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import org.the3deer.android_3d_model_engine.shader.ShaderFactory;

import javax.inject.Inject;

/**
 * This is the actual OpenGL surface.
 * It requires a @{@link android.opengl.GLSurfaceView.Renderer} implementation.
 * It requires a @{@link GLTouchListener} to listen for OpenGL screen touch events
 */
public class GLSurfaceView extends android.opengl.GLSurfaceView {

    private final static String TAG = GLSurfaceView.class.getSimpleName();

    @Inject
    private GLTouchListener glTouchListener;

    @Inject
    private Renderer renderer;

    @Inject
    private ShaderFactory shaderFactory;

    /**
     * Construct a new renderer for the specified surface view
     */
    public GLSurfaceView(Context parent) {
        super(parent);
        try {

            // Create an OpenGL ES 2.0 context.
            Log.d(TAG, "Creating OpenGL 3 surface...");
            setEGLContextClientVersion(3);

        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            Toast.makeText(parent, e.getMessage(), Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);
        }
    }

    public void setUp() {
        if (this.renderer != null) {
            Log.i(TAG, "Configuring renderer: " + this.renderer.getClass().getName());
            setRenderer(this.renderer);
        } else {
            throw new IllegalStateException("Renderer is null");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.glTouchListener != null) {
            return glTouchListener.onSurfaceTouchEvent(event);
        }
        return false;
    }
}