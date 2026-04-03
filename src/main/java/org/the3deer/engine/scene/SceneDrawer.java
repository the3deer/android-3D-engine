package org.the3deer.engine.scene;

import android.util.Log;

import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Light;
import org.the3deer.engine.Model;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.renderer.Drawer;
import org.the3deer.engine.android.shader.Shader;
import org.the3deer.engine.android.shader.ShaderFactory;
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

        if (event instanceof Camera.CameraUpdatedEvent) {
            final Camera camera = (Camera) event.getSource();
            final float[] pos = camera.getPos();
            final List<Object3D> objects = scene.getObjects();
            Collections.sort(objects, (o1, o2) -> {
                final float[] d1 = Math3DUtils.substract(pos, o1.getLocation());
                final float[] d2 = Math3DUtils.substract(pos, o2.getLocation());
                return -(int) (Math3DUtils.length(d1) - Math3DUtils.length(d2));
            });
        }
        return false;
    }

    @Override
    public void onDrawFrame(Config config) {

        if (!enabled || sceneManager == null || shaderFactory == null) return;

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
            Log.v(TAG, "onDrawFrame...");
        }

        if (!traced){
            Log.d(TAG, "Drawing Scene with " + objects.size() + " objects");
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

        final Shader shader = shaderFactory.getShader(objData);

        if (shader != null) {

            if (!traced){
                Log.d(TAG, "Drawing object... id: " + objData.getId()+", vertices: "+objData+", drawMode: "+objData.getDrawMode());
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
