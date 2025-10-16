package org.the3deer.android_3d_model_engine.scene;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.camera.CameraManager;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.model.Skeleton;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;

import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

public class SceneDrawer implements Drawer, EventListener {

    public static final String TAG = SceneDrawer.class.getSimpleName();

    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private CameraManager cameraManager;
    @Inject
    private Light light;
    @Inject
    private Projection projection;

    /**
     * Animator
     */
    private Animator animator = new Animator();

    private boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onEvent(EventObject event) {

        if (sceneManager == null) return false;

        final Scene scene = sceneManager.getCurrentScene();
        if (scene == null) return false;

        // back-to-front drawing
        if (event instanceof Camera.CameraUpdatedEvent) {
            final Camera camera = (Camera) event.getSource();
            final float[] pos = camera.getPos();
            final List<Object3DData> objects = scene.getObjects();
            for (int i=0; i<objects.size(); i++){
                Collections.sort(scene.getObjects(), (o1,o2)->{
                    final float[] d1 = Math3DUtils.substract(pos, o1.getLocation());
                    final float[] d2 = Math3DUtils.substract(pos, o2.getLocation());
                    return -(int) (Math3DUtils.length(d1) - Math3DUtils.length(d2));
                });
            }
        }
        return false;
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {

        if (!enabled) return;

        if (shaderFactory == null) return;

        if (sceneManager == null || cameraManager == null) return;

        final Scene scene = sceneManager.getCurrentScene();
        if (scene == null || scene.getObjects() == null) return;

        Camera camera = config != null ? config.camera : null;
        if (camera == null) camera = cameraManager.getActiveCamera();

        if (camera == null) {
            // Fallback if no camera is active for some reason
            Log.e(TAG, "No active camera found!");
            return;

        }

        // 0. GET OBJECTS
        List<Object3DData> objects = scene.getObjects();
        if (objects == null || objects.isEmpty()) return;

        // 1. UPDATE THE STATIC SCENE GRAPH
        // This sets the base pose for everything, including skeletons.
        if (scene.getRootNodes() != null && !scene.getRootNodes().isEmpty()) {
            for (int i=0; i<scene.getRootNodes().size(); i++) {
                // This method should recursively update all children
                scene.getRootNodes().get(i).updateBindWorldTransform(scene.getWorldMatrix());
            }
        }

        if (scene.getSkeletons() != null && !scene.getSkeletons().isEmpty()) {
            for (int i = 0; i < scene.getSkeletons().size(); i++) {
                Skeleton skeleton = scene.getSkeletons().get(i);
                animator.update(skeleton.getHeadJoint(), scene.getCurrentAnimation(),
                        scene.getWorldMatrix(), skeleton, false);
            }
        }

        /*for (int i = 0; i < objects.size(); i++) {
            Object3DData obj = objects.get(i);
            if (obj instanceof AnimatedModel) {
                animator.update(((AnimatedModel) obj).getRootJoint(), scene.getCurrentAnimation(), scene.getWorldMatrix(), obj, false);
            }
        }*/

        // 2. DRAW ALL OBJECTS
        for (int i = 0; i < objects.size(); i++) {
            //final Object3DData object3DData = objects.get(i);
                drawObject(camera, light.getLocation(), null, camera.getPos(),
                        true, light.isEnabled(), false, true, true, objects, i);
        }
        /*for (int i = 0; i < objects.size(); i++) {
            final Object3DData object3DData = objects.get(i);
            if (object3DData.getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE ||
                    object3DData.getMaterial().getAlpha() == 1.0f) {
                drawObject(camera, light.getLocation(), null, camera.getPos(),
                        true, light.isEnabled(), false, true, true, objects, i);
            }
        }
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData object3DData = objects.get(i);
            if (object3DData.getMaterial().getAlphaMode() != Material.AlphaMode.OPAQUE ||
                    object3DData.getMaterial().getAlpha() != 1.0f) {
                drawObject(camera, light.getLocation(), null, camera.getPos(),
                        true, light.isEnabled(), false, true, true, objects, i);
            }
        }*/
    }

    private void drawObject(Camera camera, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, boolean doAnimation, boolean drawLighting, boolean drawWireframe, boolean drawTextures, boolean drawColors, List<Object3DData> objects, int i) {
        Object3DData objData = null;
        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            // draw points
            if (objData.getDrawMode() == GLES20.GL_POINTS) {
                Shader basicDrawer = shaderFactory.getShader(R.raw.shader_basic_vert, R.raw.shader_basic_frag);
                basicDrawer.draw(objData, projection.getMatrix(), camera.getViewMatrix(),
                        light.getLocation(), colorMask,
                        camera.getPos(), objData.getDrawMode(), objData.getDrawSize());
            } else {
                if (objData.isRender()) {
                    final Shader shader= shaderFactory.getShader(R.raw.shader_animated_vert, R.raw.shader_animated_frag);

                    if (shader == null) {
                        Log.e(TAG, "Shader Factory returned no shader: " + shaderFactory);
                        setEnabled(false);
                        return;
                    } else {
                        //Log.i(TAG, "Drawing " + objData.getId());
                    }
                    shader.draw(objData, projection.getMatrix(), camera.getViewMatrix(),
                            light.getLocation(), colorMask, camera.getPos(),
                            objData.getDrawMode(), objData.getDrawSize());


                }
            }
        } catch (Exception ex) {
            Log.e("SceneDrawer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
            setEnabled(false);
        }
    }
}
