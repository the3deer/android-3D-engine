package org.the3deer.android.engine.shadow;

import android.app.Activity;
import android.opengl.GLES20;

import org.the3deer.android.util.Matrix;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
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
    private ShadowsRenderer shadowsRenderer = new ShadowsRenderer();
    private final float[] lightViewMatrix = new float[16];
    /**
     * Projection matrix from point of light source
     */
    private final float[] mLightProjectionMatrix = new float[16];

    public ShadowDrawer() throws IOException, IllegalAccessException {
    }

    @BeanInit
    public void setUp(){
        if (activity == null || shaderFactory == null) return;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void reset() {
        if (shadowsRenderer != null) {
            shadowsRenderer.reset();
        }
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
        if (sceneCamera == null || light == null) return;

        // get camera (it will be different in stereoscopic mode)
        final Camera camera = config != null && config.camera != null? config.camera : sceneCamera;

        // check
        if (scene.getObjects().isEmpty()) return;

        float ratio = (float) config.viewPortWidth / config.viewPortHeigth;

        float bottom = -1.0f;
        float top = 1.0f;
        float near = 1.0f;
        float far = 10000.0f;

        Matrix.frustumM(mLightProjectionMatrix, 0, -1.1f*ratio, 1.1f*ratio, 1.1f*bottom, 1.1f*top, near, far);

        // 1. Shadow Map Pass: Cull front faces to reduce shadow acne
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);
        //light.setLocation(new float[]{10,10,10});
        float[] location = light.getLocation();
        //location = new float[]{10,10,10};

        shadowsRenderer.onPrepareFrame(shaderFactory, config, camera.getProjectionMatrix(), camera.getViewMatrix(), mLightProjectionMatrix, location, scene);

        // 2. Main Render Pass: Restore standard back-face culling
        GLES20.glCullFace(GLES20.GL_BACK);
        shadowsRenderer.onDrawFrame(shaderFactory, config, camera.getProjectionMatrix(), camera.getViewMatrix(), mLightProjectionMatrix, location, scene);
    }
}
