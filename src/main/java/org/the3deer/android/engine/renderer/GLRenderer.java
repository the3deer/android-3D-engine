package org.the3deer.android.engine.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;

import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventManager;

import java.util.Arrays;
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
@Bean(name = "OpenGL Settings")
public class GLRenderer implements GLSurfaceView.Renderer {

    private final static String TAG = GLRenderer.class.getSimpleName();

    @Inject
    private Model model;
    @Inject
    private Screen screen;
    @Inject
    private EventManager eventManager;
    @Inject
    private List<RenderListener> listeners;
    @Inject
    private Map<String,Renderer> renderers;

    @BeanProperty(name = "Renderers", description = "Select the renderer")
    private String activeRenderer;

    /**
     * Background GL clear color. Default is light gray
     */
    @BeanProperty(name = "Background Color", description = "Select the default color for 3D models", valueNames = {"White", "Gray", "Black"})
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
    public GLRenderer() {
        Log.i(TAG,"GLRenderer instantiated: " + System.identityHashCode(this));
    }

    @BeanInit
    public void setUp(){
        Log.i(TAG,"GLRenderer setUp: " + System.identityHashCode(this));

        if (renderers == null || renderers.isEmpty()) {
            throw new IllegalArgumentException("No renderers found");
        }

        if (screen == null) {
            throw new IllegalArgumentException("Screen not found");
        }

        if (activeRenderer == null){
            activeRenderer = renderers.keySet().toArray(new String[0])[0];
        }
    }

    public Screen getScreen() {
        return screen;
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float[] getBackgroundColor() {
        return backgroundColor;
    }

    @BeanProperty(name = "backgroundColor", valueNames = {"White", "Gray", "Black"})
    public List<float[]> getBackgroundColorValues() {
        return Arrays.asList(Constants.COLOR_WHITE.clone(), Constants.COLOR_GRAY.clone(), Constants.COLOR_BLACK.clone());
    }

    @BeanProperty(name = "activeRenderer")
    public List<String> getActiveRendererValues() {
        return Arrays.asList(this.renderers.keySet().toArray(new String[0]));
    }

    public void updateModel(Model model){
        this.model = model;
    }

    public void updateColor(float[] color){
    }


    public void setShaders(Map<String,Renderer> renderers) {
        this.renderers = renderers;
    }

    public float getNear() {
        return Constants.near;
    }

    public float getFar() {
        return Constants.far;
    }

    public void setActiveRenderer(String rendererId){

        // check
        if (!renderers.containsKey(rendererId))
            throw new IllegalArgumentException("Renderer not found: " + rendererId);

        // enable
        Objects.requireNonNull(this.renderers.get(rendererId)).setEnabled(true);

        // update
        this.activeRenderer = rendererId;

        // debug
        Log.d(TAG, "Active renderer: " + activeRenderer);
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
        Log.i(TAG, "onSurfaceChanged. with: " + width + ", height: " + height);

        // update model
        if (this.screen != null) {
            this.screen.setSize(width, height);
            this.screenInitialized = true;
        }

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
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // check
        if (activeRenderer == null) return;

        // get renderer
        Renderer renderer = renderers.get(activeRenderer);
        if (renderer == null) return;

        // check
        if (!renderer.isEnabled()) return;

        // scene not ready
        if (!traced) {
            Log.d(TAG, "onDrawFrame. First draw...");
        }

        // initialize screen in case this bean was initialized lately
        if (!this.screenInitialized && this.screen != null) {

            // update model
            this.screen.setSize(width, height);
            this.screenInitialized = true;

            // fire late events
            if (eventManager != null) {
                eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CREATED));
                eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CHANGED,
                        width, height));
            }
        }

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

        // prepare listeners
        if (listeners != null) {

            // scene not ready
            if (!traced) {
                Log.v(TAG, "onDrawFrame. Invoking listeners... "+ listeners.size());
            }

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
            Log.v(TAG, "onDrawFrame. Invoking renderers... "+ renderers);
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
                Log.e(TAG, "Exception on fps: "+e.getMessage(), e);
            }
        }

        // debug
        if (!traced) {
            Log.i(TAG, "onDrawFrame. First draw finished.");
            traced = true;
        }
    }
}
