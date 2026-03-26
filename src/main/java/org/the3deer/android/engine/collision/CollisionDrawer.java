package org.the3deer.android.engine.collision;

import android.util.Log;

import org.the3deer.android.engine.model.Light;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.objects.Point;
import org.the3deer.android.engine.renderer.Drawer;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderFactory;
import org.the3deer.android.engine.shader.ShaderResource;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@Bean(name="Collision Points Drawer", description = "Draw points where the ray-object collisions take place")
public class CollisionDrawer implements Drawer, EventListener {

    public static final String TAG = CollisionDrawer.class.getSimpleName();

    @Inject
    private ShaderFactory shaderFactory;
    @Inject
    private Model sceneManager;
    @Inject
    private Light light;

    @BeanProperty
    private boolean enabled = true;

    /**
     * List of 3d points / collisions to draw
     */
    private final Map<String, List<Object3D>> collisions = new HashMap<>();

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

        // check
        if (!enabled) return false;

        // check
        if (!(event instanceof CollisionEvent)) return false;

        // get current scene
        final Scene scene = sceneManager.getActiveScene();

        // check
        if (scene == null) return false;

        // process event
        return onCollisionEvent(scene, (CollisionEvent) event);
    }

    /**
     * Adds a collision point to the list of collision points. Those point will be drawn later on
     *
     * @param event the collision event
     * @return true if the point was added, false otherwise
     */
    private boolean onCollisionEvent(Scene scene, CollisionEvent event) {

        // debug
        Log.v(TAG, "Processing collision... " + event);

        // get hit information
        //final Object3D hit = event.getObject();
        final float[] point = event.getPoint();

        // check
        if (point == null) {
            throw new IllegalArgumentException("point cannot be null");
        }

        // debug
        Log.d(TAG, "Adding collision point " + Arrays.toString(point));

        // create hit object
        final Object3D point3D = Point.build(point).setColor(new float[]{1.0f, 0f, 0f, 1f});
        //point3D.setParent(hit.getParent());
        //point3D.setParentNode(hit.getParentNode());

        // get scene points
        final List<Object3D> points = collisions.get(scene.getName());

        // initialize if needed
        if (points == null) {

            // register point
            return collisions.put(scene.getName(), new ArrayList<>(List.of(point3D))) == null;

        } else {

            // register point
            return points.add(point3D);
        }
    }

    @Override
    public void onDrawFrame(Config config) {

        // check
        if (!enabled || sceneManager == null || shaderFactory == null) return;

        // get current scene
        final Scene scene = sceneManager.getActiveScene();

        // check
        if (scene == null) return;

        // get collision points
        final List<Object3D> objects = collisions.get(scene.getName());

        // check
        if (objects == null || objects.isEmpty()) return;

        // debug
        if (!traced) {
            Log.v(TAG, "Drawing "+collisions.size()+" points... scene: "+scene.getName());
        }

        // Draw all objects
        for (int i = 0; i < objects.size(); i++) {
            drawCollisionPoint(config, objects, i);
        }
    }

    private void drawCollisionPoint(Config config, List<Object3D> objects, int i) {
        Object3D objData;

        objData = objects.get(i);
        if (!objData.isVisible() || !objData.isRender()) {
            return;
        }

        final Shader shader = shaderFactory.getShader(ShaderResource.BASIC);

        if (shader != null) {

            if (!traced) {
                Log.d(TAG, "Drawing collision point... id: " + objData.getId());
            }

            // Use the new high-performance draw call
            shader.draw(objData, config.camera.getProjection().getMatrix(), config.camera.getViewMatrix(),
                    light.getLocation(), null, config.camera.getPos(),
                    objData.getDrawMode(), objData.getDrawSize());

            // debug
            if (!traced) {
                Log.v(TAG, "Drawing "+collisions.size()+" points finished");
                traced = true;
            }
        }
    }
}
