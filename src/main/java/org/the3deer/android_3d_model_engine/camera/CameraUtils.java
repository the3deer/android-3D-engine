package org.the3deer.android_3d_model_engine.camera;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;

import java.util.List;

public class CameraUtils {

    private static final String TAG = CameraUtils.class.getSimpleName();

    /**
     * Frames the model(s) in the camera's view.
     * Calculates the bounding sphere of all provided objects and positions the camera
     * at a distance that ensures the entire sphere is visible.
     *
     * @param camera  the camera to position
     * @param objects the objects to frame
     */
    public static void frameModel(Camera camera, List<Object3DData> objects) {
        if (camera == null || objects == null || objects.isEmpty()) {
            return;
        }

        // 1. Calculate the global bounding box in world space
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (Object3DData obj : objects) {
            if (obj.isDecorator()) continue;

            float[][] corners = obj.getBoundingBox().getCorners();
            float[] modelMatrix = obj.getModelMatrix();
            float[] worldCorner = new float[4];

            for (float[] corner : corners) {
                float[] corner4 = new float[]{corner[0], corner[1], corner[2], 1.0f};
                Matrix.multiplyMV(worldCorner, 0, modelMatrix, 0, corner4, 0);

                minX = Math.min(minX, worldCorner[0]);
                minY = Math.min(minY, worldCorner[1]);
                minZ = Math.min(minZ, worldCorner[2]);
                maxX = Math.max(maxX, worldCorner[0]);
                maxY = Math.max(maxY, worldCorner[1]);
                maxZ = Math.max(maxZ, worldCorner[2]);
            }
        }

        if (minX == Float.MAX_VALUE) {
            Log.w(TAG, "Could not calculate bounding box for framing.");
            return;
        }

        // 2. Calculate center and radius of the bounding sphere
        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;

        float dx = maxX - minX;
        float dy = maxY - minY;
        float dz = maxZ - minZ;
        
        // Use the furthest corner from the center to get the radius of the bounding sphere
        float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;

        // 3. Calculate distance based on FOV
        // Assuming a standard 60 degree vertical FOV. 
        // In a more complex implementation, we'd pull this from the camera's Projection.
        float fov = 60.0f; 

        // distance = radius / sin(fov / 2)
        // We add a small multiplier (e.g. 1.1) to give some padding around the model.
        double halfFovRad = Math.toRadians(fov / 2.0);
        float distance = (float) (radius / Math.sin(halfFovRad)) * 1.1f;

        Log.i(TAG, "Framing model: center=(" + centerX + "," + centerY + "," + centerZ + "), radius=" + radius + ", distance=" + distance);

        // 4. Position the camera
        // We look at the model from its "front" (along the -Z axis relative to the model's center)
        // but we'll try to preserve the existing look direction if possible.
        float[] lookDir = new float[]{
                camera.getView()[0] - camera.getPos()[0],
                camera.getView()[1] - camera.getPos()[1],
                camera.getView()[2] - camera.getPos()[2]
        };
        float lookLen = (float) Math.sqrt(lookDir[0] * lookDir[0] + lookDir[1] * lookDir[1] + lookDir[2] * lookDir[2]);

        if (lookLen < 0.0001f) {
            lookDir = new float[]{0, 0, -1}; // Default look direction
        } else {
            lookDir[0] /= lookLen;
            lookDir[1] /= lookLen;
            lookDir[2] /= lookLen;
        }

        // Set camera position at 'distance' away from the center, looking at the center
        float newPosX = centerX - lookDir[0] * distance;
        float newPosY = centerY - lookDir[1] * distance;
        float newPosZ = centerZ - lookDir[2] * distance;

        camera.set(newPosX, newPosY, newPosZ, centerX, centerY, centerZ, 0, 1, 0);
    }
}
