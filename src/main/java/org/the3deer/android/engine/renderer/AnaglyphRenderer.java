package org.the3deer.android.engine.renderer;

import android.opengl.GLES20;

import org.the3deer.util.bean.Bean;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * An anaglyph is a technique used to create a 3D effect by encoding
 * a stereoscopic image into a single image, typically using two different colors.
 * When viewed through anaglyph glasses, which have colored filters (often red and blue),
 * the viewer sees a 3D image
 */
//public class AnaglyphRenderer implements Renderer, EventListener {
@Bean
public class AnaglyphRenderer extends DefaultRenderer {

    private static final Logger logger = Logger.getLogger(AnaglyphRenderer.class.getSimpleName());

    @Inject
    private List<Drawer> drawers;
    @Inject
    private Model sceneManager;
    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    // camera:  (left red eye) (right blue eye)
    private final Config leftConf = new Config();

    private Config rightConf = new Config();

    public AnaglyphRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    public void onDrawFrame() {

        // get current scene
        final Scene scene = sceneManager.getActiveScene();

        // assert
        if (scene == null || scene.getActiveCamera() == null) return;

        // 2 cameras
        final Camera[] stereoCamera = scene.getActiveCamera().toStereo(Constants.EYE_DISTANCE, Constants.UNIT);
        final Camera leftCamera = stereoCamera[0];
        final Camera rightCamera = stereoCamera[1];

        // camera:  (left red eye) (right blue eye)
        leftConf.camera = leftCamera;
        rightConf.camera = rightCamera;

        GLES20.glClearColor(0, 0, 0, 1);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Ensure depth testing is enabled at the start of your rendering loop
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        //Set the depth function: Less than Validators.or Equal.
        // This can sometimes help when drawing multiple passes to the same depth buffer,
        // especially if z-fighting is a minor issue or if co-planar surfaces are involved.
        // Default is GL_LESS.
        GLES20.glDepthFunc(GLES20.GL_LEQUAL);

        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
        GLES20.glColorMask(true, false, false, true); //
        GLES20.glDepthMask(true); // Enable depth writing for left eye

        // draw
        for (int i = 0; i < drawers.size(); i++) {
            if (!drawers.get(i).isEnabled()) {
                continue;
            }
            try {
                drawers.get(i).onDrawFrame(leftConf);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }


       GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // 2. Draw Right Eye (Blue)
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glColorMask(false, false, true, false); // Write only to Blue channel
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
        GLES20.glDepthMask(true); // Enable depth writing (so it depth tests against itself correctly)

        // draw
        for (int i = 0; i < drawers.size(); i++) {
            if (!drawers.get(i).isEnabled()) {
                continue;
            }
            try {
                drawers.get(i).onDrawFrame(rightConf);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        // Reset color mask for subsequent rendering
        GLES20.glColorMask(true, true, true, true);
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}