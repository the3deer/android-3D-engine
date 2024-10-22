package org.the3deer.android_3d_model_engine.camera;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.util.math.Math3DUtils;

import javax.inject.Inject;

public class POVHandler implements CameraController.Handler {

    @Inject
    private Camera camera;

    private float[] savePos;
    private float[] saveUp;
    private float[] saveView;

    public void setUp() {
        this.savePos = camera.getPos().clone();
        this.saveUp = camera.getUp().clone();
        this.saveView = camera.getView().clone();
    }

    @Override
    public void enable(){
        camera.setController(this);
        camera.setProjection(Projection.PERSPECTIVE);
        camera.setChanged(true);
        saveAndAnimate(this.savePos[0], this.savePos[1], this.savePos[2], this.saveUp[0], this.saveUp[1], this.saveUp[2],
                this.saveView[0], this.saveView[1], this.saveView[2]);
    }

    private void save(){
        System.arraycopy(camera.getPos(), 0, this.savePos, 0, camera.getPos().length);
        System.arraycopy(camera.getView(), 0, this.saveView, 0, camera.getView().length);
        System.arraycopy(camera.getUp(), 0, this.saveUp, 0, camera.getUp().length);
    }

    @Override
    public void move(float dX, float dY) {

        if (dX == 0 && dY == 0) return;

        // get current view and right
        float[] view = Math3DUtils.to4d(Math3DUtils.substract(camera.getView(), camera.getPos()));
        float[] right = Math3DUtils.to4d(Math3DUtils.crossProduct(view, camera.getUp()));
        if (Math3DUtils.length(right) == 0) return;

        Math3DUtils.normalizeVector(right);

        // add deltas
        float[] rightd = Math3DUtils.multiply(right, dY);
        float[] upd = Math3DUtils.multiply(camera.getUp(), dX);

        // rot vectors
        float[] viewRot = Math3DUtils.add(rightd,upd);
        float length = Math3DUtils.length(viewRot);
        Math3DUtils.normalizeVector(viewRot);

        // transform
        float[] matrixView = new float[16];
        Matrix.setIdentityM(matrixView,0);
        Matrix.translateM(matrixView,0, camera.getxPos(), camera.getyPos(), camera.getzPos());
        Matrix.rotateM(matrixView, 0, -(float) Math.toDegrees(length), viewRot[0], viewRot[1], viewRot[2]);

        final float[] newView = new float[4];
        Matrix.multiplyMV(newView,0,matrixView,0, view,0);
        camera.getView()[0] = newView[0];
        camera.getView()[1] = newView[1];
        camera.getView()[2] = newView[2];

        // ------------------------

        float[] matrixUp = new float[16];
        Matrix.setIdentityM(matrixUp,0);
        Matrix.rotateM(matrixUp, 0, -(float) Math.toDegrees(length), viewRot[0], viewRot[1], viewRot[2]);

        float[] newUp = new float[4];
        Matrix.multiplyMV(newUp,0,matrixUp,0, camera.getUp(),0);
        Math3DUtils.normalizeVector(newUp);

        camera.getUp()[0] = newUp[0];
        camera.getUp()[1] = newUp[1];
        camera.getUp()[2] = newUp[2];

        camera.setChanged(true);
    }

    public  synchronized void zoom(float direction) {

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

        camera.getView()[0] += xLookDirection * direction;
        camera.getView()[1] += yLookDirection * direction;
        camera.getView()[2] += zLookDirection * direction;

        save();

        camera.setChanged(true);
    }

    @Override
    public void rotate(float angle) {

        if (angle == 0) return;

        // get current view and right
        float[] view = Math3DUtils.to4d(Math3DUtils.substract(camera.getView(), camera.getPos()));
        Math3DUtils.normalizeVector(view);

        // transform
        float[] matrix = new float[16];
        Matrix.setRotateM(matrix, 0, (float) -Math.toDegrees(angle), view[0], view[1], view[2]);

        final float[] newUp = new float[4];
        Matrix.multiplyMV(newUp,0,matrix,0, camera.getUp(),0);
        camera.getUp()[0] = newUp[0];
        camera.getUp()[1] = newUp[1];
        camera.getUp()[2] = newUp[2];

        save();

        camera.setChanged(true);
    }

    private void saveAndAnimate(float xp, float yp, float zp,
                                float xu, float yu, float zu,
                                float xv, float yv, float zv) {

            Object[] args = new Object[]{"moveTo", camera.getxPos(), camera.getyPos(), camera.getzPos(),
                    camera.getxUp(), camera.getyUp(), camera.getzUp(),
                    xp, yp, zp, xu, yu, zu, camera.getxView(), camera.getyView(), camera.getzView(), xv, yv, zv};

            savePos[0] = xp;
            savePos[1] = yp;
            savePos[2] = zp;
            saveUp[0] = xu;
            saveUp[1] = yu;
            saveUp[2] = zu;
            saveView[0] = xv;
            saveView[1] = yv;
            saveView[2] = zv;

            //delegate.setAnimation(new CameraAnimation(delegate, args));

    }
}
