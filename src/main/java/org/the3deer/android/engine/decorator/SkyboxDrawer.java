package org.the3deer.android.engine.decorator;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;

import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.objects.Skybox;
import org.the3deer.android.util.Matrix;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.Program;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.android.engine.util.Rescaler;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Skybox drawer that supports both static cubemaps and a procedural dynamic sky.
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
@Bean
public class SkyboxDrawer implements Drawer {

    private static final Logger logger = Logger.getLogger(SkyboxDrawer.class.getSimpleName());

    @Inject
    private ShaderManager shaderManager;

    @Inject
    private Screen screen;

    @Inject
    private Camera camera;

    @BeanProperty
    private boolean enabled;

    // data
    @BeanProperty(name = "skybox", values = {"sea", "sand", "day_night"})
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
                case "day_night":
                    // Use any base mesh, the procedural shader will override all visuals
                    skybox = Skybox.getSkybox1();
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
    public void onDrawFrame(final Renderer.Config config) {
        draw(config);
    }

    private void draw(final Renderer.Config config) {

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

            final Shader shader = shaderManager.getShader(Program.SKYBOX);

            // Set time for dynamic effects (Day progress from 0.0 to 1.0)
            final long now = System.currentTimeMillis();
            final Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(now);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            final long midnight = calendar.getTimeInMillis();
            final float dayProgress = (now - midnight) / 86400000f;
            shader.setTime(dayProgress);
            
            // Toggle between textured cubemap and procedural sky
            final boolean isDynamic = "day_night".equals(this.activeSkyBox);
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
