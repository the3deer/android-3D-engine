package org.the3deer.android.engine.scene;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Light;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.Program;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.util.event.EventListener;

import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class SceneDrawer implements Drawer, EventListener {

    private static final Logger logger = Logger.getLogger(SceneDrawer.class.getSimpleName());

    @Inject
    private ShaderManager shaderManager;
    @Inject
    private Model sceneManager;
    @Inject
    private Light light;
    @Inject
    private Camera defaultCamera;

    private boolean enabled = true;

    private boolean traced;

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
        final Scene scene = sceneManager.getActiveScene();
        if (scene == null) return false;

        return false;
    }

    @Override
    public void onDrawFrame(Renderer.Config config) {

        if (!enabled || sceneManager == null || shaderManager == null) return;

        final Scene scene = sceneManager.getActiveScene();
        if (scene == null) return;

        // camera selection logic:
        // 1) use config camera if provided (when stereo rendering, for example)
        Camera activeCamera = scene.getActiveCamera();
        //if (camera == null) camera = cameraManager.getActiveCamera();
        if (activeCamera != null) {
            config.camera = activeCamera;
        }

        List<Object3D> objects = scene.getObjects();
        if (objects.isEmpty()) return;

        // debug
        if (!traced){
           logger.finest("onDrawFrame...");
        }

        if (!traced){
            logger.config("Drawing Scene with " + objects.size() + " objects");
        }

        // Draw all objects
        for (int i = 0; i < objects.size(); i++) {
            traced = drawObject(config.camera, light.getLocation(), null, config.camera.getPos(), objects, i);
        }
    }

    private boolean drawObject(Camera camera, float[] lightPos, float[] colorMask, float[] cameraPos, List<Object3D> objects, int i) {
        Object3D objData = null;

        objData = objects.get(i);
        if (!objData.isVisible() || !objData.isRender()) {
            return false;
        }

        // Determine the appropriate program based on model features
        boolean isAnimated = objData instanceof AnimatedModel && ((AnimatedModel) objData).getSkin() != null;
        boolean isPoints = objData.getDrawMode() == android.opengl.GLES20.GL_POINTS;

        final int programId;
        if (Constants.ANIMATIONS_ENABLED && isAnimated) {
            programId = Program.ANIMATED;
        } else if (Constants.LIGHTING_ENABLED && !isPoints) {
            programId = Program.STATIC;
        } else {
            programId = Program.BASIC;
        }

        final Shader shader = shaderManager.getShader(programId);

        if (shader != null) {

            if (!traced){
                logger.config("Drawing object... id: " + objData.getId()+", vertices: "+objData+", drawMode: "+objData.getDrawMode());
                traced = true;
            }

            // Use the new high-performance draw call
            shader.draw(objData, camera.getProjection().getMatrix(), camera.getViewMatrix(),
                    light.getLocation(), colorMask, camera.getPos(),
                    objData.getDrawMode(), objData.getDrawSize());

            return true;
        }

        return false;
    }
}
