package org.the3deer.engine.model;

// http://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space

import androidx.annotation.NonNull;

import org.the3deer.engine.android.util.AndroidUtils;
import org.the3deer.util.math.Matrix;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.logging.Logger;

import javax.inject.Inject;

public class Camera {

    /**
     * Controls the camera
     */
    public interface Controller {

        default void move(float dX, float dY) {
        }

        default void zoom(float direction) {
        }

        default void rotate(float angle) {
        }

        default void pan(float dX, float dY) {
        }
    }

    public static class CameraLoadedEvent extends EventObject {

        private Camera camera;

        /**
         * Constructs a prototypical Event.
         *
         * @param source the object on which the Event initially occurred
         * @throws IllegalArgumentException if source is null
         */
        public CameraLoadedEvent(Object source, Camera camera) {
            super(source);
            this.camera = camera;
        }

        public Camera getCamera() {
            return camera;
        }

        public void setCamera(Camera camera) {
            this.camera = camera;
        }
    }

    /**
     * Triggers on any camera update
     */
    public static class CameraUpdatedEvent extends EventObject {

        /**
         * Constructs a prototypical Event.
         *
         * @param source the object on which the Event initially occurred
         * @throws IllegalArgumentException if source is null
         */
        public CameraUpdatedEvent(Object source) {
            super(source);
        }

        public Camera getCamera(){
            return (Camera)getSource();
        }
    }

    private static final Logger logger = Logger.getLogger(Camera.class.getSimpleName());

    private final String name;

    // Relaxed bounding boxes to allow framing of tiny models (like Avocado)
    private final BoundingBox centerBox = new BoundingBox("scene", -0.00001f, 0.00001f,
            -0.00001f, 0.00001f, -0.00001f, 0.00001f);
    private final BoundingBox roomBox = new BoundingBox("scene", -Constants.ROOM_SIZE, Constants.ROOM_SIZE,
            -Constants.ROOM_SIZE, Constants.ROOM_SIZE, -Constants.ROOM_SIZE, Constants.ROOM_SIZE);

    // new vector model
    protected float[] pos = new float[]{0, 0, 1, 1};
    protected float[] view = new float[]{0, 0, 0, 1};
    protected float[] up = new float[]{0, 1, 0, 1};

    // transformation matrix
    private float[] viewMatrix = new float[16];
    {
        // default camera
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0f);
    }

    // default projection (inject only works for managed objects - like the default engine camera)
    @Inject
    private Projection projection = new PerspectiveProjection();

    // default listeners (inject only works for managed objects - like the default engine camera)
    @Inject
    private List<EventListener> listeners = new ArrayList<>();
    /**
     * The new orientation of the device.
     * <p>
     * Please check @{@link android.view.OrientationEventListener}
     */
    private int deviceOrientation;

    // camera orientation
    private float[] orientationMatrix = new float[16];
    private Quaternion orientation = new Quaternion();

    //@Inject
    //private Screen screen;

    //@Inject
    private Controller controller;

    private boolean changed = false;

    // stereoscopic handlers
    private Camera[] stereoCam;

    // scene graph
    private Node node;

    // kotlin
    public Camera() {
        this("Camera:_"+System.currentTimeMillis());
    }

    // gltf + legacy
    public Camera(String name) {
        this.name = name;
    }

    // default camera - shadow
    public Camera(String name, float[] location) {
        // Initialize variables...
        this(name, location[0], location[1], location[2], 0, 0, 0, 0, 1, 0);
    }

    public Camera(String name, Camera cam2) {
        this.name = name;
        //this.screen = cam2.screen;
        this.pos = cam2.pos;
        this.view = cam2.view;
        this.up = cam2.up;
        this.viewMatrix = cam2.viewMatrix;
        this.projection = cam2.projection;
    }

    public Camera(String name, float xPos, float yPos, float zPos, float xView, float yView, float zView, float xUp, float yUp,
                  float zUp) {

        this.name = name;
        // Here we set the camera to the values sent in to us. This is mostly
        // used to set up a
        // default position.
        this.pos[0] = xPos;
        this.pos[1] = yPos;
        this.pos[2] = zPos;
        this.view[0] = xView;
        this.view[1] = yView;
        this.view[2] = zView;
        this.up[0] = xUp;
        this.up[1] = yUp;
        this.up[2] = zUp;
    }

    public String getName() {
        return name;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Projection getProjection() {
        return projection;
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }

    public Controller getController() {
        return controller;
    }

    public void setController(Controller controller) {
        this.controller = controller;
    }

    @BeanInit
    public void setUp() {
        refresh();
    }

    public void addListener(EventListener eventListener) {
        this.listeners.add(eventListener);
    }

    // kotlin
    public void setProjection(int width, int height) {
        getProjection().setAspectRatio((float) width / height);
    }

    public void move(float dX, float dY) {
        if (controller != null)
            controller.move(dX, dY);
    }

    public void zoom(float direction) {
        if (controller != null)
            controller.zoom(direction);
    }

    public void rotate(float angle) {
        if (controller != null)
            controller.rotate(angle);
    }

    public void pan(float dX, float dY) {
        if (controller != null)
            controller.pan(dX, dY);
    }

    protected void refresh() {
        // update orientation
        Matrix.setLookAtM(this.orientationMatrix, 0,
                0, 0, 0,
                -getxPos() + getxView(), -getyPos() + getyView(), -getzPos() + getzView(),
                getxUp(), getyUp(), getzUp());
        this.orientation.setMatrix(orientationMatrix);

        // projection
        if (this.projection != null) {
            this.projection.refresh();
        }
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public void enable() {
    }

    /**
     * Test whether specified position is either outside room "walls" or in the very center of the room.
     *
     * @param x x position
     * @param y y position
     * @param z z position
     * @return true if specified position is outside room "walls" or in the very center of the room
     */
    public boolean isOutOfBounds(float x, float y, float z) {
        if (roomBox.outOfBound(x, y, z)) {
            logger.finest("Out of room walls. " + x + "," + y + "," + z);
            return true;
        }
        /*if (!centerBox.outOfBound(x, y, z)) {
            logger.finest("Inside absolute center");
            return true;
        }*/
        return false;
    }

    public synchronized void translateCamera(float dX, float dY) {
    }

    public boolean hasChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
        refresh();
        AndroidUtils.fireEvent(listeners, new CameraUpdatedEvent(this));
    }


    public synchronized void Rotate(float angle) {
    }

    public Camera[] toStereo(float eyeSeparation, float focalDistance) {

        // lazy init
        if (stereoCam == null) {
            stereoCam = new Camera[]{new Camera("left"), new Camera("right")};
        }

        // look vector
        float xLook = getxView() - pos[0];
        float yLook = getyView() - pos[1];
        float zLook = getzView() - pos[2];

        // right vector
        float[] crossRight = Math3DUtils.crossProduct(xLook, yLook, zLook, getxUp(), getyUp(), getzUp());
        Math3DUtils.normalizeVector(crossRight);

        // convergence point
        float xConv = pos[0] + xLook * focalDistance;
        float yConv = pos[1] + yLook * focalDistance;
        float zConv = pos[2] + zLook * focalDistance;

        // new left pos
        float midEye = eyeSeparation / 2;
        float xPosLeft = pos[0] - crossRight[0] * midEye;
        float yPosLeft = pos[1] - crossRight[1] * midEye;
        float zPosLeft = pos[2] - crossRight[2] * midEye;

        // new right pos
        float xPosRight = pos[0] + crossRight[0] * midEye;
        float yPosRight = pos[1] + crossRight[1] * midEye;
        float zPosRight = pos[2] + crossRight[2] * midEye;

        // update left
        final Camera left = stereoCam[0];
        left.projection = this.projection.clone();
        left.set(xPosLeft, yPosLeft, zPosLeft, xConv, yConv, zConv, getxUp(), getyUp(), getzUp());

        // update right
        final Camera right = stereoCam[1];
        right.projection = this.projection.clone();
        right.set(xPosRight, yPosRight, zPosRight, xConv, yConv, zConv, getxUp(), getyUp(), getzUp());

        return stereoCam;
    }

    public float getxView() {
        return view[0];
    }

    public float getyView() {
        return view[1];
    }


    public float getzView() {
        return view[2];
    }

    public float getxUp() {
        return up[0];
    }

    public float getyUp() {
        return up[1];
    }

    public float getzUp() {
        return up[2];
    }

    public float getxPos() {
        return pos[0];
    }

    public float getyPos() {
        return pos[1];
    }

    public float getzPos() {
        return pos[2];
    }

    public void set(float xPos, float yPos, float zPos, float xView, float yView, float zView, float xUp, float yUp,
                    float zUp) {
        this.pos[0] = xPos;
        this.pos[1] = yPos;
        this.pos[2] = zPos;
        this.view[0] = xView;
        this.view[1] = yView;
        this.view[2] = zView;
        this.up[0] = xUp;
        this.up[1] = yUp;
        this.up[2] = zUp;

        setChanged(true);
    }

    public void set(float[] pos, float[] view, float[] up) {
        this.set(pos[0], pos[1], pos[2], view[0], view[1], view[2], up[0], up[1], up[2]);
    }


    public float getDistance() {
        return Math3DUtils.length(this.pos);
    }

    public float[] getPos() {
        return this.pos;
    }

    public float[] getRight() {
        return Math3DUtils.normalize2(Math3DUtils.crossProduct(this.up, this.pos));
    }

    public float[] getUp() {
        return this.up;
    }

    public float[] getView() {
        return this.view;
    }

    // cellphone orientation

    /**
     * Rotate using the current view vector
     *
     * @param angle angle in degrees
     */
    public void setDeviceOrientation(int angle) {

        if (angle == this.deviceOrientation) return;
        else if (Math.abs(this.deviceOrientation - angle) < 5) return;
        else {
            int previous = this.deviceOrientation;
            this.deviceOrientation = angle;
            angle = previous - angle;
        }

        // rotation matrix
        float[] matrix = new float[16];
        Matrix.setIdentityM(matrix, 0);
        Matrix.setRotateM(matrix, 0, angle, getxPos(), getyPos(), getzPos());

        float[] newUp = new float[4];
        Matrix.multiplyMV(newUp, 0, matrix, 0, up, 0);
        Math3DUtils.normalizeVector(newUp);

        this.up[0] = newUp[0];
        this.up[1] = newUp[1];
        this.up[2] = newUp[2];

        setChanged(true);
    }

    public float[] getViewMatrix() {
        // If this camera is attached to a node in the scene graph...
        if (this.node != null) {
            // Get the node's current world-space transformation matrix
            float[] nodeTransform = this.node.getAnimatedWorldTransform();
            if (nodeTransform == null){
                nodeTransform = this.node.getWorldTransform();
            }

            // A camera's view matrix is the INVERSE of its world transform.
            if (nodeTransform != null) {
                Matrix.invertM(viewMatrix, 0, nodeTransform, 0);
            }
        } else {
            Matrix.setLookAtM(this.viewMatrix, 0, getxPos(), getyPos(), getzPos(),
                    getxView(), getyView(), getzView(), getxUp(), getyUp(), getzUp());
        }

        return viewMatrix;
    }

    public float[] getProjectionMatrix() {
        return projection.getMatrix();
    }

    @NonNull
    @Override
    public String toString() {
        return "Camera [projection="+projection+", xPos=" + pos[0] + ", yPos=" + pos[1] + ", zPos=" + pos[2] +
                ", xView=" + getxView() + ", yView=" + getyView() +
                ", zView=" + view[2] + ", xUp=" + getxUp() +
                ", yUp=" + getyUp() + ", zUp=" + getzUp() + "]";
    }
}
