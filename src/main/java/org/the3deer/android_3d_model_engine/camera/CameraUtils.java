package org.the3deer.android_3d_model_engine.camera;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Projection;

import java.util.List;

/**
 * @author Gemini AI
 */
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

            // Use the object's world-space bounding box
            float objMinX = obj.getCurrentBoundingBox().getxMin();
            float objMaxX = obj.getCurrentBoundingBox().getxMax();
            float objMinY = obj.getCurrentBoundingBox().getyMin();
            float objMaxY = obj.getCurrentBoundingBox().getyMax();
            float objMinZ = obj.getCurrentBoundingBox().getzMin();
            float objMaxZ = obj.getCurrentBoundingBox().getzMax();

            minX = Math.min(minX, objMinX);
            maxX = Math.max(maxX, objMaxX);
            minY = Math.min(minY, objMinY);
            maxY = Math.max(maxY, objMaxY);
            minZ = Math.min(minZ, objMinZ);
            maxZ = Math.max(maxZ, objMaxZ);
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
        
        // Sphere radius that encloses the box
        float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;

        // 3. Calculate distance based on FOV
        Projection projection = camera.getProjection();
        float fov = (projection != null) ? projection.getFov() : 60.0f; 

        // distance = radius / sin(fov / 2)
        // We add padding (1.2f) to give some extra space
        double halfFovRad = Math.toRadians(fov / 2.0);
        float distance = (float) (radius / Math.sin(halfFovRad)) * 1.2f;

        Log.i(TAG, "Framing model: center=(" + centerX + "," + centerY + "," + centerZ + "), radius=" + radius + ", distance=" + distance);

        // 4. Position the camera
        // Keep current direction if possible, otherwise look from a default direction (e.g. along Z axis)
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
