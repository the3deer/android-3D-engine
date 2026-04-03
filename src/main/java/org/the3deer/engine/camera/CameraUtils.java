package org.the3deer.engine.camera;

import android.util.Log;

import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Projection;

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
    public static void frameModel(Camera camera, List<Object3D> objects) {
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

        for (Object3D obj : objects) {
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
        
        // Sphere radius that perfectly encloses the AABB
        float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;

        // 3. Calculate distance based on FOV
        Projection projection = camera.getProjection();
        float fov = (projection != null) ? projection.getFov() : 60.0f; 

        // distance = radius / sin(fov / 2)
        // Adding 20% padding for a comfortable view
        double halfFovRad = Math.toRadians(fov / 2.0);
        float distance = (float) (radius / Math.sin(halfFovRad)) * 1.2f;

        // --- ATOMIC AVOCADO / TINY MODEL HANDLING ---
        if (radius < 0.1f) {
            distance = distance * 0.8f; // Get a bit closer for tiny things
        }

        // --- ROBUST CLIPPING PLANE LOGIC ---
        // Avoid "gaps" (Z-fighting) by maintaining a healthy Near/Far ratio.
        if (projection != null) {
            
            // We set near to a small fraction of the distance. 
            // This provides plenty of room to zoom in before clipping starts.
            float suggestedNear = distance * 0.05f;
            
            // Floors to avoid numerical instability
            float floor = (radius < 0.1f) ? 0.0001f : 0.01f;
            suggestedNear = Math.max(suggestedNear, floor);
            
            // Set far plane to capture the whole model plus plenty of headroom
            float suggestedFar = distance + radius * 50.0f;
            
            // The Golden Ratio: Keep Far/Near <= 10,000 to prevent depth buffer "gaps"
            if (suggestedFar / suggestedNear > 10000f) {
                suggestedFar = suggestedNear * 10000f;
            }

            projection.setNear(suggestedNear);
            projection.setFar(suggestedFar);
            
            Log.i(TAG, "Dynamic projection: near=" + suggestedNear + ", far=" + suggestedFar + " (Ratio: " + (suggestedFar/suggestedNear) + ")");
        }

        Log.i(TAG, "Framing model: center=(" + centerX + "," + centerY + "," + centerZ + "), distance=" + distance);

        // 4. Position the camera
        float[] lookDir = new float[]{
                camera.getView()[0] - camera.getPos()[0],
                camera.getView()[1] - camera.getPos()[1],
                camera.getView()[2] - camera.getPos()[2]
        };
        float lookLen = (float) Math.sqrt(lookDir[0] * lookDir[0] + lookDir[1] * lookDir[1] + lookDir[2] * lookDir[2]);

        if (lookLen < 0.0001f) {
            lookDir = new float[]{0, 0, -1};
        } else {
            lookDir[0] /= lookLen;
            lookDir[1] /= lookLen;
            lookDir[2] /= lookLen;
        }

        float newPosX = centerX - lookDir[0] * distance;
        float newPosY = centerY - lookDir[1] * distance;
        float newPosZ = centerZ - lookDir[2] * distance;

        camera.set(newPosX, newPosY, newPosZ, centerX, centerY, centerZ, 0, 1, 0);
    }
}
