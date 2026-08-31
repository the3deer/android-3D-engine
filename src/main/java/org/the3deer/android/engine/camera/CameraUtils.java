package org.the3deer.android.engine.camera;

import org.the3deer.android.engine.model.BoundingBox;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.OrthographicProjection;
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
        frameModel(camera, objects, false);
    }

    /**
     * Frames the model(s) in the camera's view.
     *
     * @param camera         the camera to position
     * @param objects        the objects to frame
     * @param worldRelative if true, frame relative to world origin (0,0,0)
     */
    public static void frameModel(Camera camera, List<Object3D> objects, boolean worldRelative) {
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
            final BoundingBox currentBoundingBox = obj.getCurrentBoundingBox();

            float objMinX = currentBoundingBox.getxMin();
            float objMaxX = currentBoundingBox.getxMax();
            float objMinY = currentBoundingBox.getyMin();
            float objMaxY = currentBoundingBox.getyMax();
            float objMinZ = currentBoundingBox.getzMin();
            float objMaxZ = currentBoundingBox.getzMax();

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
        final float centerX;
        final float centerY;
        final float centerZ;
        final float radius;

        if (worldRelative) {
            centerX = centerY = centerZ = 0;
            // Radius is the max distance from any corner of the AABB to the origin
            float maxDistSq = 0;
            for (float x : new float[]{minX, maxX}) {
                for (float y : new float[]{minY, maxY}) {
                    for (float z : new float[]{minZ, maxZ}) {
                        maxDistSq = Math.max(maxDistSq, x * x + y * y + z * z);
                    }
                }
            }
            radius = (float) Math.sqrt(maxDistSq);
        } else {
            centerX = (minX + maxX) / 2.0f;
            centerY = (minY + maxY) / 2.0f;
            centerZ = (minZ + maxZ) / 2.0f;

            float dx = maxX - minX;
            float dy = maxY - minY;
            float dz = maxZ - minZ;
            radius = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) / 2.0f;
        }

        // 3. Calculate distance based on FOV
        Projection projection = camera.getProjection();
        float fov = (projection != null && projection.getFov() > 0) ? projection.getFov() : 60.0f;
        float ratio = (projection != null && projection.getAspectRatio() > 0) ? projection.getAspectRatio() : 1.0f;

        // distance = radius / sin(fov / 2)
        // Adding 20% padding for a comfortable view
        double halfFovRad = Math.toRadians(fov / 2.0);
        float distanceV = (float) (radius / Math.sin(halfFovRad));

        // Horizontal FOV distance
        double halfFovHRad = Math.atan(Math.tan(halfFovRad) * ratio);
        float distanceH = (float) (radius / Math.sin(halfFovHRad));

        float distance = Math.max(distanceV, distanceH) * 1.2f;

        // --- ATOMIC AVOCADO / TINY MODEL HANDLING ---
        if (radius < 0.1f) {
            distance = distance * 0.8f; // Get a bit closer for tiny things
        }

        logger.info("Framing model: center=(" + centerX + "," + centerY + "," + centerZ + "), radius="+radius+", distance=" + distance);

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

        logger.info("- New Camera position: "+newPosX+","+newPosY+","+newPosZ);

        camera.set(newPosX, newPosY, newPosZ, centerX, centerY, centerZ, 0, 1, 0);

        // Update orthographic magnification if applicable
        if (projection instanceof OrthographicProjection) {
            final OrthographicProjection ortho = (OrthographicProjection) projection;
            final float mag = (float) (distance * Math.tan(halfFovRad));
            ortho.setXmag(mag);
            ortho.setYmag(mag);
        }

        // 5. Update Projection Clipping Planes (MUST happen after camera is positioned)
        updateProjection(camera, centerX, centerY, centerZ, radius);
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

        // --- ENHANCED CLIPPING PLANE LOGIC ---
        // We want to see the model, the background (skybox), and be able to zoom in close.
        final float safeRadius = Math.max(radius, 1.0f);

        // Far plane should be enough to see the back of the model and any environment (skybox).
        // We use a generous factor of the model's radius to avoid back-clipping background elements.
        float suggestedFar = distance + safeRadius * 20.0f;
        
        // Near plane should be as small as possible to allow close inspection.
        float suggestedNear = 0.01f;

        // --- HARDWARE-AWARE DEPTH PRECISION ---
        // Typical modern hardware (24-bit depth) handles a ratio of 10,000:1 well.
        // Old hardware (16-bit) might need 1,000:1.
        final float maxHealthyRatio = 10000f;

        if (suggestedFar / suggestedNear > maxHealthyRatio) {
            // If the ratio is too high, we must increase 'near' to preserve depth precision.
            suggestedNear = suggestedFar / maxHealthyRatio;
        }

        // Final safety check: near and far must be positive and far > near
        suggestedNear = Math.max(suggestedNear, 0.001f);
        suggestedFar = Math.max(suggestedFar, suggestedNear + 1.0f);

        // Apply to projection
        projection.setNear(suggestedNear);
        projection.setFar(suggestedFar);

        //logger.info("- Scene projection updated: near=" + suggestedNear + ", far=" + suggestedFar);
    }

    /**
     * Calculates the focal distance (convergence point) for stereoscopic rendering.
     * It uses the camera's controller target if available, or falls back to a safe default.
     * @param camera the camera to calculate focal distance for
     * @param scene the active scene to derive scaling from
     * @return the focal distance in world units
     */
    public static float getFocalDistance(Camera camera, Scene scene) {
        if (camera == null) return 1.0f;

        float focalDistance = 1.0f;
        if (camera.getController() != null && camera.getController().getTarget() != null) {
            final float[] target = camera.getController().getTarget();
            final float[] pos = camera.getPos();
            focalDistance = (float) Math.sqrt(
                    Math.pow(pos[0] - target[0], 2) +
                    Math.pow(pos[1] - target[1], 2) +
                    Math.pow(pos[2] - target[2], 2)
            );
        }

        // Safety floor: Prevent extreme eye convergence when camera is too close to the target.
        // We use a small fraction of the scene's radius to ensure a comfortable 3D effect.
        final float sceneRadius = (scene != null && scene.getDimensions() != null) ? scene.getDimensions().getRadius() : 10.0f;
        return Math.max(focalDistance, sceneRadius * 0.1f);
    }

    /**
     * Calculates the appropriate eye distance for stereoscopic rendering based on scene scale.
     * @param scene the active scene
     * @return scaled eye distance
     */
    public static float getEyeDistance(Scene scene) {
        if (scene == null || scene.getDimensions() == null) {
            return org.the3deer.android.engine.model.Constants.EYE_DISTANCE;
        }

        final float radius = scene.getDimensions().getRadius();
        
        // Dynamic Eye Separation: 
        // For very small models, we must reduce the eye distance to prevent divergence.
        // A ratio of 1/30th of the model's radius provides a natural sense of depth.
        // We cap it at the human standard (Constants.EYE_DISTANCE) for large models.
        return Math.min(org.the3deer.android.engine.model.Constants.EYE_DISTANCE, Math.max(0.001f, radius / 30.0f));
    }
}
