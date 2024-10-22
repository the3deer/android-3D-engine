package org.the3deer.android_3d_model_engine.drawer;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.Wireframe;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class WireframeRenderer implements Renderer, EventListener {

    private final static String TAG = WireframeRenderer.class.getSimpleName();

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    private boolean enabled = false;
    @Inject
    private Scene scene;
    @Inject
    private Camera camera;
    // The wireframe associated shape (it should be made of lines only)
    private Map<String, Object3DData> wireframes = new HashMap<>();

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int toggle(){
        this.enabled = !this.enabled;
        Log.i("WireframeDrawer", "Toggled wireframe. enabled: " + this.enabled);
        return this.enabled? 1 : 0;
    }

    @Override
    public void onDrawFrame() {

        // enabled?
        if (!enabled) return;

        // assert
        if (scene == null || camera == null) {
            return;
        }

        // draw
        List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            drawObject(objects, i);
        }
    }

    private void drawObject(List<Object3DData> objects, int i) {
        Object3DData objData = null;
        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            Shader drawerObject = ShaderFactory.getInstance().getShader(objData, false, false, false, true, false, false);
            if (drawerObject == null) {
                Log.e(TAG, "No drawer for " + objData.getId());
                return;
            }

            //boolean changed = objData.isChanged();
            //objData.setChanged(false);

            // draw points
            // draw wireframe
            if (enabled && objData.getDrawMode() == GLES20.GL_TRIANGLES) {
                // Log.d("ModelRenderer","Drawing wireframe model...");
                try {
                    // Only draw wireframes for objects having faces (triangles)
                    Object3DData wireframe = wireframes.get(objData.getId());
                    if (wireframe == null) {
                        Log.i("WireframeDrawer", "Building wireframe model...");
                        wireframe = Wireframe.build(objData);
                        wireframes.put(objData.getId(), wireframe);
                        Log.i("WireframeDrawer", "Wireframe built: " + wireframe);
                    }
                    //animator.update(wireframe, scene.isShowBindPose());
                    drawerObject.draw(wireframe, camera.projectionMatrix, camera.viewMatrix, null, null, camera.getPos(), wireframe.getDrawMode(), wireframe.getDrawSize());
                } catch (Error e) {
                    Log.e("WireframeDrawer", e.getMessage(), e);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}