package org.the3deer.engine.android.renderer;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import org.the3deer.engine.ModelEngine;
import org.the3deer.engine.android.ModelEngineViewModel;
import org.the3deer.engine.android.shader.ShaderFactory;
import org.the3deer.engine.event.FPSEvent;
import org.the3deer.engine.event.GLEvent;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Screen;
import org.the3deer.engine.renderer.RenderListener;
import org.the3deer.engine.renderer.Renderer;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL renderer
 * It calls all the @{@link Renderer#onDrawFrame()} in the list
 */
@Bean
public class GLRenderer implements GLSurfaceView.Renderer {

    private static final Logger logger = Logger.getLogger(GLRenderer.class.getSimpleName());

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
    private String activeRenderer;

    @Inject
    private ShaderFactory shaderFactory;

    private Renderer renderer;

    /**
     * Background GL clear color. Default is light gray
     */
    @BeanProperty(values = {"white", "gray", "black"})
    private String backgroundColor = "gray";
    private float[] backgroundColorSelected = Constants.COLOR_GRAY;
    /**
     * GL Screen width
     */
    private int width;
    /**
     * GL Screen width
     */
    private int height;

    // frames per second
    private long framesPerSecondTime = -1;
    private int framesPerSecondCounter = 0;

    private boolean traced;

    /**
     * Construct a new renderer for the specified surface view
     */
    public GLRenderer(ModelEngineViewModel viewModel) {
        logger.info("GLRenderer instantiated: " + System.identityHashCode(this));
        this.viewModel = viewModel;
    }

    @BeanInit
    public void setUp() {
        logger.info("GLRenderer setUp: " + System.identityHashCode(this));

        if (renderers == null || renderers.isEmpty()) {
            throw new IllegalArgumentException("No renderers found");
        }

        if (renderer == null) {
            renderer = renderers.get("renderer.default");
        }
    }

    @BeanProperty
    public void setBackgroundColor(String color) {
        if (color == null) return;
        switch (color) {
            case "gray":
                this.backgroundColorSelected = Constants.COLOR_GRAY;
                break;
            case "white":
                this.backgroundColorSelected = Constants.COLOR_WHITE;
                break;
            case "black":
                this.backgroundColorSelected = Constants.COLOR_BLACK;
                break;
            default:
                throw new IllegalArgumentException("Unknown color: " + color);
        }
    }

    @BeanProperty
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
    public String getActiveRenderer() {
        return activeRenderer;
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
        if (this.renderer != null) this.renderer.setEnabled(false);

        // reset new renderer
        renderer.reset();

        // enable new renderer
        renderer.setEnabled(true);

        // update active renderer
        this.renderer = renderer;

        // debug
        logger.config("Active renderer: " + rendererId);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // log event
        logger.config("onSurfaceCreated. config: " + config);

        // Set the background frame color
        GLES20.glClearColor(backgroundColorSelected[0], backgroundColorSelected[1], backgroundColorSelected[2], backgroundColorSelected[3]);

        // Use culling to remove back faces.
        // Don't remove back faces so we can see them
        // GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing for hidden-surface elimination.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Enable not drawing out of view port
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        // Reset Engine (Clears Shader Cache, GPU Buffers, and Texture IDs)
        final ModelEngine activeEngine = viewModel.getActiveEngine();
        if (activeEngine != null) {
            activeEngine.reset();
        }

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
        logger.info("onSurfaceChanged. width: " + width + ", height: " + height);

        // fire event
        if (renderers != null) {
            for (Renderer renderer : renderers.values()) {
                try {
                    Objects.requireNonNull(renderer).onSurfaceChanged(width, height);
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Exception on delegate: " + renderer, ex);
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

        // check
        if (renderer == null || !renderer.isEnabled()) return;

        // Default viewport
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);

        // Default color
        GLES20.glClearColor(backgroundColorSelected[0], backgroundColorSelected[1], backgroundColorSelected[2], backgroundColorSelected[3]);
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
           logger.finest("onDrawFrame. Invoking listeners... " + listeners.size());
        }

        // prepare listeners
        if (listeners != null) {

            for (int i = 0; i < listeners.size(); i++) {
                try {
                    //logger.log(Level.SEVERE, "onPrepareFrame ("+i+"): "+listeners.get(i));
                    listeners.get(i).onPrepareFrame();
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Exception on delegate: " + renderer, ex);
                    renderer.setEnabled(false);
                    break;
                }
            }
        }

        // debug
        if (!traced) {
           logger.finest("onDrawFrame. Invoking renderers... " + renderers);
        }

        try {
            renderer.onDrawFrame();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception on delegate: " + renderer, ex);
            renderer.setEnabled(false);
        }

        if (eventManager != null) {
            try {
                if (framesPerSecondTime == -1) {
                    framesPerSecondTime = System.currentTimeMillis();
                    framesPerSecondCounter++;
                } else if (System.currentTimeMillis() > framesPerSecondTime + 1000) {
                    int framesPerSecond = framesPerSecondCounter;
                    framesPerSecondCounter = 1;
                    framesPerSecondTime = System.currentTimeMillis();
                    eventManager.propagate(new FPSEvent(this, framesPerSecond));
                    //logger.finest("FPS: " + framesPerSecond);
                } else {
                    framesPerSecondCounter++;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Exception on fps: " + e.getMessage(), e);
            }
        }

        // debug
        if (!traced) {
            logger.info("onDrawFrame. First draw finished.");
            traced = true;
        }
    }
}
