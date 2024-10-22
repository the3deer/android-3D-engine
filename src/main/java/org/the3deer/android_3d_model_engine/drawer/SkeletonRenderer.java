package org.the3deer.android_3d_model_engine.drawer;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.Skeleton;
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

public class SkeletonRenderer implements Renderer, EventListener {

    private final static String TAG = SkeletonRenderer.class.getSimpleName();

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    private boolean enabled = false;
    @Inject
    private Scene scene;
    @Inject
    private Camera camera;
    // The skeleton associated
    private Map<Object3DData, Object3DData> skeleton = new HashMap<>();

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
        Log.i(TAG, "Toggled skeleton. enabled: " + this.enabled);
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

            // check
            if (!(objData instanceof AnimatedModel)) return;

            // check
            if (objData.getDrawMode() != GLES20.GL_TRIANGLES) return;

            // check
            if (((AnimatedModel) objData).getSkeleton() == null) return;
            if (((AnimatedModel) objData).getSkeleton().getHeadJoint() == null) return;

            // get shader
            Shader drawerObject = ShaderFactory.getInstance().getShader(objData, false, false, false, true, false, false);
            if (drawerObject == null) {
                Log.e(TAG, "No drawer for " + objData.getId());
                return;
            }

            // Log.d("ModelRenderer","Drawing wireframe model...");
            try {
                // draw skeleton on top of it
                // GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                Object3DData skeletonModel = this.skeleton.get(objData);
                if (skeletonModel == null) {
                    skeletonModel = Skeleton.build((AnimatedModel) objData);
                    this.skeleton.put(objData, skeletonModel);
                }
                //GLES20.glEnable(GLES20.GL_DEPTH_TEST);

                drawerObject.draw(skeletonModel, camera.projectionMatrix, camera.viewMatrix, Constants.COLOR_BLUE, null, camera.getPos(), skeletonModel.getDrawMode(), skeletonModel.getDrawSize());

            } catch (Error e) {
                Log.e("WireframeDrawer", e.getMessage(), e);
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