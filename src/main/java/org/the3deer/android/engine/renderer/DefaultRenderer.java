package org.the3deer.android.engine.renderer;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android.engine.animation.Animator;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Light;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.model.Skin;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

@Bean
public class DefaultRenderer implements Renderer, EventListener {

    private final static String TAG = DefaultRenderer.class.getSimpleName();

    protected boolean enabled = true;

    private boolean traced;

    @Inject
    private Activity activity;

    @Inject
    private Model sceneManager;

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
    /**
     * width if the screen. with default value just in case the onSurfaceChanged is not called
     */
    @Inject
    private Screen screen;

    @Inject
    private Camera defaultCamera;

    /**
     * Default drawer configuration
     */
    private final Drawer.Config defaultConfig = new Drawer.Config();

    /**
     * Construct a new renderer for the specified surface view
     */
    public DefaultRenderer() {
    }

    public List<? extends Object3D> getObjects() {
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
    public void reset() {
        this.traced = false;
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
        GLES20.glViewport(0, 0, screen.width, screen.height);
        GLES20.glScissor(0, 0, screen.width, screen.height);

        // override viewport
        if (config != null){

            // configure viewport
            GLES20.glViewport(config.viewPortX, config.viewPortY, config.viewPortWidth, config.viewPortHeigth);
            GLES20.glScissor(config.viewPortX, config.viewPortY, config.viewPortWidth, config.viewPortHeigth);
        }

        // debug
        if (!traced){
            Log.d(TAG, "onDrawFrame start... " + drawers);
        }

        // default config
        if (config == null){
            config = this.defaultConfig;
        }

        // check
        if (config.camera == null){
            config.camera = defaultCamera;
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

        if (!traced){
            Log.d(TAG, "onDrawFrame finished");
            traced = true;
        }
    }

    protected void prepareFrame(Drawer.Config config) {

        // check
        if (sceneManager == null) return;

        // get current scene
        final Scene scene = sceneManager.getActiveScene();

        // check
        if (scene == null) return;

        // process animations (hard-coded debug flag - always true in Production)
        if (Constants.ANIMATIONS_ENABLED) {


            // 1. ANIMATION PHASE: UPDATE ALL NODE TRANSFORMS
            // This single call should handle both node-based and skinned animations.
            // It will update the local transforms of all nodes affected by the animation.
            if (scene.getActiveAnimation() != null) {
                animator.update(scene.getRootNodes(), scene.getActiveAnimation(), false);
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
            scene.getSkins();
            if (!scene.getSkins().isEmpty()) {
                for (int i = 0; i < scene.getSkins().size(); i++) {
                    Skin skin = scene.getSkins().get(i);
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
