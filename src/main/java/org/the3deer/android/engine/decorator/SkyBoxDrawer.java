package org.the3deer.android.engine.decorator;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.objects.SkyBox;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.ShaderResource;
import org.the3deer.android.engine.util.Rescaler;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

@Bean(name = "SkyBox Decorator", description = "Draws a 3D Skybox as the scene background")
public class SkyBoxDrawer implements Drawer {

    @Inject
    private ShaderFactory shaderFactory;

    @Inject
    private Screen screen;

    @Inject
    private Camera camera;

    @BeanProperty
    private boolean enabled = true;

    // data
    @BeanProperty(name = "SkyBox", description = "Select the environment background", values = {"None", "Sea", "Sand"}, valueNames = {"None", "Sea", "Sand"})
    private String activeSkyBox;
    private Map<String, Object3D> skyBoxes3D = new HashMap<>();

    public float[] projectionMatrix = new float[16];

    private boolean traced;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setActiveSkyBox(String id) {

        // debug
        Log.i("SkyBoxDrawer", "Setting active sky box to '" + id + "'");

        // lazy building of the 3d object
        loadSkyBox(id);

        activeSkyBox = id;
    }

/*    @Override
    public List<? extends Object3D> getObjects() {
        if (this.skyboxId < 0 || skyBoxes3D == null || this.skyboxId >= skyBoxes.length) {
            return Collections.emptyList();
        }
        return Collections.singletonList(skyBoxes3D[skyboxId]);
    }*/

    @BeanInit
    public void setUp() {
        Matrix.frustumM(projectionMatrix, 0, -screen.getRatio(), screen.getRatio(),
                -1f, 1f, Constants.near, Constants.far);
    }

    private void loadSkyBox(String skyboxId) {

        // get skybox 3d
        Object3D skybox3D = skyBoxes3D.get(skyboxId);

        // return
        if (skybox3D != null) return;

        try {
            // lazy initialization of the skybox
            final SkyBox skybox;
            switch (skyboxId) {
                case "Sea":
                    skybox = SkyBox.getSkyBox1();
                    break;
                case "Sand":
                    skybox = SkyBox.getSkyBox2();
                    break;
                default:
                    return;
            }

            // debug
            Log.i("SkyBoxDrawer", "Building sky box... skybox: " + skyboxId);

            // build skybox
            skybox3D = SkyBox.build(skybox);

            // rescale
            Rescaler.rescale(skybox3D, 1f);
            final float scale = Constants.SKYBOX_SIZE;
            skybox3D.setScale(scale, scale, scale);

            // customize
            skybox3D.setColor(Constants.COLOR_BIT_TRANSPARENT);

            // register skybox
            skyBoxes3D.put(skyboxId, skybox3D);
        } catch (IOException e) {
            Log.e("SkyBoxDrawer", "Error building sky box '" + skyboxId + "'. " + e.getMessage(), e);
            enabled = false;
            return;
        }
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {
        draw(config);
    }

    //@Override
    private void draw(Config config) {

        // enabled?
        if (!enabled) return;

        // configured?
        if (this.activeSkyBox == null) return;

        // get skybox 3d
        final Object3D skybox3D = skyBoxes3D.get(this.activeSkyBox);

        // check
        if (skybox3D == null) {
            Log.e("SkyBoxDrawer", "Skybox '" + this.activeSkyBox + "' not found");
            enabled = false;
            return;
        }

        // draw
        try {

            // debug
            if (!traced) {
                Log.i("SkyBoxDrawer", "Drawing sky box... id: " + activeSkyBox);
            }

            // paint
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            Shader shader = shaderFactory.getShader(ShaderResource.SKYBOX);

            final Camera camera = config != null && config.camera != null ? config.camera : this.camera;
            shader.draw(skybox3D, this.projectionMatrix, camera.getViewMatrix(),
                    null, null, camera.getPos(), skybox3D.getDrawMode(), skybox3D.getDrawSize());

            // debug
            if (!traced) {
                Log.i("SkyBoxDrawer", "Sky box first draw finished. id: " + activeSkyBox);
                traced = true;
            }

        } catch (Throwable ex) {
            Log.e("SkyBoxDrawer", "Error rendering sky box. " + ex.getMessage(), ex);
            enabled = false;
        }
    }
}
