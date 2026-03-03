package org.the3deer.android_3d_model_engine.shadow;

import android.app.Activity;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.bean.BeanInit;

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
    private SceneManager sceneManager;
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

    @BeanInit
    public void setUp(){
        if (activity == null || shaderFactory == null) return;

        this.shadowsRenderer = new ShadowsRenderer();
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
        shadowsRenderer.onSurfaceChanged(width, height);
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
        if (sceneManager == null) return;

        // current scene
        final Scene scene = sceneManager.getCurrentScene();

        // check
        if (scene == null) return;

        // check
        final Camera sceneCamera = scene.getCamera();
        if (sceneCamera == null || light == null || shadowsRenderer == null) return;

        // get camera (it will be different in stereoscopic mode)
        final Camera camera = config != null && config.camera != null? config.camera : sceneCamera;

        // check
        if (scene.getObjects().isEmpty()) return;

        if (!shadowsRenderer.enabled) return;

        // shadow buffer
        shadowsRenderer.onPrepareFrame(shaderFactory, camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);

        // render with shadows
        shadowsRenderer.onDrawFrame(shaderFactory, camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);
    }
}
