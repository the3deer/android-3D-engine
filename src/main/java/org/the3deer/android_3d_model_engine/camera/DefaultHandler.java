package org.the3deer.android_3d_model_engine.camera;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.util.math.Math3DUtils;

import javax.inject.Inject;

public class DefaultHandler implements CameraController.Handler {

    @Inject
    private Camera camera;

    // state
    private float[] savePos;
    private float[] saveView;
    private float[] saveUp;

    public void setUp() {
        this.savePos = camera.getPos().clone();
        this.saveView = new float[]{0,0,0,0};
        this.saveUp = camera.getUp().clone();
    }

    @Override
    public void enable(){
        camera.setController(this);
        camera.setProjection(Projection.PERSPECTIVE);
        camera.setChanged(true);
        /*delegate.setAnimation(new CameraAnimation(delegate, new Object[]{"moveTo", getxPos(), getyPos(), getzPos(), getxUp(), getyUp(), getzUp(),
                this.savePos[0], this.savePos[1], this.savePos[2], this.saveUp[0], this.saveUp[1], this.saveUp[2],
                this.getxView(), this.getyView(), this.getzView(), this.saveView[0], this.saveView[1], this.saveView[2]}));*/
    }

    private void save(){
        System.arraycopy(camera.getPos(), 0, this.savePos, 0, camera.getPos().length);
        System.arraycopy(camera.getUp(), 0, this.saveUp, 0, camera.getUp().length);
    }

    @Override
    public void move(float dX, float dY) {
        translateCameraImpl(dX, dY);
    }

    private void translateCameraImpl(float dX, float dY) {
        float vlen;

        // Translating the camera requires a directional vector to rotate
        // First we need to get the direction at which we are looking.
        // The look direction is the view minus the position (where we are).
        // Get the Direction of the view.
        float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);
        float xLook, yLook, zLook;
        xLook = look[0];
        yLook = look[1];
        zLook = look[2];

        // Arriba is the 3D vector that is **almost** equivalent to the 2D user Y vector
        // Get the direction of the up vector
        float[] arriba = camera.getUp().clone(); //Math3DUtils.substract(up,pos);
        Math3DUtils.normalizeVector(arriba);
        float xArriba, yArriba, zArriba;
        xArriba = arriba[0];
        yArriba = arriba[1];
        zArriba = arriba[2];

        // Right is the 3D vector that is equivalent to the 2D user X vector
        // In order to calculate the Right vector, we have to calculate the cross product of the
        // previously calculated vectors...

        // The cross product is defined like:
        // A x B = (a1, a2, a3) x (b1, b2, b3) = (a2 * b3 - b2 * a3 , - a1 * b3 + b1 * a3 , a1 * b2 - b1 * a2)
        float[] right = Math3DUtils.crossProduct(camera.getUp(), camera.getPos());
        Math3DUtils.normalizeVector(right);

        float xRight, yRight, zRight;
        xRight = (yLook * zArriba) - (zLook * yArriba);
        yRight = (zLook * xArriba) - (xLook * zArriba);
        zRight = (xLook * yArriba) - (yLook * xArriba);
        // Normalize the Right.
        vlen = Matrix.length(xRight, yRight, zRight);
        xRight /= vlen;
        yRight /= vlen;
        zRight /= vlen;

        // Once we have the Look & Right vector, we can recalculate where is the final Arriba vector,
        // so its equivalent to the user 2D Y vector.
        xArriba = (yRight * zLook) - (zRight * yLook);
        yArriba = (zRight * xLook) - (xRight * zLook);
        zArriba = (xRight * yLook) - (yRight * xLook);
        // Normalize the Right.
        vlen = Matrix.length(xArriba, yArriba, zArriba);
        xArriba /= vlen;
        yArriba /= vlen;
        zArriba /= vlen;

        // coordinates = new float[] { pos[0], pos[1], pos[2], 1, xView, yView, zView, 1, xUp, yUp, zUp, 1 };
        final float[] coordinates = new float[12];
        coordinates[0] = camera.getxPos();
        coordinates[1] = camera.getyPos();
        coordinates[2] = camera.getzPos();
        coordinates[3] = 1;
        coordinates[4] = camera.getxView();
        coordinates[5] = camera.getyView();
        coordinates[6] = camera.getzView();
        coordinates[7] = 1;
        coordinates[8] = camera.getxUp();
        coordinates[9] = camera.getyUp();
        coordinates[10] = camera.getzUp();
        coordinates[11] = 1;

        final float[] buffer = new float[16];

        if (dX != 0 && dY != 0) {

            // in this case the user is drawing a diagonal line:    \v     ^\    v/     /^
            // so, we have to calculate the perpendicular vector of that diagonal

            // The perpendicular vector is calculated by inverting the X/Y values
            // We multiply the initial Right and Arriba vectors by the User's 2D vector
            xRight *= dY;
            yRight *= dY;
            zRight *= dY;
            xArriba *= dX;
            yArriba *= dX;
            zArriba *= dX;

            float[] rightd = Math3DUtils.multiply(right, dY);
            float[] upd = Math3DUtils.multiply(camera.getUp(), dX);
            float[] rot = Math3DUtils.add(rightd,upd);
            float length = Math3DUtils.length(rot);
            Math3DUtils.normalizeVector(rot);

            // in this case we use the vlen angle because the diagonal is not perpendicular
            // to the initial Right and Arriba vectors
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, length, rot[0], rot[1], rot[2]);
        } else if (dX != 0) {
            // in this case the user is drawing an horizontal line: <-- ó -->
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, dX, xArriba, yArriba, zArriba);
        } else {
            // in this case the user is drawing a vertical line: |^  v|
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, dY, xRight, yRight, zRight);
        }

        float[] newBuffer = new float[12];
        Math3DUtils.multiplyMMV(newBuffer, 0, buffer, 0, coordinates, 0);

        if (camera.isOutOfBounds(newBuffer[0], newBuffer[1], newBuffer[2])) return;

        camera.getPos()[0] = newBuffer[0];
        camera.getPos()[1] = newBuffer[1];
        camera.getPos()[2] = newBuffer[2];
        //view[0] = newBuffer[4];
        //view[1] = newBuffer[4 + 1];
        //view[2] = newBuffer[4 + 2];
        camera.getUp()[0] = newBuffer[8];
        camera.getUp()[1] = newBuffer[8 + 1];
        camera.getUp()[2] = newBuffer[8 + 2];
        Math3DUtils.normalizeVector(camera.getUp());

        //Log.v("DefaultCamera","changing...");
        camera.setChanged(true);
        save();
    }

    public  synchronized void zoom(float direction) {
        //if (true) return;
        // Moving the camera requires a little more then adding 1 to the z or
        // subracting 1.
        // First we need to get the direction at which we are looking.
        float xLookDirection, yLookDirection, zLookDirection;

        // The look direction is the view minus the position (where we are).
        xLookDirection = camera.getxView() - camera.getPos()[0];
        yLookDirection = camera.getyView() - camera.getPos()[1];
        zLookDirection = camera.getView()[2] - camera.getPos()[2];

        // Normalize the direction.
        float dp = Matrix.length(xLookDirection, yLookDirection, zLookDirection);
        xLookDirection /= dp;
        yLookDirection /= dp;
        zLookDirection /= dp;

        float x = camera.getPos()[0] + xLookDirection * direction;
        float y = camera.getPos()[1] + yLookDirection * direction;
        float z = camera.getPos()[2] + zLookDirection * direction;


        if (camera.isOutOfBounds(x, y, z)) return;

        camera.getPos()[0] = x;
        camera.getPos()[1] = y;
        camera.getPos()[2] = z;

        save();

        camera.setChanged(true);
    }

    @Override
    public synchronized void rotate(float angle) {

        if (angle == 0 || Float.isNaN(angle)) {
            Log.w("DefaultCamera", "NaN");
            return;
        }
        float xLook = camera.getxView() - camera.getPos()[0];
        float yLook = camera.getyView() - camera.getPos()[1];
        float zLook = camera.getView()[2] - camera.getPos()[2];
        float vlen = Matrix.length(xLook, yLook, zLook);
        xLook /= vlen;
        yLook /= vlen;
        zLook /= vlen;

        final float[] buffer = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(buffer, 0, angle, xLook, yLook, zLook);
        // float[] coordinates = new float[] { xPos, pos[1], pos[2], 1, xView, yView, zView, 1, xUp, yUp, zUp, 1 };

        final float[] coordinates= new float[12];

        coordinates[0] = camera.getPos()[0];
        coordinates[1] = camera.getPos()[1];
        coordinates[2] = camera.getPos()[2];
        coordinates[3] = 1;
        coordinates[4] = camera.getxView();
        coordinates[5] = camera.getyView();
        coordinates[6] = camera.getView()[2];
        coordinates[7] = 1;
        coordinates[8] = camera.getxUp();
        coordinates[9] = camera.getyUp();
        coordinates[10] = camera.getzUp();
        coordinates[11] = 1;

        float[] newBuffer = new float[16];
        Math3DUtils.multiplyMMV(newBuffer, 0, buffer, 0, coordinates, 0);

        camera.getPos()[0] = newBuffer[0];
        camera.getPos()[1] = newBuffer[1];
        camera.getPos()[2] = newBuffer[2];
        //view[0] = buffer[4];
        //view[1] = buffer[4 + 1];
        //view[2] = buffer[4 + 2];
        camera.getUp()[0] = newBuffer[8];
        camera.getUp()[1] = newBuffer[8 + 1];
        camera.getUp()[2] = newBuffer[8 + 2];

        camera.setChanged(true);
        save();
    }
}
