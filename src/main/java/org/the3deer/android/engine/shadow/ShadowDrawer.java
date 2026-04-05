package org.the3deer.android.engine.shadow;

import android.app.Activity;
import android.opengl.GLES20;

import org.the3deer.bean.Bean;
import org.the3deer.bean.BeanInit;
import org.the3deer.bean.BeanProperty;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Light;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.ShaderManager;

import java.io.IOException;

import javax.inject.Inject;

/**
 * Render shadows for the visible objects
 * The light comes from 1 single light source
 */
@Bean(category = "decorators", experimental = true)
public class ShadowDrawer implements Drawer {

    @Inject
    private ShaderManager shaderFactory;
    @Inject
    private Activity activity;
    @Inject
    private Model sceneManager;
    /*@Inject
    private Camera camera;*/
    @Inject
    private Light light;

    // shadowing
    @BeanProperty
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
    public List<? extends Object3D> getObjects() {
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
    public void onDrawFrame(Renderer.Config config) {

        // check
        if (!enabled) return;

        // assert
        if (sceneManager == null) return;

        // current scene
        final Scene scene = sceneManager.getActiveScene();

        // check
        if (scene == null) return;

        // check
        final Camera sceneCamera = scene.getActiveCamera();
        if (sceneCamera == null || light == null || shadowsRenderer == null) return;

        // get camera (it will be different in stereoscopic mode)
        final Camera camera = config != null && config.camera != null? config.camera : sceneCamera;

        // check
        if (scene.getObjects().isEmpty()) return;

        if (!shadowsRenderer.enabled) return;

        // Front-Face Culling for the Shadow Map
        // A very effective trick to reduce acne is to render only the back faces of your objects
        // when creating the depth map. This ensures the depth stored in the map is always slightly
        // "behind" the surface visible to the camera.
        GLES20.glCullFace(GLES20.GL_FRONT);

        // shadow buffer
        shadowsRenderer.onPrepareFrame(shaderFactory, camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);

        // ... render objects to depth FBO ...
        GLES20.glCullFace(GLES20.GL_FRONT); // Restore for normal rendering (was GLES20.glCullFace(GLES20.GL_BACK) in original code, but restoring the state)

        // render with shadows
        shadowsRenderer.onDrawFrame(shaderFactory, camera.getProjectionMatrix(), camera.getViewMatrix(), light.getLocation(), scene);
    }
}
