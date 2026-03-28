package org.the3deer.android.engine.decorator;

import android.util.Log;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.objects.BoundingBox;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@Bean
public class BoundingBoxDrawer implements Drawer {

    private final static String TAG = BoundingBoxDrawer.class.getSimpleName();

    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private Model sceneManager;

    // dynamic bounding boxes
    private Map<String, Object3D> boundingBoxes = new HashMap<>();

    @BeanProperty
    private boolean enabled = true;

    public List<? extends Object3D> getObjects() {
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
        final Scene scene = sceneManager.getActiveScene();
        if (scene == null) {
            // scene not ready
            return;
        }

        // draw all available objects
        final List<Object3D> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3D objData = objects.get(i);
            try {
                if (objData == scene.getSelectedObject() && objData.isRender()) {

                    final Object3D boundingBoxData = getBoundingBox(objData);
                    if (boundingBoxData == null) return;

                    if (boundingBoxData instanceof AnimatedModel){
                        ((AnimatedModel) boundingBoxData).getSkin().updateSkinMatrices();
                    }

                    final Shader shader = shaderFactory.getShader(boundingBoxData);
                    //final Shader drawerObject = shaderFactory.getShader(R.raw.shader_basic_vert, R.raw.shader_basic_frag);
                    if (shader == null) {
                        Log.e(TAG, "No drawer for " + objData.getId());
                        return;
                    }


                    Object3D selectedObject = scene.getSelectedObject();
                    /*if (selectedObject instanceof AnimatedModel) {
                        animator.update(((AnimatedModel) selectedObject).getRootJoint(), ((AnimatedModel) selectedObject).getCurrentAnimation(), scene.getWorldMatrix(), boundingBoxData
                                , false);
                    }*/

                    final Camera camera = config != null && config.camera != null ? config.camera : scene.getActiveCamera();
                    shader.draw(boundingBoxData, camera.getProjectionMatrix(), camera.getViewMatrix(),
                            null, null, null,
                            boundingBoxData.getDrawMode(), boundingBoxData.getDrawSize());
                }
            } catch (Exception ex) {
                this.enabled = false;
                Log.e(TAG, "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }

    private Object3D getBoundingBox(Object3D objData) {
        Object3D boundingBoxData = boundingBoxes.get(objData.getId());
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
