package org.the3deer.android.engine.decorator;

import android.opengl.GLES20;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.PerspectiveProjection;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.objects.Skybox;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.Program;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;

import java.io.IOException;
import java.util.Arrays;
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
    private Model model;

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

    private final Projection skyboxProjection = new PerspectiveProjection();
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
        // We use the camera's projection matrix dynamically during draw
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
            if (!traced && model.getActiveScene() != null) {
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

            // Get camera and projection
            final Camera camera = config != null && config.camera != null ? config.camera : this.camera;
            final Projection projection = camera.getProjection();

            // Center skybox at the scene's absolute center (or 0,0,0)
            final Scene activeScene = model.getActiveScene();
            final float[] center = activeScene != null ? activeScene.getDimensions().getCenter() : Constants.VECTOR_ZERO;
            skybox3D.setPosition(center[0], center[1], center[2]);

            // Set scale to 10x the model size as requested.
            // This allows the user to zoom out and see the skybox as a "planet" from the outside.
            final float modelSize = activeScene != null ? activeScene.getDimensions().getLargest() : 100f;
            final float skyboxScale = modelSize * 10f;
            skybox3D.setScale(skyboxScale, skyboxScale, skyboxScale);

            // Use the standard view matrix
            final float[] viewMatrix = camera.getViewMatrix();

            // PRIVATE SKYBOX PROJECTION
            // The skybox has its own projection pass to avoid interfering with the model's depth precision.
            final float distToCenter = (float) Math.sqrt(
                    Math.pow(camera.getPos()[0] - center[0], 2) +
                    Math.pow(camera.getPos()[1] - center[1], 2) +
                    Math.pow(camera.getPos()[2] - center[2], 2));

            // Set up private projection aspect ratio
            skyboxProjection.setAspectRatio(projection.getAspectRatio());

            // Calculate safe near and far planes for the skybox universe pass
            // A factor of 2.0 ensures that corners of the skybox (at sqrt(3) ~ 1.732 distance from its center) are not clipped
            final float skyboxFar = (distToCenter + skyboxScale) * 2.0f;
            float skyboxNear = (distToCenter < skyboxScale * 2.0f) ? 0.01f : (distToCenter - skyboxScale * 2.0f) * 0.9f;
            skyboxNear = Math.max(0.001f, skyboxNear);

            // Respect hardware limits for the skybox pass to avoid any face-flickering
            final int[] depthBits = new int[1];
            GLES20.glGetIntegerv(GLES20.GL_DEPTH_BITS, depthBits, 0);
            final float maxHealthyRatio = (depthBits[0] >= 24) ? 10000f : 1000f;
            if (skyboxFar / skyboxNear > maxHealthyRatio) {
                skyboxNear = skyboxFar / maxHealthyRatio;
            }

            skyboxProjection.setNear(skyboxNear);
            skyboxProjection.setFar(skyboxFar);

            shader.draw(skybox3D, skyboxProjection.getMatrix(), viewMatrix,
                    null, null, camera.getPos(), skybox3D.getDrawMode(), skybox3D.getDrawSize());

            // debug
            if (!traced && model.getActiveScene() != null){
                logger.info("Sky box first draw finished. id: " + activeSkyBox +  ", scale: "+ Arrays.toString(skybox3D.getScale()) +", time: " + dayProgress);
                traced = true;
            }

        } catch (Exception ex) {
            logger.log(Level.SEVERE,  "Error rendering sky box. " + ex.getMessage(), ex);
            enabled = false;
            traced = true;
        }
    }
}
