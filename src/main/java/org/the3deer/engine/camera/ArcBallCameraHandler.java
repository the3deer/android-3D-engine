package org.the3deer.engine.camera;

import org.the3deer.engine.Model;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Projection;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.math.Math3DUtils;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Robust Arcball (Orbit) Camera implementation.
 * It rotates around a target point.
 * If an object is selected in the scene, it orbits around that object's center.
 * Otherwise, it orbits around the world center (0,0,0) or its current view point.
 */
public class ArcBallCameraHandler implements CameraController.CameraHandler {

    @Inject
    private Model model;

    @Inject
    @Named("perspectiveProjection")
    private Projection projection;

    @BeanInit
    public void setUp() {
    }

    @Override
    public void enable() {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;

        camera.setController(this);
        camera.setProjection(projection);
        camera.setChanged(true);
    }

    /**
     * Calculates the current target point for the camera.
     * @return The world-space center of the selected object, or (0,0,0) if none selected.
     */
    private float[] getTarget() {
        final Object3D selected = model.getActiveScene().getSelectedObject();
        if (selected == null) {
            return new float[]{0, 0, 0};
        }

        // Get the center of the object in its local space
        final float[] localCenter = selected.getDimensions().getCenter();
        // Transform the local center to world space using its final transform
        return Math3DUtils.transform(localCenter[0], localCenter[1], localCenter[2], selected.getFinalWorldTransform());
    }

    @Override
    public void move(final float dX, final float dY) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;
        if (dX == 0 && dY == 0) return;

        // Update camera's view point to follow selection target
        final float[] target = getTarget();
        camera.getView()[0] = target[0];
        camera.getView()[1] = target[1];
        camera.getView()[2] = target[2];

        final float[] pos = camera.getPos();
        final float[] view = camera.getView();
        final float[] up = camera.getUp();

        // 1. Calculate relative vector from target to camera
        final float[] relPos = Math3DUtils.substract(pos, view);

        // 2. Determine camera axes for rotation
        final float[] look = Math3DUtils.normalize2(Math3DUtils.negate(relPos));
        final float[] right = Math3DUtils.normalize2(Math3DUtils.crossProduct(look, up));
        final float[] cameraUp = Math3DUtils.crossProduct(right, look);

        // 3. Create rotation matrix (dX negated for intuitive "drag" feeling)
        final float[] rotMatrix = new float[16];
        if (dX != 0 && dY != 0) {
            final float[] rotAxis = Math3DUtils.add(Math3DUtils.multiply(right, dY), Math3DUtils.multiply(cameraUp, -dX));
            final float angle = Math3DUtils.length(rotAxis);
            Math3DUtils.normalizeVector(rotAxis);
            Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, angle, rotAxis[0], rotAxis[1], rotAxis[2]);
        } else if (dX != 0) {
            Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, -dX, cameraUp[0], cameraUp[1], cameraUp[2]);
        } else {
            Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, dY, right[0], right[1], right[2]);
        }

        // 4. Rotate relative position and up vector
        final float[] newRelPos = new float[4];
        Math3DUtils.multiplyMV(newRelPos, 0, rotMatrix, 0, new float[]{relPos[0], relPos[1], relPos[2], 1}, 0);

        final float[] newUp = new float[4];
        Math3DUtils.multiplyMV(newUp, 0, rotMatrix, 0, new float[]{up[0], up[1], up[2], 1}, 0);

        // 5. Apply changes
        final float nx = view[0] + newRelPos[0];
        final float ny = view[1] + newRelPos[1];
        final float nz = view[2] + newRelPos[2];

        if (camera.isOutOfBounds(nx, ny, nz)) return;

        pos[0] = nx; pos[1] = ny; pos[2] = nz;
        up[0] = newUp[0]; up[1] = newUp[1]; up[2] = newUp[2];
        Math3DUtils.normalizeVector(up);
        
        camera.setChanged(true);
    }

    @Override
    public void zoom(final float direction) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;
        if (direction == 0) return;

        // Ensure target is up to date
        final float[] target = getTarget();
        camera.getView()[0] = target[0];
        camera.getView()[1] = target[1];
        camera.getView()[2] = target[2];

        final float[] pos = camera.getPos();
        final float[] view = camera.getView();
        
        final float[] look = Math3DUtils.substract(view, pos);
        final float distance = Math3DUtils.length(look);
        Math3DUtils.normalizeVector(look);

        if (distance < 0.1f && direction > 0) return;

        final float nx = pos[0] + look[0] * direction;
        final float ny = pos[1] + look[1] * direction;
        final float nz = pos[2] + look[2] * direction;

        if (camera.isOutOfBounds(nx, ny, nz)) return;

        pos[0] = nx; pos[1] = ny; pos[2] = nz;
        camera.setChanged(true);
    }

    @Override
    public void pan(final float dX, final float dY) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;
        if (dX == 0 && dY == 0) return;

        // If an object is selected, we usually disable free panning to keep focus on the object.
        // Alternatively, we could allow panning but then subsequent rotations would snap back to center.
        // For a "gravitating" camera, it's better to keep the target locked to the selection.
        if (model.getActiveScene().getSelectedObject() != null) return;

        final float[] pos = camera.getPos();
        final float[] view = camera.getView();
        final float[] up = camera.getUp();

        final float[] look = Math3DUtils.normalize2(Math3DUtils.substract(view, pos));
        final float[] right = Math3DUtils.normalize2(Math3DUtils.crossProduct(look, up));
        final float[] cameraUp = Math3DUtils.crossProduct(right, look);

        final float distance = Math3DUtils.length(Math3DUtils.substract(view, pos));
        final float sensitivity = distance * 0.0015f;

        final float tx = -right[0] * dX * sensitivity + cameraUp[0] * dY * sensitivity;
        final float ty = -right[1] * dX * sensitivity + cameraUp[1] * dY * sensitivity;
        final float tz = -right[2] * dX * sensitivity + cameraUp[2] * dY * sensitivity;

        pos[0] += tx; pos[1] += ty; pos[2] += tz;
        view[0] += tx; view[1] += ty; view[2] += tz;

        camera.setChanged(true);
    }

    @Override
    public void rotate(final float angle) {
        final Camera camera = model.getActiveScene().getActiveCamera();
        if (camera == null) return;
        if (angle == 0) return;

        final float[] pos = camera.getPos();
        final float[] view = camera.getView();
        final float[] up = camera.getUp();

        final float[] look = Math3DUtils.normalize2(Math3DUtils.substract(view, pos));
        
        final float[] rotMatrix = new float[16];
        Math3DUtils.createRotationMatrixAroundVector(rotMatrix, 0, angle, look[0], look[1], look[2]);

        final float[] newUp = new float[4];
        Math3DUtils.multiplyMV(newUp, 0, rotMatrix, 0, new float[]{up[0], up[1], up[2], 1}, 0);

        up[0] = newUp[0]; up[1] = newUp[1]; up[2] = newUp[2];
        Math3DUtils.normalizeVector(up);

        camera.setChanged(true);
    }
}
