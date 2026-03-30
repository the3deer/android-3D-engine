package org.the3deer.android.engine.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

import org.the3deer.android.engine.ModelEngineViewModel;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL renderer
 * It calls all the @{@link Renderer#onDrawFrame()} in the list
 */
@Bean
public class GLRenderer implements GLSurfaceView.Renderer {

    private final static String TAG = GLRenderer.class.getSimpleName();

    private final ModelEngineViewModel viewModel;

    @Inject
    private Screen screen;
    @Inject
    private EventManager eventManager;
    @Inject
    private List<RenderListener> listeners;
    @Inject
    private Map<String, Renderer> renderers;

    @BeanProperty(name = "renderer")
    private Renderer activeRenderer;

    /**
     * Background GL clear color. Default is light gray
     */
    @BeanProperty(name = "backgroundColor", values = {"white", "gray", "black"})
    private float[] backgroundColor = Constants.COLOR_GRAY;
    /**
     * GL Screen width
     */
    private int width;
    /**
     * GL Screen width
     */
    private int height;
    /**
     * Toggle feature to initialize the Engine's screen
     */
    private boolean screenInitialized;

    // frames per second
    private long framesPerSecondTime = -1;
    private int framesPerSecondCounter = 0;

    private boolean traced;

    /**
     * Construct a new renderer for the specified surface view
     */
    public GLRenderer(ModelEngineViewModel viewModel) {
        Log.i(TAG, "GLRenderer instantiated: " + System.identityHashCode(this));
        this.viewModel = viewModel;
    }

    @BeanInit
    public void setUp() {
        Log.i(TAG, "GLRenderer setUp: " + System.identityHashCode(this));

        if (renderers == null || renderers.isEmpty()) {
            throw new IllegalArgumentException("No renderers found");
        }

        if (activeRenderer == null) {
            activeRenderer = renderers.get("renderer.default");
        }
    }

    @BeanProperty
    public void setBackgroundColor(String color) {
        if (color == null) return;
        switch (color) {
            case "gray":
                this.backgroundColor = Constants.COLOR_GRAY;
                break;
            case "white":
                this.backgroundColor = Constants.COLOR_WHITE;
                break;
            case "black":
                this.backgroundColor = Constants.COLOR_BLACK;
                break;
            default:
                throw new IllegalArgumentException("Unknown color: " + color);
        }
    }

    @BeanProperty(name = "renderer")
    public List<String> getActiveRendererValues() {
        return new ArrayList<>(renderers.keySet());
    }

    public void setShaders(Map<String, Renderer> renderers) {
        this.renderers = renderers;
    }

    public float getNear() {
        return Constants.near;
    }

    public float getFar() {
        return Constants.far;
    }

    @BeanProperty
    public void setActiveRenderer(String rendererId) {

        // check
        if (rendererId == null) throw new IllegalArgumentException("Renderer id cannot be null");

        // get renderer
        final Renderer renderer = renderers.get(rendererId);

        // check
        if (renderer == null) throw new IllegalStateException("Renderer not found: " + rendererId +
                ", available renderers: " + renderers.keySet() +
                ", active renderer: " + activeRenderer);

        // disable current renderer
        if (this.activeRenderer != null) this.activeRenderer.setEnabled(false);

        // enable new renderer
        renderer.setEnabled(true);

        // update active renderer
        this.activeRenderer = renderer;

        // debug
        Log.d(TAG, "Active renderer: " + rendererId);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // log event
        Log.d(TAG, "onSurfaceCreated. config: " + config);

        // Set the background frame color
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        // Use culling to remove back faces.
        // Don't remove back faces so we can see them
        // GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing for hidden-surface elimination.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Enable not drawing out of view port
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        if (eventManager != null) {
            eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CREATED));
        }

    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {

        // init
        this.width = width;
        this.height = height;

        // log event
        Log.i(TAG, "onSurfaceChanged. width: " + width + ", height: " + height);

        // fire event
        if (renderers != null) {
            for (Renderer renderer : renderers.values()) {
                try {
                    Objects.requireNonNull(renderer).onSurfaceChanged(width, height);
                } catch (Exception ex) {
                    Log.e(TAG, "Exception on delegate: " + renderer, ex);
                }
            }
        }

        // fire event
        if (eventManager != null) {
            eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CHANGED,
                    width, height));
        }

        // forward event
        viewModel.onSurfaceChanged(width, height);
    }

    public int getWidth() {
        return width;
    }


    public int getHeight() {
        return height;
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // get active renderer
        final Renderer renderer = activeRenderer;

        // check
        if (renderer == null || !renderer.isEnabled()) return;

        // Default viewport
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);

        // Default color
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glColorMask(true, true, true, true);
        GLES20.glLineWidth((float) Math.PI);

        // Default blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // enable depth testing
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // scene not ready
        if (!traced) {
            Log.v(TAG, "onDrawFrame. Invoking listeners... " + listeners.size());
        }

        // prepare listeners
        if (listeners != null) {

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    //Log.e(TAG, "onPrepareFrame ("+i+"): "+listeners.get(i));
                    listeners.get(i).onPrepareFrame();
                } catch (Exception ex) {
                    Log.e(TAG, "Exception on delegate: " + renderers.get(i), ex);
                    renderer.setEnabled(false);
                    break;
                }
            }
        }

        // debug
        if (!traced) {
            Log.v(TAG, "onDrawFrame. Invoking renderers... " + renderers);
        }

        try {
            renderer.onDrawFrame();
        } catch (Exception ex) {
            Log.e(TAG, "Exception on delegate: " + renderer, ex);
            renderer.setEnabled(false);
        }

        if (eventManager != null) {
            try {
                if (framesPerSecondTime == -1) {
                    framesPerSecondTime = SystemClock.elapsedRealtime();
                    framesPerSecondCounter++;
                } else if (SystemClock.elapsedRealtime() > framesPerSecondTime + 1000) {
                    int framesPerSecond = framesPerSecondCounter;
                    framesPerSecondCounter = 1;
                    framesPerSecondTime = SystemClock.elapsedRealtime();
                    eventManager.propagate(new FPSEvent(this, framesPerSecond));
                    //Log.v(TAG, "FPS: " + framesPerSecond);
                } else {
                    framesPerSecondCounter++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on fps: " + e.getMessage(), e);
            }
        }

        // debug
        if (!traced) {
            Log.i(TAG, "onDrawFrame. First draw finished.");
            traced = true;
        }
    }
}
