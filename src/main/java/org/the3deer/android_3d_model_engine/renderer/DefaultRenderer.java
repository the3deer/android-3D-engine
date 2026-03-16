package org.the3deer.android_3d_model_engine.renderer;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.scene.SceneManager;
import org.the3deer.android_3d_model_engine.view.Renderer;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

public class DefaultRenderer implements Renderer, EventListener {

    private final static String TAG = DefaultRenderer.class.getSimpleName();

    private boolean enabled = true;

    @Inject
    private Activity activity;

    @Inject
    private SceneManager sceneManager;

    @Inject
    private List<Drawer> drawers;

    @Inject
    private Light light;

    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    /**
     * Animator
     */
    private final Animator animator = new Animator();

    private int width;
    // height of the screen
    private int height;

    /**
     * Construct a new renderer for the specified surface view
     */
    public DefaultRenderer() {
    }

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DefaultRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {

        // update screen size
        this.width = width;
        this.height = height;

        // call decorators
        for (int i = 0; i < drawers.size(); i++) {
            try {
                drawers.get(i).onSurfaceChanged(width, height);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                setEnabled(false);
            }
        }
    }

    @Override
    public void onPrepareFrame(){

        // check
        if (!enabled) return;

        // prepare frame
        prepareFrame(null);
    }

    @Override
    public void onDrawFrame() {

        // check
        if (!enabled) return;

        drawFrame(null);
    }

    protected void drawFrame(Drawer.Config config) {

        // Default viewport
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);

        // override viewport
        if (config != null){

            // configure viewport
            GLES20.glViewport(config.viewPortX, config.viewPortY, config.viewPortWidth, config.viewPortHeigth);
            GLES20.glScissor(config.viewPortX, config.viewPortY, config.viewPortWidth, config.viewPortHeigth);
        }

        // invoke all decorators
        for (int i = 0; i < drawers.size(); i++) {
            if (!drawers.get(i).isEnabled()) {
                continue;
            }


            try {
                drawers.get(i).onDrawFrame(config);
            } catch (Exception e) {

                // log the error
                Log.e(TAG, e.getMessage(), e);

                // disable drawer
                drawers.get(i).setEnabled(false);

                // notify user
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "There was a problem rendering the model: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }
    }

    protected void prepareFrame(Drawer.Config config) {

        // get current scene
        final Scene scene = sceneManager.getCurrentScene();

        // check
        if (scene == null || scene.getObjects() == null) return;

        //
        if (Constants.ANIMATIONS_ENABLED) {


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
                    scene.getRootNodes().get(i).updateAnimatedWorldTransform(Math3DUtils.IDENTITY_MATRIX);
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
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}
