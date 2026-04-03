package org.the3deer.engine.decorator;

import org.the3deer.engine.android.shader.Shader;
import org.the3deer.engine.android.shader.ShaderFactory;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Light;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.renderer.Drawer;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventListener;

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

    private final float[] tempVector4 = new float[4];
    private final float[] lightPosInWorldSpace = new float[4];

    @BeanProperty
    private boolean enabled;

    public int toggle(){
        this.enabled = !this.enabled;
        logger.info("Toggled wireframe. enabled: " + this.enabled);
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
        this.enabled =enabled;
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

        Shader drawer = shaderFactory.getShader(lightBulb);
        if (drawer == null) {
            logger.log(Level.SEVERE, "No drawer");
            setEnabled(false);
            return;
        }

        // light enabled?
        if (light.isEnabled()) {
            lightBulb.setLocation(light.getLocation());
            drawer.draw(lightBulb, camera.getProjectionMatrix(), camera.getViewMatrix(),
                    light.getLocation(), null, camera.getPos(), lightBulb.getDrawMode(), lightBulb.getDrawSize());
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}
