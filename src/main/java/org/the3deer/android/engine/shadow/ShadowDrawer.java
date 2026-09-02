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
import org.the3deer.android.engine.model.Dimensions;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.util.math.Math3DUtils;

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

        final float[] location = light.getLocation();

        // 0. Update light matrices
        final Dimensions dimensions = scene.getDimensions();
        if (dimensions == null) return;

        // Calculate light view matrix
        final float[] look = Math3DUtils.negate(location);
        Math3DUtils.normalizeVector(look);
        final float[] upTemp = new float[]{0, 1, 0};
        final float[] right = Math3DUtils.crossProduct(look, upTemp);
        Math3DUtils.normalizeVector(right);
        final float[] up = Math3DUtils.crossProduct(right, look);
        Math3DUtils.normalizeVector(up);
        Matrix.setLookAtM(lightViewMatrix, 0,
                location[0], location[1], location[2],
                0f, 0f, 0f,
                up[0], up[1], up[2]);

        // Calculate light projection matrix (orthographic)
        // Transform scene bounding box to light view space to find optimal projection planes
        final float[] min = dimensions.getMin();
        final float[] max = dimensions.getMax();
        final float[][] corners = new float[8][4];
        corners[0] = new float[]{min[0], min[1], min[2], 1.0f};
        corners[1] = new float[]{min[0], min[1], max[2], 1.0f};
        corners[2] = new float[]{min[0], max[1], min[2], 1.0f};
        corners[3] = new float[]{min[0], max[1], max[2], 1.0f};
        corners[4] = new float[]{max[0], min[1], min[2], 1.0f};
        corners[5] = new float[]{max[0], min[1], max[2], 1.0f};
        corners[6] = new float[]{max[0], max[1], min[2], 1.0f};
        corners[7] = new float[]{max[0], max[1], max[2], 1.0f};

        float l = Float.MAX_VALUE, r = -Float.MAX_VALUE;
        float b = Float.MAX_VALUE, t = -Float.MAX_VALUE;
        float n = Float.MAX_VALUE, f = -Float.MAX_VALUE;

        final float[] transformed = new float[4];
        for (float[] corner : corners) {
            Matrix.multiplyMV(transformed, 0, lightViewMatrix, 0, corner, 0);
            l = Math.min(l, transformed[0]);
            r = Math.max(r, transformed[0]);
            b = Math.min(b, transformed[1]);
            t = Math.max(t, transformed[1]);
            n = Math.min(n, -transformed[2]);
            f = Math.max(f, -transformed[2]);
        }

        // Add some padding to avoid clipping
        final float padding = 0.1f * Math.max(r - l, t - b);
        Matrix.orthoM(mLightProjectionMatrix, 0, l - padding, r + padding, b - padding, t + padding, n - padding, f + padding);

        // 1. Shadow Map Pass: Cull front faces to reduce shadow acne
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);

        shadowsRenderer.onPrepareFrame(shaderFactory, config, camera.getProjectionMatrix(), camera.getViewMatrix(), mLightProjectionMatrix, lightViewMatrix, location, scene);

        // 2. Main Render Pass: Restore standard back-face culling
        GLES20.glCullFace(GLES20.GL_BACK);
        shadowsRenderer.onDrawFrame(shaderFactory, config, camera.getProjectionMatrix(), camera.getViewMatrix(), mLightProjectionMatrix, lightViewMatrix, location, scene);
    }
}
