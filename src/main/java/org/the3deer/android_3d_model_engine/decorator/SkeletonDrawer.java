package org.the3deer.android_3d_model_engine.decorator;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.Skeleton;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.bean.BeanOrder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@BeanOrder(order = 100)
public class SkeletonDrawer implements Drawer {

    private final static String TAG = SkeletonDrawer.class.getSimpleName();

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    private boolean enabled = true;
    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private Camera camera;
    // The skeleton associated
    private final Map<String, AnimatedModel> skeleton = new HashMap<>();

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int toggle() {
        this.enabled = !this.enabled;
        Log.i(TAG, "Toggled skeleton. enabled: " + this.enabled);
        return this.enabled ? 1 : 0;
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    public void onDrawFrame(Camera camera, Object3DData object) {

    }

    @Override
    public void onDrawFrame(Config config) {

        // check
        if (!enabled) return;

        // check
        if (sceneManager == null) {
            return;
        }

        // get scene
        final Scene scene = sceneManager.getCurrentScene();
        if (scene == null) return;

        // we need to write on top of everything
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // draw
        List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            Object3DData objData = null;
            try {
                objData = objects.get(i);
                if (!objData.isVisible()) {
                    continue;
                }

                // check
                if (!(objData instanceof AnimatedModel)) continue;

                // check
                if (objData.getDrawMode() != GLES20.GL_TRIANGLES) continue;

                // draw skeleton on top of it
                // GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                AnimatedModel skeletonModel = this.skeleton.get(objData.getId());
                if (skeletonModel == null) {

                    // check
                    if (((AnimatedModel) objData).getSkin() == null) continue;
                    if (((AnimatedModel) objData).getSkin().getRootJoint() == null) continue;
                    if (((AnimatedModel) objData).getSkin().getJointCount() == 0) continue;

                    // debug
                    Log.d(TAG,"Building skeleton... object: "+objData.getId());

                    // build
                    skeletonModel = Skeleton.build((AnimatedModel) objData);

                    // debug
                    Log.d(TAG,"Skeleton built: "+skeletonModel);

                    // register
                    this.skeleton.put(objData.getId(), skeletonModel);
                }

                // get shader
                //Shader drawerObject = shaderFactory.getShader(scene, objData, false, false, false, true, false, false);
                final Shader shader = shaderFactory.getShader(skeletonModel);
                if (shader == null) {
                    Log.e(TAG, "No drawer for " + objData.getId());
                    continue;
                }

                final Camera camera = config != null && config.camera != null ? config.camera : scene.getCamera();
                shader.draw(skeletonModel, camera.getProjectionMatrix(), camera.getViewMatrix(), null, null, camera.getPos(), skeletonModel.getDrawMode(), skeletonModel.getDrawSize());

            } catch (Exception ex) {
                this.enabled = false;
                Log.e(TAG, "There was a problem rendering the skeleton '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }
}