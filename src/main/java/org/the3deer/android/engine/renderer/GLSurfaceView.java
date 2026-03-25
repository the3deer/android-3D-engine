package org.the3deer.android.engine.renderer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import javax.inject.Inject;

/**
 * This is the actual OpenGL surface.
 * It requires a @{@link Renderer} implementation.
 * It requires a @{@link GLTouchHandler} to listen for OpenGL screen touch events
 */
public class GLSurfaceView extends android.opengl.GLSurfaceView {

    private final static String TAG = GLSurfaceView.class.getSimpleName();

    @Inject
    private GLTouchHandler touchHandler;

    private Renderer renderer;

    public GLSurfaceView(Context context) {
        super(context);

        // debug
        Log.i(TAG,"GLSurfaceView created: " + System.identityHashCode(this));
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // debug
        Log.i(TAG,"GLSurfaceView created: " + System.identityHashCode(this));
    }

    @Override
    public void setRenderer(Renderer renderer) {

        Log.i(TAG,"GLSurfaceView setRenderer: " + System.identityHashCode(renderer));

        this.renderer = renderer;
        super.setRenderer(renderer);
    }

    public Renderer getRenderer() {
        return renderer;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // send the event to the manager
        if (this.touchHandler != null) {
            return touchHandler.onSurfaceTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }
}
