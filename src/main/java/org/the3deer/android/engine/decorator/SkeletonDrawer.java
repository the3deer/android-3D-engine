package org.the3deer.android.engine.decorator;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;

import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanOrder;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.animation.Animator;
import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.objects.Skeleton;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.Program;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

@BeanOrder(order = 100)
@Bean
public class SkeletonDrawer implements Drawer {

    private static final Logger logger = Logger.getLogger(SkeletonDrawer.class.getSimpleName());

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    @BeanProperty
    private boolean enabled;
    @Inject
    private ShaderManager shaderFactory;
    @Inject
    private Model sceneManager;
    @Inject
    private Camera camera;
    // The skeleton associated
    private final Map<String, AnimatedModel> skeleton = new HashMap<>();

    public List<? extends Object3D> getObjects() {
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
        logger.info("Toggled skeleton. enabled: " + this.enabled);
        return this.enabled ? 1 : 0;
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    public void onDrawFrame(Camera camera, Object3D object) {

    }

    @Override
    public void onDrawFrame(Renderer.Config config) {

        // check
        if (!enabled) return;

        // check
        if (sceneManager == null) {
            return;
        }

        // get scene
        final Scene scene = sceneManager.getActiveScene();
        if (scene == null) return;

        // we need to write on top of everything
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // draw
        List<Object3D> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            Object3D objData = null;
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
                    logger.config("Building skeleton... object: "+objData.getId());

                    // build
                    skeletonModel = Skeleton.build((AnimatedModel) objData);

                    // debug
                    logger.config("Skeleton built: "+skeletonModel);

                    // register
                    this.skeleton.put(objData.getId(), skeletonModel);
                }

                // get shader
                //Shader drawerObject = shaderFactory.getShader(scene, objData, false, false, false, true, false, false);
                final Shader shader = shaderFactory.getShader(Program.ANIMATED);
                if (shader == null) {
                    logger.log(Level.SEVERE, "No drawer for " + objData.getId());
                    continue;
                }

                final Camera camera = config != null && config.camera != null ? config.camera : scene.getActiveCamera();
                shader.draw(skeletonModel, camera.getProjectionMatrix(), camera.getViewMatrix(), null, null, camera.getPos(), skeletonModel.getDrawMode(), skeletonModel.getDrawSize());

            } catch (Exception ex) {
                this.enabled = false;
                logger.log(Level.SEVERE, "There was a problem rendering the skeleton '" + objData.getId() + "':" + ex.getMessage(), ex);
            }
        }
    }
}