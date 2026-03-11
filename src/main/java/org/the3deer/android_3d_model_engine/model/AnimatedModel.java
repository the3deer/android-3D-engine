package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;

/**
 * This class represents an entity in the world that can be animated. It
 * contains the model's VAO which contains the mesh data, the texture, and the
 * root joint of the joint hierarchy, or "skeleton". It also holds an int which
 * represents the number of joints that the model's skeleton contains, and has
 * its own {@link org.the3deer.android_3d_model_engine.animation.Animator} instance which can be used to apply animations to
 * this entity.
 *
 * @author andresoviedo
 */
public class AnimatedModel extends Object3DData {

    // skeleton
    private Skin skin;

    public AnimatedModel() {
        super();
    }

    public AnimatedModel(FloatBuffer vertexBuffer) {
        super(vertexBuffer);
    }

    public AnimatedModel(FloatBuffer vertexBuffer, Buffer drawOrderBuffer) {
        super(vertexBuffer, drawOrderBuffer);
    }

    public AnimatedModel(String id, FloatBuffer positions, FloatBuffer normals, FloatBuffer colors, FloatBuffer texCoords, Material material, Skin skin) {
        super(id, positions, normals, texCoords, colors, material);
        setSkin(skin);
    }

    /**
     * Returns the definitive final world transform for this object, which can be used
     * to position and orient non-deforming helper objects like a Bounding Box.
     * <p>
     * This method correctly handles three distinct cases:
     * 1. Skinned Models (like BrainStem, CesiumMan): It returns the animated transform of the skeleton's root joint.
     * 2. Node-Animated Models (like BoxAnimated): It falls back to getting the transform from the scene graph node.
     * 3. Static Models: It falls back and returns the basic model matrix.
     *
     * @return The final, world-space transformation matrix for this object.
     */
    // In AnimatedModel.java

    // In AnimatedModel.java

    // In AnimatedModel.java
    // In AnimatedModel.java
    @Override
    public float[] getFinalWorldTransform() {

        /*// --- CASE 1: SKINNED MODEL (like BrainStem) ---
        // Does this primitive have a primary joint assigned from the loader?
        if (getSkeleton() != null && getPrimaryJointIndex() != -1) {

            // Get the skinning matrices that are calculated for the GPU each frame.
            float[][] jointMatrices = getSkeleton().getJointMatrices();

            // Check if the matrices are available and our joint index is valid.
            if (jointMatrices != null && jointMatrices.length > getPrimaryJointIndex()) {

                // This is the FINAL skinning matrix for our specific joint.
                // This matrix is designed to transform a vertex from its original local T-pose
                // position directly to its final animated world position.
                float[] skinningMatrix = jointMatrices[getPrimaryJointIndex()];

                // This is the primitive's original LOCAL transform (its T-pose position relative to the model's origin).
                // This is the correct matrix to multiply with the skinning matrix.
                float[] myLocalTransform = getLocalTransformMatrix(); // The safe getter from Object3DData

                // Calculate the final transform for the bounding box.
                // final = final_skinning_matrix * primitive's_local_transform
                float[] finalTransform = new float[16];
                android.opengl.Matrix.multiplyMM(finalTransform, 0, skinningMatrix, 0, myLocalTransform, 0);
                return finalTransform;
            }
        }*/

        // --- CASE 2: NODE-ANIMATED (CesiumMan) or if the above fails ---
        // Fallback to the method that works for correctly structured models.
        return super.getFinalWorldTransform();
    }

    public AnimatedModel setSkin(Skin skin) {
        this.skin = skin;
        return this;
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public AnimatedModel clone() {
        final AnimatedModel ret = new AnimatedModel();
        super.copy(ret);
        ret.setSkin(this.getSkin());
        return ret;
    }

    // FIXME:
    /**
     * This is the bind shape transform found in skin (ie. {@code <library_controllers><skin><bind_shape_matrix>}
     */
    public void setBindShapeMatrix(float[] matrix) {
        if (matrix == null) return;

        float[] vertex = new float[]{0, 0, 0, 1};
        float[] shaped = new float[]{0, 0, 0, 1};
        for (int i = 0; i < this.vertexArrayBuffer.capacity(); i += 3) {
            vertex[0] = this.vertexArrayBuffer.get(i);
            vertex[1] = this.vertexArrayBuffer.get(i + 1);
            vertex[2] = this.vertexArrayBuffer.get(i + 2);
            Matrix.multiplyMV(shaped, 0, matrix, 0, vertex, 0);
            this.vertexArrayBuffer.put(i, shaped[0]);
            this.vertexArrayBuffer.put(i + 1, shaped[1]);
            this.vertexArrayBuffer.put(i + 2, shaped[2]);
        }
        updateDimensions();
    }

    @Override
    public void debug() {
        try {
            // --- EXPANDED LOGGING ---
            Log.d("MODEL_DEBUG", "--- MODEL DATA --- " + getId());

            if (modelMatrix != null) {
                StringBuilder pos_sb = new StringBuilder("modelMatrix: ").append(Arrays.toString(modelMatrix));
                Log.d("MODEL_DEBUG", pos_sb.toString());
            }

// Print first 30 floats (10 vertices)
            if (vertexArrayBuffer != null) {
                StringBuilder pos_sb = new StringBuilder("Positions: ").append("(").append(vertexArrayBuffer.capacity()).append(") ");
                for (int i = 0; i < 16 && i < vertexArrayBuffer.capacity(); i++) {
                    pos_sb.append(vertexArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", pos_sb.toString());
            }

// Print first 30 floats (10 normals)
// IMPORTANT: Add a null check for finalNormals
            if (vertexNormalsArrayBuffer != null && vertexNormalsArrayBuffer.capacity() >= 30) {
                StringBuilder norm_sb = new StringBuilder("Normals:   ").append("(").append(vertexNormalsArrayBuffer.capacity()).append(") ");;
                for (int i = 0; i < 16 && i < vertexNormalsArrayBuffer.capacity(); i++) {
                    norm_sb.append(vertexNormalsArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", norm_sb.toString());
            } else {
                Log.d("MODEL_DEBUG", "Normals: null or too short.");
            }

            // Print first 15 indices
            if (indexBuffer != null) {
                StringBuilder idx_sb = new StringBuilder("Indices:   ").append("(").append(indexBuffer.capacity()).append(") ");;
                idx_sb.append("(").append(indexBuffer.getClass().getSimpleName()).append(")");

                for (int i = 0; i < 16 && i < indexBuffer.capacity(); i++) {
                    idx_sb.append(IOUtils.getIntBufferValue(indexBuffer, i)).append(" ");
                }
                Log.d("MODEL_DEBUG", idx_sb.toString());
            }

            if (textureCoordsArrayBuffer != null) {
                StringBuilder norm_sb = new StringBuilder("Textures:   ").append("(").append(textureCoordsArrayBuffer.capacity()).append(") ");;
                for (int i = 0; i < 16 && i < textureCoordsArrayBuffer.capacity(); i++) {
                    norm_sb.append(textureCoordsArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", norm_sb.toString());
            } else {
                Log.d("MODEL_DEBUG", "Textures: null or too short.");
            }

            // --- END LOGGING ---
        } catch (Exception e) {
            Log.e("MODEL_DEBUG", e.getMessage(), e);
        }
    }
}
