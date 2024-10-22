package org.the3deer.android_3d_model_engine.gui;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.collision.Collision;
import org.the3deer.android_3d_model_engine.collision.CollisionDetection;
import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.bean.BeanOrder;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

@BeanOrder(order=101)
public class GUISystem implements EventListener, Renderer {

    private final static String TAG = GUISystem.class.getSimpleName();

    private boolean enabled = true;

    @Inject
    private EventManager eventManager;
    @Inject
    private Screen screen;
    @Inject
    private Camera camera = new Camera();
    @Inject
    private List<Widget> widgets = new ArrayList<>();
    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    public GUISystem(){
    }

    /*@Override
    public List<? extends Object3DData> getObjects() {
        return widgets;
    }*/

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
        this.widgets = widgets;
    }

    public List<EventListener> getListeners() {
        return listeners;
    }

    public void setListeners(List<EventListener> listeners) {
        this.listeners = listeners;
    }

    public void setUp(){
        /*this.camera = Registry.getInstance().find(Camera.class, this);
        this.widgets = Registry.getInstance().findAll(Widget.class);
        this.listeners = Registry.getInstance().findAll(EventListener.class);*/

        // BeanFactory.getInstance().find(TouchController.class).addListener(this);

        Log.v(TAG,"Widgets found: "+widgets.size());
    }

    public void onDrawFrame() {

        // check
        if (!enabled) return;

        // draw
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // draw all GUI objects
        for (int i = 0; i < widgets.size(); i++) {
            // ((Widget)guiObjects.get(i)).invokeUIBehaviours();
            render(widgets.get(i));
        }
    }

    public void render(Widget widget) {
        // super.render(rendererFactory, camera, lightPosInWorldSpace, colorMask);

        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        widget.invokeUIBehaviours();

        // draw background?
        if (widget.getBackground() != null) {
            renderImpl(widget.getBackground());
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // draw widget
        renderImpl(widget);

        // draw children
        for (int i = 0; i < widget.widgets.size(); i++) {
            if (widget.widgets.get(i) == widget.getBackground()) continue;
            render(widget.widgets.get(i));
        }
    }

    protected void renderImpl(Widget widget) {

        widget.onDrawFrame();

        // don't draw this widget?
        if (!widget.isVisible()) return;

        // not something to render
        if (!widget.isRender() || widget.getVertexBuffer() == null) return;

        // draw all GUI objects
        //GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        final Shader drawer = ShaderFactory.getInstance().getShader(widget, false, false, false, false, false, false);
        //final Camera camera = widget.camera != null? widget.camera : this.camera;

        if (widget.getId().startsWith("fps")) {
            /*Log.v("GUISystem","Rendering widget... : "+widget.getId()+
                    ", loc: "+ Arrays.toString(widget.getLocation()));
            Log.v("GUISystem","Rendering with cam "+camera.getProjection());*/
        }

        drawer.draw(widget, camera.projectionMatrix, camera.viewMatrix, null, null,
                GUIConstants.CAMERA_POSITION, widget.getDrawMode(), widget.getDrawSize());
    }

    @Override
    public boolean onEvent(EventObject event) {

        if (event instanceof TouchEvent) {

            TouchEvent touchEvent = (TouchEvent) event;
            TouchEvent.Action action = touchEvent.getAction();
            if (action == TouchEvent.Action.CLICK) {
                boolean processed = processTouchEvent(touchEvent, widgets);

                // floating widgets
                // FIXME:
                if (!processed) hideFloating(widgets);

                return processed;
            } else if (action == TouchEvent.Action.MOVE) {
                float x = touchEvent.getX();
                float y = touchEvent.getY();
                if (screen != null) {
                    float[] nearHit = CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix, x, y, 0);
                    float[] farHit = CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix, x, y, 1);
                    float[] direction = Math3DUtils.substract(farHit, nearHit);
                    Math3DUtils.normalizeVector(direction);
                    return processDragEvent(touchEvent, widgets, nearHit, direction);
                }
            }
        }
        return false;
    }

    private void hideFloating(List<Widget> widgets){
        //Log.v(TAG,"Hiding floating widgets... ");
        if (widgets != null && !widgets.isEmpty()) {
            for (Widget child : widgets) {
                hideFloating(child.widgets);
                if (child.isFloating()) {
                    Log.d(TAG, "Disposing floating widget... " + child.getId());
                    child.setVisible(false);
                    child.dispose();
                }
            }
        }
    }


    private boolean processTouchEvent(TouchEvent touchEvent, List<Widget> widgets){
        for (Widget child : widgets) {
            // forward first the event to our children
            if (processTouchEvent(touchEvent, child.widgets)) {
                return true;
            } else {
                // test if this object is clickable
                if (child.isClickable() && child.isVisible()) {

                    // test if user is touching this widget
                    float[] point = getClickPoint(touchEvent.getX(), touchEvent.getY(), child);
                    if (point != null) {
                        // otherwise we just fire the event (other will handle it)
                        if (eventManager != null) {
                            eventManager.propagate(new Widget.ClickEvent(child, point[0], point[1], point[2]));
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // FIXME: we can calculate vector once rather than recursively
    // FIXME: this function may be replace by detect collision (need to generalize function)
    private float[] getClickPoint(float x, float y, Widget widget) {
        float[] nearHit = CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix, x, y, 0);
        float[] farHit = CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix, x, y, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalizeVector(direction);

        float[] intersection = CollisionDetection.getBoxIntersection(nearHit, direction, widget.getCurrentBoundingBox());
        if (intersection[0] >= 0 && intersection[0] <= intersection[1]) {
            Log.d("GUISystem", "Clicked! " + widget.getId());
            return Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection[0]));
        }
        return null;
    }

    protected float[] unproject(float x, float y, float z) {
        return CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix, x, y, z);
    }

    protected boolean processDragEvent(TouchEvent touchEvent, List<Widget> widgets, float[] nearHit, float[] direction) {

        // detect child collisions first
        if (!widgets.isEmpty()) {
            for (int i = 0; i < widgets.size(); i++) {
                final Widget child = widgets.get(i);
                if (processDragEvent(touchEvent, child.widgets, nearHit, direction)) {
                    return true;
                }
                else {
                    if (child.isMovable()) {
                        final Collision collision = detectCollision(touchEvent, child, nearHit, direction);
                        if (collision != null) {
                            if (eventManager != null) {
                                eventManager.propagate(new Widget.MoveEvent(collision.getObject(), collision.getPoint(), collision.getDx(), collision.getDy()));
                            }
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private Collision detectCollision(TouchEvent touchEvent, Widget widget, float[] nearHit, float[] direction) {

        // discard not applicable objects
        if (!widget.isVisible() || !widget.isSolid()) return null;

        // get intersection
        final float[] intersection = CollisionDetection.getBoxIntersection(nearHit, direction, widget.getCurrentBoundingBox());
        if (intersection[0] >= 0 && intersection[0] <= intersection[1]) {
            float[] point = Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection[0]));

            // calculate point 2
            float[] nearHit2 = CollisionDetection.unProject(screen.getWidth(), screen.getHeight(), camera.viewMatrix, camera.projectionMatrix,
                    touchEvent.getX2(), touchEvent.getY2(), 0);
            float[] point2 = Math3DUtils.add(nearHit2, Math3DUtils.multiply(direction, intersection[0]));

            float dx = point2[0] - point[0];
            float dy = point2[1] - point[1];

            // Log.v("Widget", "Collision ! "+getId());
            return new Collision(widget, point, dx, dy);
        }
        return null;
    }
}
