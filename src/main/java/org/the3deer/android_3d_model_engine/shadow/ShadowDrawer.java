package org.the3deer.android_3d_model_engine.shadow;

import android.app.Activity;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Render shadows for the visible objects
 * The light comes from 1 single light source
 */
public class ShadowDrawer implements Drawer {

    @Inject
    private ShaderFactory shaderFactory;
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
    private final float[] lightViewMatrix = new float[16];

    public ShadowDrawer() throws IOException, IllegalAccessException {
    }

    public void setUp(){
        if (activity == null || shaderFactory == null) return;

        this.shadowsRenderer = new ShadowsRenderer(activity, shaderFactory);
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
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {

        // check
        if (!enabled) return;

        // assert

        if (scene == null || scene.getCamera() == null || light == null || shadowsRenderer == null) return;
        final Camera camera = config != null && config.camera != null? config.camera : scene.getCamera();

        // check
        if (scene.getObjects().isEmpty()) return;

        // shadow buffer
        shadowsRenderer.onPrepareFrame(camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);

        // render with shadows
        shadowsRenderer.onDrawFrame(camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);
    }
}
