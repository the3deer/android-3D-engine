package org.the3deer.android_3d_model_engine.drawer;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class LightBulbRenderer implements Renderer, EventListener {

    private final static String TAG = LightBulbRenderer.class.getSimpleName();

    private boolean enabled = true;
    @Inject
    private Camera camera;
    @Inject
    private Light light;
    @Inject @Named("lightBulb")
    private Object3DData lightBulb;

    private final float[] tempVector4 = new float[4];
    private final float[] lightPosInWorldSpace = new float[4];

    public int toggle(){
        this.enabled = !this.enabled;
        Log.i("LightBulbDrawer", "Toggled wireframe. enabled: " + this.enabled);
        return this.enabled? 1 : 0;
    }

    public List<? extends Object3DData> getObjects() {
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

        // check
        if (!enabled) return;

        // assert
        if (camera == null || light == null || lightBulb == null) {
            return;
        }

        Shader drawer = ShaderFactory.getInstance().getShader(lightBulb, false, false, false, false, false, false);
        if (drawer == null) {
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