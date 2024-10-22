package org.the3deer.android_3d_model_engine.collision;

import android.util.Log;

import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.objects.Point;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;

import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

/**
 * Collision controller that, based on View settings (width, height and projection matrices)
 * it can detect a collision between an Object and a Ray casted from the screen to the farthest point
 *
 * Collision controller processes {@link TouchEvent} and fires {@link CollisionEvent}
 */
public class CollisionController implements EventListener {

    @Inject
    private Screen screen;
    @Inject
    private Camera camera;
    @Inject
    private Scene scene;
    @Inject
    private EventManager eventManager;

    public CollisionController() {
    }

    public List<Object3DData> getObjects() {
        return scene.getObjects();
    }

    @Override
    public boolean onEvent(EventObject event) {
        //Log.v("CollisionController", "Processing event... " + event.toString());
        if (event instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event;
            if (touchEvent.getAction() == TouchEvent.CLICK) {
                if (getObjects().isEmpty()) return true;
                //Log.v("CollisionController", getObjects().get(0).getCurrentDimensions().toString());
                final float x = touchEvent.getX();
                final float y = touchEvent.getY();
                Log.v("CollisionController", "Testing for collision... (" + getObjects().size() + ") " + x + "," + y);
                Object3DData objectHit = CollisionDetection.getBoxIntersection(
                        getObjects(), screen.getWidth(), screen.getHeight(),
                        camera.getViewMatrix(), camera.getProjectionMatrix(), x, y);
                if (objectHit != null) {

                    // intersection point
                    Object3DData point3D = null;

                    if (this.scene.isCollision()) {

                        Log.i("CollisionController", "Collision. Getting triangle intersection... " + objectHit.getId());
                        float[] point = CollisionDetection.getTriangleIntersection(objectHit, screen.getWidth(), screen.getHeight(),
                                camera.getViewMatrix(), camera.getProjectionMatrix(), x, y);

                        if (point != null) {
                            Log.i("CollisionController", "Building intersection point: " + Arrays.toString(point));
                            point3D = Point.build(point).setColor(new float[]{1.0f, 0f, 0f, 1f});
                            scene.addObject(point3D);
                        }
                    }

                    final CollisionEvent collisionEvent = new CollisionEvent(this, objectHit, x, y, point3D);
                    if (eventManager != null) {
                        eventManager.propagate(collisionEvent);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}