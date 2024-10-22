package org.the3deer.android_3d_model_engine.gui;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.collision.Collision;
import org.the3deer.android_3d_model_engine.collision.CollisionDetection;
import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

public class Widget extends Object3DData implements EventListener {


    private static final String TAG = Widget.class.getSimpleName();

    public static abstract class Event extends EventObject {

        public Event(Object source) {
            super(source);
        }

        public Widget getWidget() {
            return (Widget) super.getSource();
        }
    }

    public static class ChildAdded extends Widget.Event {

        public ChildAdded(Object source) {
            super(source);
        }
    }

    public static class ChildRemoved extends Widget.Event {

        public ChildRemoved(Object source) {
            super(source);
        }
    }

    public static class ClickEvent extends Widget.Event {

        private final float x;
        private final float y;
        private final float z;

        ClickEvent(Object source, float x, float y, float z) {
            super(source);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }


    }

    public static class MoveEvent extends Widget.Event {

        private final float x;
        private final float y;
        private final float z;
        private final float dx;
        private final float dy;

        MoveEvent(Object source, float[] point, float dx, float dy) {
            this(source, point[0], point[1], point[2], dx, dy);
        }

        MoveEvent(Object source, float x, float y, float z, float dx, float dy) {
            super(source);
            this.x = x;
            this.y = y;
            this.z = z;
            this.dx = dx;
            this.dy = dy;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }

        public float getDx() {
            return dx;
        }

        public float getDy() {
            return dy;
        }
    }

    public static final int POSITION_TOP_LEFT = 0;
    public static final int POSITION_TOP = 1;
    public static final int POSITION_MIDDLE = 4;
    public static final int POSITION_RIGHT = 5;
    public static final int POSITION_LEFT = 3;
    public static final int POSITION_TOP_RIGHT = 2;
    public static final int POSITION_BOTTOM = 7;
    public static final int POSITION_BOTTOM_LEFT = 8;

    public static final int POSITION_CHILD_LEFT = 11;

    public static final int POSITION_CHILD_BOTTOM = 12;


    private static int counter = 0;

    protected Dimensions contentDimensions;
    protected float width;
    protected float height;
    protected int relativeLocation = -1;

    private float[] viewport;
    private float[] viewportSize;


    protected List<Widget> widgets = new ArrayList<>(0);

    protected final List<Runnable> runnables = new ArrayList<>();

    /**
     * Widget's background.  can be a quad or texture
     */
    private Widget background;

    private float[] bindScale;
    private float[] bindLocation;
    private float[] userLocation;

    {
        setId(getClass().getSimpleName() + "_" + ++counter);
        setDrawUsingArrays(true);
        setDrawMode(GLES20.GL_LINE_STRIP);
        setVisible(false);
        //setCentered(true);

    }

    protected Runnable animation;

    protected Object3DData source;

    public static final float PADDING_01 = 0.1f;
    public static final float PADDING_02 = 0.2f;

    private float margin;

    private float padding;

    //protected Camera camera;

    private Runnable onClick;

    @Inject
    private EventManager eventManager;

    private boolean isFloating;

    public Widget() {
        super();
    }

    public Widget(Widget parent) {
        super();
        this.parent = parent;
    }

    public Widget(Widget parent, float width, float height) {
        super();
        this.parent = parent;
        this.width = width;
        this.height = height;
        setDimensions(new Dimensions(0, width, height, 0, 0, 0));
    }

    public Widget(Object3DData source) {
        this.source = source;
        setVertexBuffer(source.getVertexBuffer());
        setDrawMode(source.getDrawMode());
        setColorsBuffer(source.getColorsBuffer());
    }

    /**
     * Called by the {@link BeanFactory}
     */
    public void setUp() {
        //this.camera = BeanFactory.getInstance().find(Camera.class, this);
        //refresh();
    }

    public void init() {
        //refresh();
    }

    /**
     * This changes the visibility on children as well
     *
     * @param visible true or false
     * @return this
     */
    public Widget setVisible(boolean visible, boolean children) {
        super.setVisible(visible);
        if (children) {
            for (Widget widget : widgets) {
                widget.setVisible(visible, true);
            }
        }
        return this;
    }

    public void setFloating(boolean floating) {
        isFloating = floating;
    }

    public boolean isFloating() {
        return isFloating;
    }

    public float getPadding() {
        return padding;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }

    public float[] getViewMatrix() {
        //return Configuration.getInstance().viewMatrix;
        return ((Camera) BeanFactory.getInstance().get("gui.camera")).getViewMatrix();
    }

    public float[] getProjectionMatrix() {
        //return Configuration.getInstance().projectionMatrix;
        return ((Camera) BeanFactory.getInstance().get("gui.camera")).getProjectionMatrix();
    }

    public Dimensions getContentDimensions() {
        if (contentDimensions == null) {
            return getCurrentDimensions();
        }
        return new Dimensions(contentDimensions, getModelMatrix());
    }

    @Override
    protected void updateDimensions() {
        super.updateDimensions();
        this.contentDimensions = null;
    }

    /*public float[] getViewport() {
        if (viewport == null && parent != null) {
            return ((Widget) parent).getViewport();
        }
        return getLocation();
    }*/

/*    public float[] getViewPortWorldSpace() {
        return unproject(getViewport()[0], getViewport()[1], 0);
    }*/

    public void setViewport(float[] viewport) {
        this.viewport = viewport;
    }

/*    public float[] getViewportSize() {
        if (viewportSize == null) {
            viewportSize = new float[]{getScreenWidth(), getScreenHeight(), 1};
        }
        return viewportSize;
    }*/

    public void setViewportSize(float[] viewportSize) {
        this.viewportSize = viewportSize;
    }

    @Override
    public Object3DData setColor(float[] color) {
        super.setColor(color);

        if (!isDrawUsingArrays()) return this;
        if (getColorsBuffer() == null) return this;


        /*FloatBuffer buffer = getColorsBuffer();
        for (int i = 0; i < buffer.capacity(); i += color.length) {
            float alpha = buffer.get(i + 3);
            if (alpha > 0f) {
                buffer.position(i);
                buffer.put(color);
            }
        }
        setColorsBuffer(buffer);*/

        return this;
    }

    public void setBackground(Widget background) {
        this.background = background;
        Log.v("Widget", "New background: " + background);
    }

    public Widget getBackground() {
        return background;
    }

    public Runnable getOnClick() {
        return onClick;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    static void buildBorder(FloatBuffer vertexBuffer, int voffset, FloatBuffer colorBuffer, int coffset,
                            float width, float height, float padding) {

        // transparent link
        vertexBuffer.put(voffset++, -padding).put(voffset++, -padding).put(voffset++, 0);
        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 0f);

        // border
        vertexBuffer.put(voffset++, -padding).put(voffset++, -padding).put(voffset++, 0);
        vertexBuffer.put(voffset++, width + padding).put(voffset++, -padding).put(voffset++, 0);
        vertexBuffer.put(voffset++, width + padding).put(voffset++, height + padding).put(voffset++, 0);
        vertexBuffer.put(voffset++, -padding).put(voffset++, height + padding).put(voffset++, 0);

        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 1f);
        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 1f);
        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 1f);
        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 1f);

        // transparent link
        vertexBuffer.put(voffset++, -padding).put(voffset++, -padding).put(voffset++, -1);
        for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 1f);

    }

    @Override
    public Object3DData setLocation(float[] location) {
        super.setLocation(location);
        return this;
    }

    public void setBindLocation(float[] bindLocation) {
        this.bindLocation = bindLocation;
    }

    public float[] getBindLocation() {
        if (bindLocation == null) return getLocation();
        return bindLocation;
    }

    public float[] getUserLocation() {
        if (userLocation == null) return getLocation();
        return userLocation;
    }

    public void setUserLocation(float[] userLocation) {
        this.userLocation = userLocation;
    }

    /**
     * @param scale the initial scale for this widget
     * @return this object
     */
    public Object3DData setBindScale(float[] scale) {
        this.bindScale = scale;
        return this;
    }


    public Object3DData setRelativeLocation(int relativeLocation) {
        this.relativeLocation = relativeLocation;
        this.refreshRelativeLocation();
        return this;
    }

    /**
     * updates the location of all the widgets to their relative location
     */
    @Override
    public void refresh() {
        refreshRelativeScale();
        refreshRelativeLocation();
        if (!widgets.isEmpty()) {
            for (Widget widget : widgets) {
                widget.refresh();
            }
        }
    }

    protected void refreshRelativeLocation() {

        if (parent == null || relativeLocation == -1) {
            return;
        }

        final Dimensions parentDim = parent.getCurrentDimensions();
        final Dimensions widgetDim = this.getCurrentDimensions2();

        float x = 0, y = 0, z = parentDim.getMin()[2];

        switch (relativeLocation) {
            case POSITION_TOP_LEFT:
                x = parentDim.getMin()[0] - widgetDim.getMin()[0];
                //y = parentDim.getMax()[1] - widgetDim.getMax()[1];
                y = parentDim.getMax()[1] - widgetDim.getMax()[1];
                break;
            case POSITION_TOP:
                x = parentDim.getCenter()[0] - widgetDim.getCenter()[0];
                y = parentDim.getMax()[1] - widgetDim.getMax()[1];
                //Log.v("Widget","("+getId()+") parentDim: "+parentDim);
                //Log.v("Widget","("+getId()+") widgetDim: "+widgetDim);
                break;
            case POSITION_TOP_RIGHT:
                //Log.v(TAG, "top right: widget: " + getId() + " parent (" + parent.getId() + ") : " + parentDim + ", child: " + widgetDim);
                x = parentDim.getMax()[0] - widgetDim.getMax()[0];
                y = parentDim.getMax()[1] - widgetDim.getMax()[1];
                break;
            case POSITION_MIDDLE:
                x = parentDim.getCenter()[0] - widgetDim.getCenter()[0];
                y = parentDim.getCenter()[1] - widgetDim.getCenter()[1];
                /*x = parentDim.getCenter()[0];
                y = parentDim.getCenter()[1];*/
                break;
            case POSITION_RIGHT:
                //Log.v("Widget", "right: widget: " + widget.getId() + " x min: " + widgetDim.getMin()[1] + ", max: " + widgetDim.getMax()[1]);
                x = parentDim.getMax()[0] - widgetDim.getMax()[0];
                y = parentDim.getCenter()[0] - widgetDim.getCenter()[1];
                break;
            case POSITION_LEFT:
                x = parentDim.getMin()[0] - widgetDim.getMin()[0];
                y = parentDim.getCenter()[1] - widgetDim.getMiddle()[1];
                break;
            case POSITION_BOTTOM:
                // x = parentDim.getCenter()[0] - widgetDim.getWidth() / 2f;
                //y = parentDim.getCenter()[1] - widgetDim.getHeight() / 2f;
                x = parentDim.getCenter()[0] - widgetDim.getCenter()[0];
                y = parentDim.getMin()[1] - widgetDim.getMin()[1];
                break;
            case POSITION_BOTTOM_LEFT:
                x = parentDim.getMin()[0] - widgetDim.getMin()[0];
                y = -1 + widgetDim.getHeight() / 2 - widgetDim.getCenter()[1];
                break;
            case POSITION_CHILD_LEFT:
                x = parentDim.getMin()[0] - widgetDim.getMax()[0];
                y = parentDim.getCenter()[1] - widgetDim.getCenter()[1];
                //z += Constants.UI_WIDGET_CHILD_Z;
                break;
            case POSITION_CHILD_BOTTOM:
                x = parentDim.getMin()[0] - widgetDim.getMin()[0];
                y = parentDim.getMin()[1] - widgetDim.getMax()[1];
                //z += Constants.UI_WIDGET_CHILD_Z;
                break;
            default:
                // throw new UnsupportedOperationException();
                Log.e("Widget", "invalid relative location");
        }
        float[] newLocation = new float[]{x, y, z};
        this.setLocation(newLocation);
    }

    public int getScreenWidth() {
//        return Configuration.getInstance().screenWidth;
        return ((Screen) BeanFactory.getInstance().get("screen")).getWidth();
    }

    public int getScreenHeight() {
        //return Configuration.getInstance().screenHeight;
        return ((Screen) BeanFactory.getInstance().get("screen")).getHeight();
    }

    float[] getBindScale() {
        if (bindScale == null) return getScale();
        return bindScale;
    }


    public float getMargin() {
        return margin;
    }

    public void setMargin(float margin) {
        this.margin = margin;
    }


    /**
     * This animation transforms the widget (initially the parent) and all children.
     * The scale of the children are impacted (scale 0-100%)
     * The position of the children are refreshed if they are relative
     *
     * @param parent widget being animated
     * @param start  start spec
     * @param end    end spec
     * @param millis time
     */
    private void animate(final Widget parent, final JointTransform start, final JointTransform end, long millis) {

        animate_impl(parent, start, end, millis);

        /*for (Widget widget : widgets) {
            widget.animate(this, start, end, millis);
        }*/

    }

    private void animate_impl(final Widget parent, final JointTransform start, final JointTransform end, final long millis) {
        final JointTransform result = new JointTransform(new float[16]);
        final long startTime = SystemClock.uptimeMillis();
        this.animation = () -> {
            result.setVisible(start.isVisible());
            long elapsed = SystemClock.uptimeMillis() - startTime;
            if (elapsed >= millis) {
                elapsed = millis;
                result.setVisible(end.isVisible());
                this.animation = null;
            }
            float progression = (float) elapsed / millis;
            Math3DUtils.interpolate(result, start, end, progression);

            float[] unbox = start.getLocation() != null && end.getLocation() != null ? unbox(result.getLocation()) : null;
            if (unbox != null && parent == this) this.setLocation(unbox);
            //if (unbox != null && parent != this) this.refreshLocation();

            // FIXME: scaling must scale child as well
            //float[] unbox1 = start.getScale() != null && end.getScale() != null ? unbox(result.getScale()) : null;
            //if (unbox1 != null) this.setScale(unbox1);
            //float[] unbox2 = unbox(result.getRotation1());
            //if (unbox2 != null) setRotation(unbox2);
            // setRotation2(unbox(result.getRotation2()), unbox(result.getRotation2Location()));
            this.setVisible(result.isVisible(), true);

            // FIXME: refreshing relative location makes sense with animations?
            // widget.refresh();
        };
    }

    private static float[] unbox(Float[] boxed) {
        if (boxed == null) return null;

        float[] ret = new float[boxed.length];
        for (int i = 0; i < boxed.length; i++) {
            ret[i] = boxed[i];
        }
        return ret;
    }

    @Override
    public void toggleVisible() {
        if (animation != null) return;

        if (isVisible()) {
            Log.v("Widget", "Hiding widget...");

            JointTransform start = new JointTransform(new Float[3], null, new Float[3]);
            start.setVisible(true);
            start.setScale(getBindScale());
            start.setLocation(getUserLocation());

            JointTransform end = new JointTransform(new Float[3], null, new Float[3]);
            end.setVisible(false);
            end.setScale(new float[]{0, 0, 0});
            end.setLocation(getBindLocation());

            animate(this, start, end, 250);
        } else {
            Log.v("Widget", "Showing widget... this:" + this);

            JointTransform start = new JointTransform(new Float[3], null, new Float[3]);
            start.setVisible(true);
            start.setScale(new float[]{0, 0, 0});
            start.setLocation(getBindLocation());
            Log.v("Widget", "Showing widget... bind location:" + Arrays.toString(getBindLocation()));

            JointTransform end = new JointTransform(new Float[3], (Float[]) null, new Float[3]);
            end.setVisible(true);
            end.setScale(getBindScale());
            end.setLocation(getUserLocation());
            Log.v("Widget", "Showing widget... target location:" + Arrays.toString(getUserLocation()));

            animate(this, start, end, 250);
        }
        /*if (!widgets.isEmpty()) {
            for (Widget widget : widgets) {
                widget.toggleVisible();
            }
        }*/
    }

    public void toggleLocation(float[] newLocation) {
        if (animation != null) return;

        //Log.v("Widget", "Moving widget... this:" + this);
        JointTransform start = new JointTransform(null, null, new Float[3]);
        start.setLocation(getLocation());

        JointTransform end = new JointTransform(null, null, new Float[3]);
        end.setLocation(newLocation);
        Log.v("Widget", "Showing widget... target location:" + Arrays.toString(newLocation));

        animate(this, start, end, 250);
    }

    /**
     * You can only impact (add,remove) certain data structures (list)  while doing it in the same UI thread that iterates over them
     * Otherwise we get a ConcurrentModificationException
     *
     * @param behaviour the action to execute
     */
    public void invokeOnUIThread(Runnable behaviour) {
        synchronized (this.runnables) {
            this.runnables.add(behaviour);
        }
    }

    protected void invokeUIBehaviours() {

        if (!this.runnables.isEmpty()) {
            synchronized (this.runnables) {
                for (int i = 0; i < this.runnables.size(); i++) {
                    this.runnables.get(i).run();
                }
                this.runnables.clear();
            }
        }

        if (!widgets.isEmpty()) {
            for (int i = 0; i < widgets.size(); i++) {
                widgets.get(i).invokeUIBehaviours();
            }
        }
    }

    /*@Override
    public void render(RendererFactory rendererFactory, Camera camera, float[] lightPosInWorldSpace, float[] colorMask) {
        super.render(rendererFactory, camera, lightPosInWorldSpace, colorMask);

        //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        //GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);


        // draw background?
        if (this.background != null) {
            background.render(rendererFactory, camera, lightPosInWorldSpace, colorMask);
        }

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        // draw widget
        this.renderImpl(rendererFactory, lightPosInWorldSpace, colorMask);

        // draw children
        for (int i = 0; i < widgets.size(); i++) {
            if (widgets.get(i) == this.background) continue;
            widgets.get(i).render(rendererFactory, camera, lightPosInWorldSpace, colorMask);
        }
    }*/

    /*protected void renderImpl(RendererFactory rendererFactory, float[] lightPosInWorldSpace, float[] colorMask) {

        onDrawFrame();

        // don't draw this widget?
        if (!isVisible()) return;

        // not something to render
        if (!isRender() || vertexBuffer == null) return;

        // draw all GUI objects
        //GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        Renderer drawer = rendererFactory.getDrawer(this, false, false, false, false, false, false);
        drawer.draw(this, Configuration.getInstance().projectionMatrix, Configuration.getInstance().viewMatrix, -1, lightPosInWorldSpace, colorMask,
                Constants.CAMERA_POSITION, this.getDrawMode(), this.getDrawSize());
    }*/

    public void onDrawFrame() {
        if (animation != null) animation.run();
    }

    /**
     * Unproject world coordinates into model original coordinates
     *
     * @param clickEvent click event
     * @return original model coordinates
     */
    protected float[] unproject(Widget.ClickEvent clickEvent) {
        float x = clickEvent.getX();
        float y = clickEvent.getY();
        float z = clickEvent.getZ();
        x -= getLocationX();
        x /= getScaleX();
        y -= getLocationY();
        y /= getScaleY();
        z -= getLocationZ();
        z /= getScaleZ();
        return new float[]{x, y, z};
    }

    @Override
    protected boolean propagate(EventObject event) {
        if (super.propagate(event)){
            return false;
        }
        if (onEvent(event)) {
            return false;
        }
        if (parent instanceof Widget) {
            //Log.v(TAG, "Propagate from "+this.getId()+"->"+parent.getId());
            if (parent == this) {
                throw new IllegalStateException("parent==this: "+parent);
            }
            ((Widget) parent).propagate(event);
            return false;
        }
        //}
        if (eventManager != null) {
            eventManager.propagate(event);
            return false;
        }

        return false;
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof ChildAdded) {
            //Log.v(TAG,"onEvent ("+getId()+"): "+event);
            //refresh();
            /*if (event.getSource() == this ||
                    widgets.contains(((ChildAdded) event).getWidget())) {
                refresh();
            }
            if (!widgets.isEmpty()) {
                for (Widget widget : widgets) {
                    if (widget.onEvent(event)) {
                        return true;
                    }
                }
            }*/

        } else if (event instanceof ChildRemoved) {
            if (!widgets.isEmpty()) {
                for (Widget widget : widgets) {
                    if (widget.onEvent(event)) {
                        return true;
                    }
                }
            }
            if (event.getSource() == this) {
                refresh();
            }
        } else if (event instanceof ChangeEvent) {
            if (!widgets.isEmpty()) {
                for (Widget widget : widgets) {
                    if (widget.onEvent(event)) {
                        return true;
                    }
                }
            }
            // refreshLocation();
        } else if (event instanceof ClickEvent) {
            if (!widgets.isEmpty()) {
                for (Widget widget : widgets) {
                    if (widget.onEvent(event)) {
                        return true;
                    }
                }
            }
            if (canHandle(event)) {
                return click((ClickEvent) event);
            }
        } else if (event instanceof MoveEvent) {
            return processMoveEvent((MoveEvent) event);
        } /*else if (event instanceof TouchEvent) {
            TouchEvent touchEvent = (TouchEvent) event;
            TouchEvent.Action action = touchEvent.getAction();
            if (action == TouchEvent.Action.CLICK) {
                return processTouchEvent(touchEvent);
            } else if (action == TouchEvent.Action.MOVE) {
                float x = touchEvent.getX();
                float y = touchEvent.getY();
                float[] nearHit = CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(), x, y, 0);
                float[] farHit = CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(), x, y, 1);
                float[] direction = Math3DUtils.substract(farHit, nearHit);
                Math3DUtils.normalize(direction);
                return processDragEvent(touchEvent, nearHit, direction);
            }
        }*/ else if (event.getSource() == this) {
            if (!widgets.isEmpty()) {  //FIXME: review why we are doing this
                for (Widget widget : widgets) {
                    widget.updateModelMatrix();
                }
            }
        } else {
            for (Widget widget : widgets) {
                if (widget.onEvent(event)) {
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean canHandle(EventObject event) {
        if (event.getSource() == this) {
            if (event instanceof ClickEvent) {
                return isClickable();
            } else if (event instanceof MoveEvent) {
                return isMovable();
            }
        }
        return false;
    }

    protected boolean click(ClickEvent event) {
        if (isClickable()) {
            if (onClick != null) {
                onClick.run();
                return true;
            }
        }
        return false;
    }

    protected boolean move(MoveEvent event) {
        if (isMovable()) {
            //Log.v("Widget","Moving widget... "+getId());
            float[] newPosition = event.getWidget().getLocation().clone();
            newPosition[0] += event.getDx();
            newPosition[1] += event.getDy();
            event.getWidget().setLocation(newPosition);
            event.getWidget().setUserLocation(newPosition);
            // event.getWidget().setRelativeLocation(-1);
            refresh();
            return true;
        }
        return false;
    }

    public void dispose() {
        if (parent instanceof Widget) {
            ((Widget) parent).removeChild(this);
        }

        // dispose children
        for (Widget widget : widgets) {
            widget.dispose();
        }

        // unregister from parent
        this.widgets = new ArrayList<>(0);
        this.background = null;

        // default dipose
        super.dispose();
    }

    /**
     * Adds the widget so it can be rendered when parent is drawn
     *
     * @param child child
     */
    public void addChild(Widget child) {
        // invokeOnUIThread(() -> {
        if (child == this){
            throw new IllegalStateException("child==this : "+child);
        }

        // update parent link
        if (child.parent instanceof Widget && child.parent != this) {
            ((Widget) child.parent).removeChild(child);
        }
        child.parent = this;

        // register child
        if (child instanceof Background) {
            // The background must be rendered first so we keep a reference
            this.background = child;
        }

        final ArrayList<Widget> newWidgets = new ArrayList<>(widgets);
        newWidgets.add(child);
        this.widgets = newWidgets;
        //addListener(child);

        //propagate(new Widget.ChildAdded(child));

        //Log.d("Widget", "new child for " + getId() + ". child: " + child.getId());
        // });
    }

    public void removeChild(Widget child) {
        child.setParent(null);
        final ArrayList<Widget> newWidgets = new ArrayList<>(widgets);
        newWidgets.remove(child);
        this.widgets = newWidgets;

        propagate(new Widget.ChildRemoved(child));
    }


    protected boolean processDragEvent(TouchEvent touchEvent, float[] nearHit, float[] direction) {

        // detect child collisions first
        if (!this.widgets.isEmpty()) {
            for (int i = 0; i < this.widgets.size(); i++) {
                final Widget child = this.widgets.get(i);
                if (!child.processDragEvent(touchEvent, nearHit, direction)) {
                    return false;
                }
            }
        }

        if (isMovable()) {
            final Collision collision = detectCollision(touchEvent, nearHit, direction);
            if (collision != null) {
                propagate(new MoveEvent(collision.getObject(), collision.getPoint(), collision.getDx(), collision.getDy()));
                return false;
            }
        }
        return true;
    }

    /**
     * @param event
     * @return true if processing is successful. otherwise true to continue event chain
     */
    protected boolean processMoveEvent(MoveEvent event) {
        if (!widgets.isEmpty()) {
            for (Widget widget : widgets) {
                if (widget.processMoveEvent(event)) {
                    return true;
                }
            }
        }
        if (canHandle(event)) {
            return move(event);
        }
        return false;
    }

    private Collision detectCollision(TouchEvent touchEvent, float[] nearHit, float[] direction) {

        // discard not applicable objects
        if (!isVisible() || !isSolid()) return null;

        // get intersection
        final float[] intersection = CollisionDetection.getBoxIntersection(nearHit, direction, getCurrentBoundingBox());
        if (intersection[0] >= 0 && intersection[0] <= intersection[1]) {
            float[] point = Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection[0]));

            // calculate point 2
            float[] nearHit2 = CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(),
                    touchEvent.getX2(), touchEvent.getY2(), 0);
            float[] point2 = Math3DUtils.add(nearHit2, Math3DUtils.multiply(direction, intersection[0]));

            float dx = point2[0] - point[0];
            float dy = point2[1] - point[1];

            // Log.v("Widget", "Collision ! "+getId());
            return new Collision(this, point, dx, dy);
        }
        return null;
    }

    private boolean processTouchEvent(TouchEvent touchEvent) {

        // forward first the event to our children
        for (Widget widget : widgets) {
            if (widget.processTouchEvent(touchEvent)) {
                return true;
            }
        }

        // test if this object is clickable
        if (this.isClickable() && this.isVisible()) {

            // test if user is touching this widget
            float[] point = getClickPoint(touchEvent.getX(), touchEvent.getY(), this);
            if (point != null) {
                // otherwise we just fire the event (other will handle it)
                propagate(new ClickEvent(this, point[0], point[1], point[2]));
                return true;
            }
        }
        return false;
    }

    // FIXME: we can calculate vector once rather than recursively
    // FIXME: this function may be replace by detect collision (need to generalize function)
    private float[] getClickPoint(float x, float y, Widget widget) {
        float[] nearHit = CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(), x, y, 0);
        float[] farHit = CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(), x, y, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalizeVector(direction);

        float[] intersection = CollisionDetection.getBoxIntersection(nearHit, direction, widget.getCurrentBoundingBox());
        if (intersection[0] >= 0 && intersection[0] <= intersection[1]) {
            Log.d("Widget", "Clicked! " + widget.getId());
            return Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection[0]));
        }
        return null;
    }

    protected float[] unproject(float x, float y, float z) {
        return CollisionDetection.unProject(getScreenWidth(), getScreenHeight(), getViewMatrix(), getProjectionMatrix(), x, y, z);
    }
}
