package org.the3deer.engine.decorator;

import android.opengl.GLES20;

import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.ShaderResource;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Screen;
import org.the3deer.engine.objects.Skybox;
import org.the3deer.engine.renderer.Drawer;
import org.the3deer.engine.util.Rescaler;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.opengl.Matrix;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

@Bean
public class SkyboxDrawer implements Drawer {

    private static final Logger logger = Logger.getLogger(SkyboxDrawer.class.getSimpleName());

    @Inject
    private ShaderFactory shaderFactory;

    @Inject
    private Screen screen;

    @Inject
    private Camera camera;

    @BeanProperty
    private boolean enabled;

    // data
    @BeanProperty(name = "skybox", values = {"none", "sea", "sand", "dynamic"})
    private String activeSkyBox;
    private final Map<String, Object3D> skyboxes3D = new HashMap<>();

    public final float[] projectionMatrix = new float[16];

    private boolean traced;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setActiveSkyBox(final String id) {

        // debug
        logger.info("Setting active sky box to '" + id + "'");

        // lazy building of the 3d object
        loadSkyBox(id);

        activeSkyBox = id;
    }

/*    @Override
    public List<? extends Object3D> getObjects() {
        if (this.skyboxId < 0 || skyboxes3D == null || this.skyboxId >= skyboxes.length) {
            return Collections.emptyList();
        }
        return Collections.singletonList(skyboxes3D[skyboxId]);
    }*/

    @BeanInit
    public void setUp() {
        Matrix.frustumM(projectionMatrix, 0, -screen.getRatio(), screen.getRatio(),
                -1f, 1f, Constants.near, Constants.far);
    }

    private void loadSkyBox(final String skyboxId) {

        // get skybox 3d
        Object3D skybox3D = skyboxes3D.get(skyboxId);

        // return
        if (skybox3D != null) return;

        try {
            // lazy initialization of the skybox
            final Skybox skybox;
            switch (skyboxId) {
                case "sea":
                    skybox = Skybox.getSkybox1();
                    break;
                case "sand":
                    skybox = Skybox.getSkybox2();
                    break;
                case "dynamic":
                    skybox = Skybox.getSkybox1(); // Use any base, shader will override
                    break;
                default:
                    throw new IllegalArgumentException("Skybox '" + skyboxId + "' not found");
            }

            // debug
            logger.info("Building sky box... skybox: " + skyboxId);

            // build skybox
            skybox3D = Skybox.build(skybox);

            // rescale
            Rescaler.rescale(skybox3D, 1f);
            final float scale = Constants.SKYBOX_SIZE;
            skybox3D.setScale(scale, scale, scale);

            // customize
            skybox3D.setColor(Constants.COLOR_BIT_TRANSPARENT);

            // register skybox
            skyboxes3D.put(skyboxId, skybox3D);
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Error building sky box '" + skyboxId + "'. " + e.getMessage(), e);
            enabled = false;
            return;
        }
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(final Config config) {
        draw(config);
    }

    //@Override
    private void draw(final Config config) {

        // enabled?
        if (!enabled) return;

        // configured?
        if (this.activeSkyBox == null) return;

        // get skybox 3d
        final Object3D skybox3D = skyboxes3D.get(this.activeSkyBox);

        // check
        if (skybox3D == null) {
            logger.log(Level.SEVERE,  "Skybox '" + this.activeSkyBox + "' not found");
            enabled = false;
            return;
        }

        // draw
        try {

            // debug
            if (!traced) {
                logger.info("Drawing sky box... id: " + activeSkyBox);
            }

            // paint
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glDisable(GLES20.GL_CULL_FACE);

            final Shader shader = shaderFactory.getShader(ShaderResource.SKYBOX);
            
            // Calculate Day Progress (0.0 to 1.0)
            final Calendar calendar = Calendar.getInstance();
            final float dayProgress = (calendar.get(Calendar.HOUR_OF_DAY) * 3600f +
                                 calendar.get(Calendar.MINUTE) * 60f +
                                 calendar.get(Calendar.SECOND)) / 86400f;
            
            // Pass day progress to shader
            shader.setTime(dayProgress);
            
            // Toggle between textured cubemap and procedural sky
            final boolean isDynamic = "dynamic".equals(this.activeSkyBox);
            shader.setTexturesEnabled(!isDynamic);

            final Camera camera = config != null && config.camera != null ? config.camera : this.camera;
            shader.draw(skybox3D, this.projectionMatrix, camera.getViewMatrix(),
                    null, null, camera.getPos(), skybox3D.getDrawMode(), skybox3D.getDrawSize());

            // debug
            if (!traced) {
                logger.info("Sky box first draw finished. id: " + activeSkyBox + ", time: " + dayProgress);
                traced = true;
            }

        } catch (Throwable ex) {
            logger.log(Level.SEVERE,  "Error rendering sky box. " + ex.getMessage(), ex);
            enabled = false;
        }
    }
}