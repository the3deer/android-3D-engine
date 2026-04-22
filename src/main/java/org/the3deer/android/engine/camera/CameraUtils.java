package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Projection;
import org.the3deer.android.engine.model.Scene;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Gemini AI
 */
public class CameraUtils {

    private static final Logger logger = Logger.getLogger(CameraUtils.class.getSimpleName());

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
            logger.warning("Could not calculate bounding box for framing.");
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
        final float radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;

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

        // 4. Update Projection Clipping Planes
        updateProjection(camera, centerX, centerY, centerZ, radius);

        logger.info("Framing model: center=(" + centerX + "," + centerY + "," + centerZ + "), distance=" + distance);

        // 5. Position the camera
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

        logger.info("- New Camera position: "+newPosX+","+newPosY+","+newPosZ);

        camera.set(newPosX, newPosY, newPosZ, centerX, centerY, centerZ, 0, 1, 0);
    }

    /**
     * Updates the camera's projection clipping planes (near/far) based on the scene's current state.
     * This ensures the entire model is visible regardless of camera movement.
     *
     * @param camera the camera to update
     * @param scene the scene containing the model dimensions
     */
    public static void updateProjection(Camera camera, Scene scene) {
        if (camera == null || scene == null || scene.getDimensions() == null) {
            return;
        }

        final float[] center = scene.getDimensions().getCenter();
        final float radius = scene.getDimensions().getRadius();

        updateProjection(camera, center[0], center[1], center[2], radius);
    }

    /**
     * Updates the camera's projection clipping planes (near/far) based on camera position and target sphere.
     */
    public static void updateProjection(Camera camera, float centerX, float centerY, float centerZ, float radius) {
        final Projection projection = camera.getProjection();
        if (projection == null) return;

        // 1. Calculate Distance from Camera to Scene Center
        final float[] camPos = camera.getPos();
        final float distance = (float) Math.sqrt(
                Math.pow(camPos[0] - centerX, 2) +
                Math.pow(camPos[1] - centerY, 2) +
                Math.pow(camPos[2] - centerZ, 2)
        );

        // --- ROBUST CLIPPING PLANE LOGIC ---
        // We set near to a balanced fraction of the distance,
        // but we must ensure it's closer than the front of the model.
        float suggestedNear = (distance - radius) * 0.9f;
        float suggestedFar = distance + radius * 1.1f;

        // Minimum near plane to avoid numerical issues (0.1% of radius or 0.01)
        final float floor = Math.max(0.001f, radius * 0.001f);
        if (suggestedNear < floor) {
            suggestedNear = floor;
        }

        // --- HARDWARE-AWARE DEPTH PRECISION ---
        final int[] depthBits = new int[1];
        android.opengl.GLES20.glGetIntegerv(android.opengl.GLES20.GL_DEPTH_BITS, depthBits, 0);
        final float maxHealthyRatio = (depthBits[0] >= 24) ? 100000f : 10000f; // Increased for better handling

        if (suggestedFar / suggestedNear > maxHealthyRatio) {
            // If the ratio is too high, we must increase 'near' to preserve depth precision.
            suggestedNear = suggestedFar / maxHealthyRatio;
        }

        // Apply to projection
        projection.setNear(suggestedNear);
        projection.setFar(suggestedFar);

        // logger.finest("- Scene projection updated: near=" + suggestedNear + ", far=" + suggestedFar);
    }
}
