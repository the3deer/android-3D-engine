package org.the3deer.android_3d_model_engine.scene;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.renderer.Drawer;
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
    private Light light;
    @Inject
    private Camera defaultCamera;

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

        if (event instanceof Camera.CameraUpdatedEvent) {
            final Camera camera = (Camera) event.getSource();
            final float[] pos = camera.getPos();
            final List<Object3DData> objects = scene.getObjects();
            Collections.sort(objects, (o1, o2) -> {
                final float[] d1 = Math3DUtils.substract(pos, o1.getLocation());
                final float[] d2 = Math3DUtils.substract(pos, o2.getLocation());
                return -(int) (Math3DUtils.length(d1) - Math3DUtils.length(d2));
            });
        }
        return false;
    }

    @Override
    public void onDrawFrame() {
        this.onDrawFrame(null);
    }

    @Override
    public void onDrawFrame(Config config) {
        if (!enabled || sceneManager == null || shaderFactory == null) return;

        final Scene scene = sceneManager.getCurrentScene();
        if (scene == null || scene.getObjects() == null) return;

        // camera selection logic:
        // 1) use config camera if provided (when stereo rendering, for example)
        Camera camera = config != null ? config.camera : null;
        //if (camera == null) camera = cameraManager.getActiveCamera();
        if (camera == null){
            // 2) else use scene camera if set
            camera = scene.getCamera();
            if (camera == null){
                // 3) else use default engine camera
                camera = defaultCamera;
                if (camera == null) {
                    // No-op if no camera is active for some reason
                    Log.e(TAG, "No active camera found!");
                    return;
                }
            }
        }

        List<Object3DData> objects = scene.getObjects();
        if (objects == null || objects.isEmpty()) return;

        // Draw all objects
        for (int i = 0; i < objects.size(); i++) {
            drawObject(camera, light.getLocation(), null, camera.getPos(), objects, i);
        }
    }

    private void drawObject(Camera camera, float[] lightPos, float[] colorMask, float[] cameraPos, List<Object3DData> objects, int i) {
        Object3DData objData = null;

        objData = objects.get(i);
        if (!objData.isVisible() || !objData.isRender()) {
            return;
        }

        final Shader shader = shaderFactory.getShader(objData);

        if (shader != null) {
            // Use the new high-performance draw call
            shader.draw(objData, camera.getProjection().getMatrix(), camera.getViewMatrix(),
                    light.getLocation(), colorMask, camera.getPos(),
                    objData.getDrawMode(), objData.getDrawSize());
        }
    }
}
