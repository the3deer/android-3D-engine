package org.the3deer.android.engine.renderer;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * A stereoscopic rendering is a technique used to create a 3D effect by encoding
 * a stereoscopic image into 2 images side-by-side image, each presenting a different perspective.
 * When viewed through VR/cardboard glasses, which have each lens pointing 1 side of the image
 * the viewer sees a 3D image
 */
//public class AnaglyphRenderer implements Renderer, EventListener {
@Bean
public class StereoscopicRenderer extends DefaultRenderer {

    private final static String TAG = StereoscopicRenderer.class.getSimpleName();

    @Inject
    private Screen screen;
    @Inject
    private List<Drawer> drawers;
    @Inject
    private Model sceneManager;
    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    public StereoscopicRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public void onDrawFrame() {

        // check
        if (!enabled) return;

        // assert
        if (sceneManager == null || screen == null) return;

        // get current scene
        final Scene scene = sceneManager.getActiveScene();

        // assert
        if (scene == null || scene.getActiveCamera() == null) return;

        // 2 cameras
        final Camera[] stereoCamera = scene.getActiveCamera().toStereo(Constants.EYE_DISTANCE, Constants.UNIT);
        final Camera leftCamera = stereoCamera[0];
        final Camera rightCamera = stereoCamera[1];

        // camera:  (left red eye) (right blue eye)
        Drawer.Config leftConf = new Drawer.Config();
        leftConf.camera = leftCamera;

        Drawer.Config rightConf = new Drawer.Config();
        rightConf.camera = rightCamera;

        //GLES20.glClearColor(0, 0, 0, 1);
        //GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // get screen size
        final int width = screen.getWidth();
        final int height = screen.getHeight();

        // configure left viewport
        leftConf.viewPortX = 0;
        leftConf.viewPortY = 0;
        leftConf.viewPortWidth = width / 2;
        leftConf.viewPortHeigth = height;

        // configure right viewport
        rightConf.viewPortX = width / 2;
        rightConf.viewPortY = 0;
        rightConf.viewPortWidth = width / 2;
        rightConf.viewPortHeigth = height;

        // Fix projections
        float halfRatio = (float)(width / 2) / height;
        leftCamera.getProjection().setAspectRatio(halfRatio);
        rightCamera.getProjection().setAspectRatio(halfRatio);


        // draw left eye image
        super.drawFrame(leftConf);

        // draw right eye image
        super.drawFrame(rightConf);
    }
}