package org.the3deer.engine.camera;

import org.the3deer.engine.Model;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Projection;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.math.Math3DUtils;

import javax.inject.Inject;
import javax.inject.Named;

public class DefaultCameraHandler implements CameraController.CameraHandler {

    @Inject
    private Model model;

    @Inject
    @Named("perspectiveProjection")
    private Projection projection;

    @BeanInit
    public void setUp() {
    }

    @Override
    public void enable(){
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;
        camera.setController(this);
        camera.setProjection(projection);
        camera.setChanged(true);
    }

    @Override
    public void move(float dX, float dY) {
        translateCameraImpl(dX, dY);
    }

    private void translateCameraImpl(float dX, float dY) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;

        float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);
        
        float[] right = Math3DUtils.crossProduct(camera.getUp(), camera.getPos());
        Math3DUtils.normalizeVector(right);

        // Correct world-space UP for the camera rotation
        float[] up = Math3DUtils.crossProduct(look, right);
        Math3DUtils.normalizeVector(up);

        final float[] coordinates = new float[12];
        coordinates[0] = camera.getxPos(); coordinates[1] = camera.getyPos(); coordinates[2] = camera.getzPos(); coordinates[3] = 1;
        coordinates[4] = camera.getxView(); coordinates[5] = camera.getyView(); coordinates[6] = camera.getzView(); coordinates[7] = 1;
        coordinates[8] = camera.getxUp(); coordinates[9] = camera.getyUp(); coordinates[10] = camera.getzUp(); coordinates[11] = 1;

        final float[] buffer = new float[16];
        if (dX != 0 && dY != 0) {
            float[] rotAxis = Math3DUtils.add(Math3DUtils.multiply(right, dY), Math3DUtils.multiply(up, dX));
            float length = Math3DUtils.length(rotAxis);
            Math3DUtils.normalizeVector(rotAxis);
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, length, rotAxis[0], rotAxis[1], rotAxis[2]);
        } else if (dX != 0) {
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, dX, up[0], up[1], up[2]);
        } else {
            Math3DUtils.createRotationMatrixAroundVector(buffer, 0, dY, right[0], right[1], right[2]);
        }

        float[] newBuffer = new float[12];
        Math3DUtils.multiplyMMV(newBuffer, 0, buffer, 0, coordinates, 0);

        if (camera.isOutOfBounds(newBuffer[0], newBuffer[1], newBuffer[2])) return;

        camera.getPos()[0] = newBuffer[0]; camera.getPos()[1] = newBuffer[1]; camera.getPos()[2] = newBuffer[2];
        camera.getUp()[0] = newBuffer[8]; camera.getUp()[1] = newBuffer[9]; camera.getUp()[2] = newBuffer[10];
        Math3DUtils.normalizeVector(camera.getUp());
        camera.setChanged(true);
    }

    @Override
    public synchronized void zoom(float direction) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;

        float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);

        float x = camera.getPos()[0] + look[0] * direction;
        float y = camera.getPos()[1] + look[1] * direction;
        float z = camera.getPos()[2] + look[2] * direction;

        if (camera.isOutOfBounds(x, y, z)) return;

        camera.getPos()[0] = x; camera.getPos()[1] = y; camera.getPos()[2] = z;
        camera.setChanged(true);
    }

    @Override
    public synchronized void rotate(float angle) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;

        float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        Math3DUtils.normalizeVector(look);

        final float[] buffer = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(buffer, 0, angle, look[0], look[1], look[2]);

        final float[] coordinates= new float[]{
            camera.getxPos(), camera.getyPos(), camera.getzPos(), 1,
            camera.getxView(), camera.getyView(), camera.getzView(), 1,
            camera.getxUp(), camera.getyUp(), camera.getzUp(), 1
        };

        float[] newBuffer = new float[12];
        Math3DUtils.multiplyMMV(newBuffer, 0, buffer, 0, coordinates, 0);

        camera.getPos()[0] = newBuffer[0]; camera.getPos()[1] = newBuffer[1]; camera.getPos()[2] = newBuffer[2];
        camera.getUp()[0] = newBuffer[8]; camera.getUp()[1] = newBuffer[9]; camera.getUp()[2] = newBuffer[10];
        camera.setChanged(true);
    }

    @Override
    public synchronized void pan(float dX, float dY) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;

        float[] look = Math3DUtils.substract(camera.getView(), camera.getPos());
        float dist = Math3DUtils.length(look);
        Math3DUtils.normalizeVector(look);
        
        float[] right = Math3DUtils.crossProduct(camera.getUp(), camera.getPos());
        Math3DUtils.normalizeVector(right);

        // FIX: Inverting cross product to get a true UP vector relative to camera look
        float[] up = Math3DUtils.crossProduct(right, look);
        Math3DUtils.normalizeVector(up);

        float sensitivity = dist * 0.0015f;
        
        float[] shiftX = Math3DUtils.multiply(right, dX * sensitivity);
        float[] shiftY = Math3DUtils.multiply(up, dY * sensitivity);
        float[] totalShift = Math3DUtils.add(shiftX, shiftY);

        camera.getPos()[0] += totalShift[0];
        camera.getPos()[1] += totalShift[1];
        camera.getPos()[2] += totalShift[2];

        camera.getView()[0] += totalShift[0];
        camera.getView()[1] += totalShift[1];
        camera.getView()[2] += totalShift[2];

        camera.setChanged(true);
    }
}
