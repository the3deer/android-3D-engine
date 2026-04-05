package org.the3deer.android.engine.renderer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * This is the actual OpenGL surface.
 * It requires a @{@link Renderer} implementation.
 * It requires a @{@link TouchHandler} to listen for OpenGL screen touch events
 */
public class GLSurfaceView extends android.opengl.GLSurfaceView {

    private static final Logger logger = Logger.getLogger(GLSurfaceView.class.getSimpleName());

    @Inject
    private TouchHandler touchHandler;

    private Renderer renderer;

    public GLSurfaceView(Context context) {
        super(context);

        // debug
        logger.info("GLSurfaceView created: " + System.identityHashCode(this));
    }

    public GLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // debug
        logger.info("GLSurfaceView created: " + System.identityHashCode(this));
    }

    @Override
    public void setRenderer(Renderer renderer) {

        logger.info("GLSurfaceView setRenderer: " + System.identityHashCode(renderer));

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
            return touchHandler.onSurfaceTouchEvent(new org.the3deer.android.engine.event.MotionEvent(event));
        }
        return super.onTouchEvent(event);
    }
}
