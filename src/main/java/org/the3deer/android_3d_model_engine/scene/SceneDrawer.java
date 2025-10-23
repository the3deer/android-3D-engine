package org.the3deer.android_3d_model_engine.scene;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.camera.CameraManager;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;

import java.util.Arrays;
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
            for (int i = 0; i < objects.size(); i++) {
                Collections.sort(scene.getObjects(), (o1, o2) -> {
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

        // 1. ANIMATION PHASE: UPDATE ALL NODE TRANSFORMS
        // This single call should handle both node-based and skinned animations.
        // It will update the local transforms of all nodes affected by the animation.
        if (scene.getCurrentAnimation() != null) {
            animator.update(scene.getRootNodes(), scene.getCurrentAnimation(), false);
        }


        // 2. FINAL WORLD TRANSFORM CALCULATION (including Z_UP)
        // This is the step that makes Z_UP work. It bakes the animation and the static
        // hierarchy together into a final world transform for every node.
        if (scene.getRootNodes() != null && !scene.getRootNodes().isEmpty()) {
            for (int i = 0; i < scene.getRootNodes().size(); i++) {
                // This method should now recursively calculate the *animated* world transform
                scene.getRootNodes().get(i).updateAnimatedWorldTransform(scene.getWorldMatrix());
            }
        }

        // 3. SKINNING PHASE: UPDATE ALL SKELETON MATRICES
        // Now that all nodes have their final world transforms, we can compute the skinning matrices.
        if (scene.getSkeletons() != null && !scene.getSkeletons().isEmpty()) {
            for (int i = 0; i < scene.getSkeletons().size(); i++) {
                Skin skin = scene.getSkeletons().get(i);
                // This method now loops through the skeleton's joints and calculates the final skinning matrix
                // using the now-correct animatedWorldTransform of each joint node.
                skin.updateSkinMatrices();
            }
        }

        // 4. DRAW ALL OBJECTS
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
                    final Shader shader = shaderFactory.getShader(R.raw.shader_animated_vert, R.raw.shader_animated_frag);

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

    private void rescale(List<Object3DData> datas, float newScale, float[] newPosition) {

        //if (true) return;

        // check we have objects to scale, otherwise, there should be an issue with LoaderTask
        if (datas == null || datas.isEmpty()) {
            return;
        }

        Log.v(TAG, "Scaling datas... total: " + datas.size());
        // calculate the global max length
        final Object3DData firstObject = datas.get(0);
        final Dimensions currentDimensions = firstObject.getCurrentDimensions();
        Log.v(TAG, "Model[0] dimension: " + currentDimensions.toString());

        final float[] corner01 = currentDimensions.getCornerLeftTopNearVector();
        ;
        final float[] corner02 = currentDimensions.getCornerRightBottomFar();
        final float[] center01 = currentDimensions.getCenter();

        float maxLeft = corner01[0];
        float maxTop = corner01[1];
        float maxNear = corner01[2];
        float maxRight = corner02[0];
        float maxBottom = corner02[1];
        float maxFar = corner02[2];
        float maxCenterX = center01[0];
        float maxCenterY = center01[1];
        float maxCenterZ = center01[2];

        for (int i = 1; i < datas.size(); i++) {

            final Object3DData obj = datas.get(i);
            final Dimensions original = obj.getCurrentDimensions();

            Log.v(TAG, "Model[" + i + "] '" + obj.getId() + "' dimension: " + original.toString());
            final float[] corner1 = original.getCornerLeftTopNearVector();
            final float[] corner2 = original.getCornerRightBottomFar();
            final float[] center = original.getCenter();
            float maxLeft2 = corner1[0];
            float maxTop2 = corner1[1];
            float maxNear2 = corner1[2];
            float maxRight2 = corner2[0];
            float maxBottom2 = corner2[1];
            float maxFar2 = corner2[2];
            float centerX = center[0];
            float centerY = center[1];
            float centerZ = center[2];

            if (maxRight2 > maxRight) maxRight = maxRight2;
            if (maxLeft2 < maxLeft) maxLeft = maxLeft2;
            if (maxTop2 > maxTop) maxTop = maxTop2;
            if (maxBottom2 < maxBottom) maxBottom = maxBottom2;
            if (maxNear2 > maxNear) maxNear = maxNear2;
            if (maxFar2 < maxFar) maxFar = maxFar2;
            if (maxCenterX < centerX) maxCenterX = centerX;
            if (maxCenterY < centerY) maxCenterY = centerY;
            if (maxCenterZ < centerZ) maxCenterZ = centerZ;
        }
        float lengthX = maxRight - maxLeft;
        float lengthY = maxTop - maxBottom;
        float lengthZ = maxNear - maxFar;

        float maxLength = lengthX;
        if (lengthY > maxLength) maxLength = lengthY;
        if (lengthZ > maxLength) maxLength = lengthZ;
        Log.v(TAG, "Max length: " + maxLength);

        float maxLocation = 0;
        if (datas.size() > 1) {
            maxLocation = maxCenterX;
            if (maxCenterY > maxLocation) maxLocation = maxCenterY;
            if (maxCenterZ > maxLocation) maxLocation = maxCenterZ;
        }
        //Log.v(TAG, "Max location: " + maxLocation);

        // calculate the scale factor
        float scaleFactor = newScale / (maxLength + maxLocation);
        Log.v(TAG, "scale factor: " + scaleFactor);

        // calculate the global center
        float centerX = (maxRight + maxLeft) / 2;
        float centerY = (maxTop + maxBottom) / 2;
        float centerZ = (maxNear + maxFar) / 2;
        //Log.v(TAG, "Total center: " + centerX + "," + centerY + "," + centerZ);

        // calculate the new location
        float translationX = -centerX + newPosition[0];
        float translationY = -centerY + newPosition[1];
        float translationZ = -centerZ + newPosition[2];
        final float[] globalDifference = new float[]{translationX * scaleFactor, translationY * scaleFactor, translationZ * scaleFactor};
        Log.v(TAG, "Translation delta: " + Arrays.toString(globalDifference));
    }
}
