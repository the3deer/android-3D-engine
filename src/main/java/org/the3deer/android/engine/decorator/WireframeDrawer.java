package org.the3deer.android.engine.decorator;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android.engine.animation.Animator;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.objects.Wireframe;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@Bean
public class WireframeDrawer implements Drawer, EventListener {

    private final static String TAG = WireframeDrawer.class.getSimpleName();

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    @BeanProperty
    private boolean enabled;
    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private Model sceneManager;
    @Inject
    private Camera camera;
    // The wireframe associated shape (it should be made of lines only)
    private Map<String, Object3D> wireframes = new HashMap<>();

    public List<? extends Object3D> getObjects() {
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
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {

        // enabled?
        if (!enabled) return;

        // assert
        if (sceneManager == null) {
            return;
        }

        // current scene
        final Scene scene = sceneManager.getActiveScene();

        // check
        if (scene == null) return;

        // get objects
        final List<Object3D> objects = scene.getObjects();

        // draw objects
        for (int i = 0; i < objects.size(); i++) {
            final Camera camera = config != null && config.camera != null ? config.camera : scene.getActiveCamera();
            drawObject(camera, objects, i);
        }
    }

    private void drawObject(Camera camera, List<Object3D> objects, int i) {
        Object3D objData = null;
        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            // draw points
            // draw wireframe
            if (enabled && objData.getDrawMode() == GLES20.GL_TRIANGLES) {
                // Log.d("ModelRenderer","Drawing wireframe model...");
                try {
                    // Only draw wireframes for objects having faces (triangles)
                    Object3D wireframe = wireframes.get(objData.getId());
                    if (wireframe == null) {
                        Log.i("WireframeDrawer", "Building wireframe model... object: "+objData.getId());
                        wireframe = Wireframe.build(objData);
                        wireframes.put(objData.getId(), wireframe);
                        Log.i("WireframeDrawer", "Wireframe built: " + wireframe);
                    }

                    //Shader drawerObject = shaderFactory.getShader(scene, objData, false, false, false, true, false, false);
                    final Shader shader = shaderFactory.getShader(wireframe);
                    if (shader == null) {
                        Log.e(TAG, "No drawer for " + objData.getId());
                        return;
                    }

                    //animator.update(wireframe, scene.isShowBindPose());
                    //animator.update(scene.getRootNodes(), scene.getCurrentAnimation(), false);
                    shader.draw(wireframe, camera.getProjectionMatrix(), camera.getViewMatrix(), null, null, camera.getPos(), wireframe.getDrawMode(), wireframe.getDrawSize());
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