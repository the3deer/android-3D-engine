package org.the3deer.android_3d_model_engine.model;

// http://stackoverflow.com/questions/14607640/rotating-a-vector-in-3d-space

import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;

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
    }

    private final BoundingBox centerBox = new BoundingBox("scene", -Constants.ROOM_CENTER_SIZE, Constants.ROOM_CENTER_SIZE,
            -Constants.ROOM_CENTER_SIZE, Constants.ROOM_CENTER_SIZE, -Constants.ROOM_CENTER_SIZE, Constants.ROOM_CENTER_SIZE);
    private final BoundingBox roomBox = new BoundingBox("scene", -Constants.ROOM_SIZE, Constants.ROOM_SIZE,
            -Constants.ROOM_SIZE, Constants.ROOM_SIZE, -Constants.ROOM_SIZE, Constants.ROOM_SIZE);

    // new vector model
    protected float[] pos = new float[]{0, 0, 1, 1};
    protected float[] view = new float[]{0, 0, 0, 1};
    protected float[] up = new float[]{0, 1, 0, 1};

    // transformation matrix
    public float[] viewMatrix = new float[16];

    // camera mode
    @Inject
    private Projection projection;

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

    @Inject
    private Screen screen;

    //@Inject
    private Controller controller;

    private boolean changed = false;

    private Dimensions dimensions2D = new Dimensions();
    private Dimensions dimensions3D = new Dimensions();

    // stereoscopic handlers
    private Camera[] stereoCam;

    public Camera() {
    }

    public Camera(float[] location) {
        // Initialize variables...
        this(location[0], location[1], location[2], 0, 0, 0, 0, 1, 0);
    }

    public Camera(Camera cam2) {
        this.screen = cam2.screen;
        this.pos = cam2.pos;
        this.view = cam2.view;
        this.up = cam2.up;
        this.viewMatrix = cam2.viewMatrix;
        this.projection = cam2.projection;
    }

    public Camera(float xPos, float yPos, float zPos, float xView, float yView, float zView, float xUp, float yUp,
                  float zUp) {

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

    protected void refresh() {

        // check
        if (screen == null) return;

        //Log.v("Camera", "location: "+ Arrays.toString(this.pos));

        // setup view matrix
        Matrix.setLookAtM(viewMatrix, 0,
                getxPos(), getyPos(), getzPos(),
                getxView(), getyView(), getzView(),
                getxUp(), getyUp(), getzUp());


        // update orientation
        Matrix.setLookAtM(this.orientationMatrix, 0,
                0, 0, 0,
                -getxPos() + getxView(), -getyPos() + getyView(), -getzPos() + getzView(),
                getxUp(), getyUp(), getzUp());
        this.orientation.setMatrix(orientationMatrix);

        // dimensions
        this.dimensions2D.set(-Constants.UNIT * screen.getRatio(), Constants.UNIT * screen.getRatio(), Constants.UNIT, -Constants.UNIT, 0, 1);
        this.dimensions3D.set(0, screen.getWidth(), screen.getHeight(), 0, Constants.near, Constants.far);

        // projection
        if (this.projection != null) {
            this.projection.refresh();
        }

        //Log.v("Camera","Camera refreshed: "+this.projection);
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
            Log.v("Camera", "Out of room walls. " + x + "," + y + "," + z);
            return true;
        }
        if (!centerBox.outOfBound(x, y, z)) {
            Log.v("Camera", "Inside absolute center");
            return true;
        }
        return false;
    }

    /**
     * Translation is the movement that makes the Earth around the Sun.
     * So in this context, translating the camera means moving the camera around the Zero (0,0,0)
     * <p>
     * This implementation makes uses of 3D Vectors Algebra.
     * <p>
     * The idea behind this implementation is to translate the 2D user vectors (the line in the
     * screen) with the 3D equivalents.
     * <p>
     * In order to to that, we need to calculate the Right and Arriba vectors so we have a match
     * for user 2D vector.
     *
     * @param dX the X component of the user 2D vector, that is, a value between [-1,1]
     * @param dY the Y component of the user 2D vector, that is, a value between [-1,1]
     */
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
            stereoCam = new Camera[]{new Camera(), new Camera()};
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
        float xViewLeft = xConv - xPosLeft;
        float yViewLeft = yConv - yPosLeft;
        float zViewLeft = zConv - zPosLeft;

        // new right pos
        float xPosRight = pos[0] + crossRight[0] * midEye;
        float yPosRight = pos[1] + crossRight[1] * midEye;
        float zPosRight = pos[2] + crossRight[2] * midEye;
        float xViewRight = xConv - xPosRight;
        float yViewRight = yConv - yPosRight;
        float zViewRight = zConv - zPosRight;

        // update left
        final Camera left = stereoCam[0];
        left.screen = this.screen;
        left.projection = this.projection;
        left.set(xPosLeft, yPosLeft, zPosLeft, xViewLeft, yViewLeft, zViewLeft, getxUp(), getyUp(), getzUp());
        //left.refresh();

        // update right
        final Camera right = stereoCam[1];
        right.screen = this.screen;
        right.projection = this.projection;
        right.set(xPosRight, yPosRight, zPosRight, xViewRight, yViewRight, zViewRight, getxUp(), getyUp(), getzUp());
        //right.refresh();

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

        // check that we have valid coordinates


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

        float dX = (float) Math.sin(Math.toRadians(angle));
        float dY = (float) Math.cos(Math.toRadians(angle));
        float[] right = Math3DUtils.to4d(getRight());

        // screen orientation vector
        /*float[] ovector1 = Math3DUtils.multiply(up, dX);
        float[] ovector2 = Math3DUtils.multiply(getRight(), -dY);
        float[] ovector = Math3DUtils.add(ovector1,ovector2);
        Math3DUtils.normalize(ovector);*/

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

        /*float rota = -(float) Math.tan(angle-180)/5f; //-angle / 90f;

        float dot = Math.abs(Math3DUtils.dotProduct(up, newUp));
        if (Math.abs(dot) < 0.1f){
            Log.v("Camera","angle: "+angle+", rot:"+rota+", dx:"+dX+", dy:"+dY+" HIT! "+Math3DUtils.dotProduct(up,newUp));
            return;
        } else {
            Log.v("Camera","angle: "+angle+", rot:"+rota+", dx:"+dX+", dy:"+dY+" DOT! "+Math3DUtils.dotProduct(up,newUp));
            //return;
        }

        Matrix.setIdentityM(matrix,0);
        Matrix.setRotateM(matrix,0, -angle/90f, getxPos(),getyPos(),getzPos());
        Matrix.multiplyMV(newUp,0,matrix,0,up,0);
        Math3DUtils.normalize(newUp);

        this.up[0] = newUp[0];
        this.up[1] = newUp[1];
        this.up[2] = newUp[2];
        setChanged(true);*/
    }

    public float[] getViewMatrix() {
        /*Matrix.setLookAtM(this.viewMatrix,0,getxPos(),getyPos(),getzPos(),
                getxView(),getyView(),getzView(),getxUp(),getyUp(),getzUp());*/
        return viewMatrix;
    }

    public float[] getProjectionMatrix() {
        return projection.getMatrix();
    }

/*    public float[] getProjectionViewMatrix() {
        return projectionViewMatrix;
    }*/

    // from gui

    public Dimensions getDimensions2D() {
        return dimensions2D;
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
