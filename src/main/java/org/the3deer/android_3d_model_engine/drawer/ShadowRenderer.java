package org.the3deer.android_3d_model_engine.drawer;

import android.app.Activity;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.Plane2;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shadow.ShadowsRenderer;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Render shadows for the visible objects
 * The light comes from 1 single light source
 */
public class ShadowRenderer implements Renderer {

    @Inject
    private Activity activity;
    @Inject
    private Scene scene;
    /*@Inject
    private Camera camera;*/
    @Inject
    private Light light;

    // shadowing
    private boolean enabled = false;
    private ShadowsRenderer shadowsRenderer;
    final Object3DData plane2 = Plane2.build();
    final Object3DData plane3 = Plane2.build();
    private final float[] lightViewMatrix = new float[16];

    public ShadowRenderer() throws IOException, IllegalAccessException {
    }

    public void setUp(){
        if (activity == null) return;
        this.shadowsRenderer = new ShadowsRenderer(activity);
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*@Override
    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }*/

    public void onSurfaceChanged(int width, int height) {
        if (enabled) {
            shadowsRenderer.onSurfaceChanged(width, height);
        }
    }

    @Override
    public void onDrawFrame() {

        // check
        if (!enabled) return;

        // assert
        if (scene == null || scene.getCamera() == null || light == null
                || plane2 == null || shadowsRenderer == null) return;

        // check
        if (scene.getObjects().isEmpty()) return;

        // shadow plane
        if (!scene.getObjects().contains(plane2)) {
            //scene.getLightBulb().setLocation(new float[]{25f, 200f, 0f});
            plane2.setColor(Constants.COLOR_CYAN);
            plane2.setLocation(new float[]{0f, -Constants.UNIT/2, 0f});
            plane2.setPinned(true);
            scene.addObject(plane2);
        }

        // shadow buffer
        shadowsRenderer.onPrepareFrame(scene.getCamera().getProjectionMatrix(), scene.getCamera().getViewMatrix(), light.getLocation(), scene);

        // render with shadows
        shadowsRenderer.onDrawFrame(scene.getCamera().getProjectionMatrix(), scene.getCamera().getViewMatrix(), light.getLocation(), scene);
    }
}
