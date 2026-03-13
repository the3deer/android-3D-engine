package org.the3deer.android_3d_model_engine.renderer;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.view.Renderer;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

/**
 * An anaglyph is a technique used to create a 3D effect by encoding
 * a stereoscopic image into a single image, typically using two different colors.
 * When viewed through anaglyph glasses, which have colored filters (often red and blue),
 * the viewer sees a 3D image
 */
//public class AnaglyphRenderer implements Renderer, EventListener {
public class AnaglyphRenderer implements EventListener, Renderer {

    private final static String TAG = AnaglyphRenderer.class.getSimpleName();

    @Inject
    private List<Drawer> drawers;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    private boolean enabled;

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public AnaglyphRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    public void onDrawFrame() {

        // assert
        if (sceneManager == null) return;

        // get current scene
        final Scene scene = sceneManager.getCurrentScene();

        // assert
        if (scene == null || scene.getCamera() == null) return;

        // 2 cameras
        final Camera[] stereoCamera = scene.getCamera().toStereo(Constants.EYE_DISTANCE, Constants.UNIT);
        final Camera leftCamera = stereoCamera[0];
        final Camera rightCamera = stereoCamera[1];

        // camera:  (left red eye) (right blue eye)
        Drawer.Config leftConf = new Drawer.Config();
        leftConf.camera = leftCamera;

        Drawer.Config rightConf = new Drawer.Config();
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
                Log.e(TAG, e.getMessage(), e);
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
                Log.e(TAG, e.getMessage(), e);
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