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
import java.util.Collections;
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

    private boolean enabled;

    public CollisionController() {
    }

    public List<Object3DData> getObjects() {
        if (scene != null) {
            return scene.getObjects();
        }
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
                    Log.v("CollisionController", "Collision. Getting triangle intersection... " + objectHit.getId());
                    float[] point3D = CollisionDetection.getTriangleIntersection(objectHit, screen.getWidth(), screen.getHeight(),
                            camera.getViewMatrix(), camera.getProjectionMatrix(), x, y);

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