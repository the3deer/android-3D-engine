package org.the3deer.android_3d_model_engine.drawer;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

public class SceneRenderer implements Renderer, EventListener {

    private final static String TAG = SceneRenderer.class.getSimpleName();

    private boolean enabled = true;
    @Inject
    private Scene scene;
    @Inject
    private Light light;
    @Inject
    private List<EventListener> listeners = new ArrayList<>();
    // debug only once
    private boolean debug = true;

    /**
     * Construct a new renderer for the specified surface view
     */
    public SceneRenderer() {
    }

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setScene(Scene scene) {
        Log.v(TAG, "New scene. Objects: " + scene.getObjects().size());
        this.scene = scene;
    }

    public SceneRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public void onDrawFrame() {

        // check
        if (!enabled) return;

        // assert
        if (scene == null || scene.getCamera() == null) return;

        // draw scene
        // draw all available objects
        List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData object3DData = objects.get(i);
            if (object3DData.getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE) {
                drawObject(light.getLocation(), null, scene.getCamera().getPos(),
                        true, light.isEnabled(), false, true, true, objects, i);
            }
        }
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData object3DData = objects.get(i);
            if (object3DData.getMaterial().getAlphaMode() != Material.AlphaMode.OPAQUE) {
                drawObject(light.getLocation(), null, scene.getCamera().getPos(),
                        true, light.isEnabled(), false, true, true, objects, i);
            }
        }
    }

    private void drawObject(float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, boolean doAnimation, boolean drawLighting, boolean drawWireframe, boolean drawTextures, boolean drawColors, List<Object3DData> objects, int i) {
        Object3DData objData = null;
        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            Shader drawerObject = ShaderFactory.getInstance().getShader(objData, false, drawTextures, drawLighting, doAnimation, false, false);
            if (drawerObject == null) {
                // Log.e(TAG, "No drawer for " + objData.getId());
                return;
            }

            boolean changed = objData.isChanged();
            objData.setChanged(false);

            // draw points
            if (objData.getDrawMode() == GLES20.GL_POINTS) {
                Shader  basicDrawer = ShaderFactory.getInstance().getBasicShader();
                basicDrawer.draw(objData, scene.getCamera().getProjectionMatrix(), scene.getCamera().getViewMatrix(),
                        light.getLocation(), null,
                        scene.getCamera().getPos(), objData.getDrawMode(), objData.getDrawSize());
            } else {
                if (objData.isRender()) {
                    drawerObject.draw(objData, scene.getCamera().getProjectionMatrix(), scene.getCamera().getViewMatrix(),
                            light.getLocation(), colorMask, scene.getCamera().getPos(),
                            objData.getDrawMode(), objData.getDrawSize());


                }
            }
        } catch (Exception ex) {
            if (debug) {
                Log.e(TAG, "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
                debug = false;
            }
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}