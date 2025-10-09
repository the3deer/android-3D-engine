package org.the3deer.android_3d_model_engine.drawer;

import android.util.Log;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.objects.BoundingBox;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
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
    private Scene scene;
    @Inject
    private Camera camera;
    /**
     * Animator
     */
    private Animator animator = new Animator();

    // dynamic bounding boxes
    private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<>();

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
        if (!enabled || scene == null || camera == null) {
            // scene not ready
            return;
        }

        // draw all available objects
        final List<Object3DData> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData objData = objects.get(i);
            try {
                if (objData == scene.getSelectedObject() && objData.isRender()) {
                    Object3DData boundingBoxData = getBoundingBox(objData);

                    final Shader drawerObject = shaderFactory.getShader(R.raw.shader_basic_vert, R.raw.shader_basic_frag);
                    if (drawerObject == null) {
                        // Log.e(TAG, "No drawer for " + objData.getId());
                        return;
                    }

                    Object3DData selectedObject = scene.getSelectedObject();
                    if (selectedObject instanceof AnimatedModel) {
                        animator.update(((AnimatedModel) selectedObject).getRootJoint(), ((AnimatedModel) selectedObject).getCurrentAnimation(), boundingBoxData
                                , false);
                    }

                    final Camera camera = config != null && config.camera != null ? config.camera : this.camera;
                    drawerObject.draw(boundingBoxData, camera.getProjectionMatrix(), camera.getViewMatrix(),
                            null, null, null,
                            boundingBoxData.getDrawMode(), boundingBoxData.getDrawSize());
                }
            } catch (Exception ex) {
                Log.e(TAG, "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }

    private Object3DData getBoundingBox(Object3DData objData) {
        Object3DData boundingBoxData = boundingBoxes.get(objData);
        if (boundingBoxData == null) {
            Log.v(TAG, "Building bounding box... id: " + objData.getId());
            boundingBoxData = BoundingBox.build(objData);
            //boundingBoxData.setModelMatrix(objData.getModelMatrix());
            //boundingBoxData.setParentBound(true);
            boundingBoxData.setSolid(false);
            //boundingBoxData.setReadOnly(true);
            //boundingBoxData.setScale(objData.getScale());
            boundingBoxes.put(objData, boundingBoxData);
        }

        /*boundingBoxData.setColor(Constants.COLOR_GRAY);
        if (scene.getSelectedObject() == objData) {
            boundingBoxData.setColor(Constants.COLOR_WHITE);
        }*/
        return boundingBoxData;
    }
}
