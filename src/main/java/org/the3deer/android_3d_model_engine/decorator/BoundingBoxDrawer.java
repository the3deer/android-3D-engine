package org.the3deer.android_3d_model_engine.decorator;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.BoundingBox;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class BoundingBoxDrawer implements Drawer {

    private final static String TAG = BoundingBoxDrawer.class.getSimpleName();

    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private SceneManager sceneManager;

    // dynamic bounding boxes
    private Map<String, Object3DData> boundingBoxes = new HashMap<>();

    private boolean enabled = true;

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
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
        if (!enabled || sceneManager == null) {
            // scene not ready
            return;
        }

        // check
        final Scene scene = sceneManager.getCurrentScene();
        if (scene == null) {
            // scene not ready
            return;
        }

        // draw all available objects
        final List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData objData = objects.get(i);
            try {
                if (objData == scene.getSelectedObject() && objData.isRender()) {

                    final Object3DData boundingBoxData = getBoundingBox(objData);
                    if (boundingBoxData == null) return;

                    if (boundingBoxData instanceof AnimatedModel){
                        ((AnimatedModel) boundingBoxData).getSkin().updateSkinMatrices();
                    }

                    final Shader drawerObject = shaderFactory.getShader(boundingBoxData);
                    //final Shader drawerObject = shaderFactory.getShader(R.raw.shader_basic_vert, R.raw.shader_basic_frag);
                    if (drawerObject == null) {
                        Log.e(TAG, "No drawer for " + objData.getId());
                        return;
                    }


                    Object3DData selectedObject = scene.getSelectedObject();
                    /*if (selectedObject instanceof AnimatedModel) {
                        animator.update(((AnimatedModel) selectedObject).getRootJoint(), ((AnimatedModel) selectedObject).getCurrentAnimation(), scene.getWorldMatrix(), boundingBoxData
                                , false);
                    }*/

                    final Camera camera = config != null && config.camera != null ? config.camera : scene.getCamera();
                    drawerObject.draw(boundingBoxData, camera.getProjectionMatrix(), camera.getViewMatrix(),
                            null, null, null,
                            boundingBoxData.getDrawMode(), boundingBoxData.getDrawSize());
                }
            } catch (Exception ex) {
                this.enabled = false;
                Log.e(TAG, "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }

    private Object3DData getBoundingBox(Object3DData objData) {
        Object3DData boundingBoxData = boundingBoxes.get(objData.getId());
        if (boundingBoxData == null) {
            if (Constants.STRATEGY_BBOX_NEW && objData instanceof AnimatedModel && ((AnimatedModel) objData).getSkin() != null){
                boundingBoxData = BoundingBox.buildSkinned((AnimatedModel) objData);
            } else {
                boundingBoxData = BoundingBox.buildStatic(objData);
            }
            if (boundingBoxData != null) {
                //boundingBoxData.setModelMatrix(objData.getModelMatrix());
                //boundingBoxData.setParentBound(true);
                boundingBoxData.setDecorator(false);
                //boundingBoxData.setReadOnly(true);
                //boundingBoxData.setScale(objData.getScale());
                boundingBoxes.put(objData.getId(), boundingBoxData);
            }
        }

        /*boundingBoxData.setColor(Constants.COLOR_GRAY);
        if (scene.getSelectedObject() == objData) {
            boundingBoxData.setColor(Constants.COLOR_WHITE);
        }*/
        return boundingBoxData;
    }
}
