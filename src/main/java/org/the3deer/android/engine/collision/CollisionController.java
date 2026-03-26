package org.the3deer.android.engine.collision;

import android.util.Log;

import org.the3deer.android.engine.controller.TouchEvent;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;

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
    private Model sceneManager;
    @Inject
    private EventManager eventManager;

    private boolean enabled = true;

    public CollisionController() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean onEvent(EventObject event) {

        // process event
        //Log.v("CollisionController", "Processing event... " + event.toString());
        if (event instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event;
            if (touchEvent.getAction() == TouchEvent.CLICK) {

                // check
                if (!enabled || sceneManager == null) return false;

                // get scene
                final Scene scene = sceneManager.getActiveScene();
                if (scene == null) return false;

                // get objects
                final List<Object3D> objects = scene.getObjects();
                if (objects == null || objects.isEmpty()) return false;

                //Log.v("CollisionController", getObjects().get(0).getCurrentDimensions().toString());
                final float x = touchEvent.getX();
                final float y = touchEvent.getY();

                // check collision
                Object3D objectHit = CollisionDetection.getBoxIntersection(
                        objects, screen.getWidth(), screen.getHeight(),
                        camera.getViewMatrix(), camera.getProjectionMatrix(), x, y);
                if (objectHit != null) {

                    // intersection point
                    Log.i("CollisionController", "Getting intersection... " + objectHit.getId()+", x="+x+", y="+y);
                    float[] point3D = CollisionDetection.getTriangleIntersection(objectHit, screen.getWidth(), screen.getHeight(),
                            camera.getViewMatrix(), camera.getProjectionMatrix(), x, y);

                    // check
                    if (point3D == null || eventManager == null) return false;

                    // fire event
                    final CollisionEvent collisionEvent = new CollisionEvent(this, objectHit, x, y, point3D);
                    eventManager.propagate(collisionEvent);

                    // acknowledged
                    return true;
                }
            }
        }
        return false;
    }
}
