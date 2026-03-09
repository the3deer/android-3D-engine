package org.the3deer.android_3d_model_engine.decorator;

import android.util.Log;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

public class LightBulbDrawer implements Drawer, EventListener {

    private final static String TAG = LightBulbDrawer.class.getSimpleName();


    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private Camera camera;
    @Inject
    private Light light;
    @Inject @Named("lightBulb")
    private Object3DData lightBulb;

    private final float[] tempVector4 = new float[4];
    private final float[] lightPosInWorldSpace = new float[4];

    private boolean enabled;

    public int toggle(){
        this.enabled = !this.enabled;
        Log.i(TAG, "Toggled wireframe. enabled: " + this.enabled);
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

        Shader drawer = shaderFactory.getShader(R.raw.shader_basic_vert, R.raw.shader_basic_frag);
        if (drawer == null) {
            Log.e(TAG, "No drawer");
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