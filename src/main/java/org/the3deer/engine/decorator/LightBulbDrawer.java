package org.the3deer.engine.decorator;

import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.ShaderResource;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Light;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.renderer.Drawer;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventListener;

import java.util.Calendar;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

@Bean
public class LightBulbDrawer implements Drawer, EventListener {

    private static final Logger logger = Logger.getLogger(LightBulbDrawer.class.getSimpleName());

    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private Camera camera;
    @Inject
    private Light light;
    @Inject @Named("lightBulb")
    private Object3D lightBulb;

    @BeanProperty
    private boolean enabled;

    public int toggle(){
        this.enabled = !this.enabled;
        logger.info("Toggled light bulb. enabled: " + this.enabled);
        return this.enabled? 1 : 0;
    }

    public List<? extends Object3D> getObjects() {
        return Collections.singletonList(lightBulb);
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
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {

        // check
        if (!enabled) return;

        // assert
        if (camera == null || light == null || lightBulb == null) {
            return;
        }

        // Use the new custom SUN shader for a beautiful glowing effect
        final Shader drawer = shaderFactory.getShader(ShaderResource.SUN);
        if (drawer == null) {
            logger.log(Level.SEVERE, "No sun drawer found");
            setEnabled(false);
            return;
        }

        // 1. Calculate Sun Position (Sync with Skybox)
        final Calendar calendar = Calendar.getInstance();
        final float dayProgress = (calendar.get(Calendar.HOUR_OF_DAY) * 3600f +
                             calendar.get(Calendar.MINUTE) * 60f +
                             calendar.get(Calendar.SECOND)) / 86400f;
        
        final float[] sunPos = getSunPosition(dayProgress);
        
        // 2. Update Light and Bulb location
        light.setLocation(sunPos);
        lightBulb.setLocation(sunPos);

        // 3. Draw the "Sun" Bulb
        if (light.isEnabled() && sunPos[1] > -0.1f) { // Only draw if sun is above horizon
            drawer.draw(lightBulb, camera.getProjectionMatrix(), camera.getViewMatrix(),
                    light.getLocation(), null, camera.getPos(), lightBulb.getDrawMode(), lightBulb.getDrawSize());
        }
    }

    /**
     * Calculates the Sun's 3D position based on day progress, 
     * matching the logic in shader_v3_skybox_frag.glsl.
     */
    private float[] getSunPosition(final float progress) {
        final float PI = (float) Math.PI;
        
        // Convert progress to angle so Noon (0.5) is peak (Zenith)
        final float angle = (progress * 2.0f * PI) - PI; 
        final float lat = 0.7f; // Spain Latitude (~40N)
        
        // Direction matching the updated skybox shader
        final float x = (float) Math.sin(angle);
        final float y = (float) (Math.cos(angle) * Math.cos(lat));
        final float z = (float) (Math.cos(angle) * Math.sin(lat));
        
        // Place it far away, just inside the skybox
        final float distance = Constants.SKYBOX_SIZE * 0.8f;
        return new float[]{-x * distance, y * distance, z * distance};
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}