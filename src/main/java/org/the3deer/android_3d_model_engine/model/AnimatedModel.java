package org.the3deer.android_3d_model_engine.model;

import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;

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
    private Skeleton skeleton;

    // bind_shape_matrix
	/* The bind shape matrix describes how to transform the geometry into the right
	coordinate system for use with the joints */
    private float[] bindShapeMatrix;

    private Buffer jointIds;
    private Buffer vertexWeigths;

    private int jointComponents = Constants.MAX_VERTEX_WEIGHTS;
    private int weightsComponents = Constants.MAX_VERTEX_WEIGHTS;

    public AnimatedModel() {
        super();
    }

    public AnimatedModel(FloatBuffer vertexBuffer) {
        super(vertexBuffer);
    }

    public AnimatedModel(FloatBuffer vertexBuffer, Buffer drawOrderBuffer) {
        super(vertexBuffer, drawOrderBuffer);
    }

    @Override
    public float[] getModelMatrix() {
        // If this animated model is attached to a node that has a skeleton,
        // it means the vertex positions will be fully calculated by the
        // skinning matrices (u_jointMat) in the vertex shader.
        // The skinning matrices already contain the full world transformation.
        //
        // Therefore, the main model matrix (u_modelMatrix) must be an identity matrix
        // to avoid applying the world transformation twice.
        if (getParentNode() != null && getParentNode().getSkeleton() != null) {
            return Math3DUtils.IDENTITY_MATRIX;
        }

        // If there is no skeleton, this model is being animated by node transforms only.
        // In this case, fall back to the standard behavior.
        return super.getModelMatrix();
    }

    /**
     * Returns the definitive final world transform for this object, which can be used
     * to position and orient non-deforming helper objects like a Bounding Box.
     *
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


    public float[] getBindShapeMatrix() {
        if (bindShapeMatrix == null) {
            return Math3DUtils.IDENTITY_MATRIX;
        }
        return bindShapeMatrix;
    }

    public AnimatedModel setSkeleton(Skeleton jointsData) {
        this.skeleton = jointsData;
        return this;
    }

    public Skeleton getSkeleton() {
        return skeleton;
    }

    public int getJointCount() {
        return skeleton.getJointCount();
    }

    public int getBoneCount() {
        if (skeleton != null) {
            return skeleton.getBoneCount();
        }
        return 0;
    }

    public AnimatedModel setJoints(Buffer jointIds) {
        this.jointIds = jointIds;
        return this;
    }

    public Buffer getJointIds() {
        return jointIds;
    }

    public AnimatedModel setWeights(Buffer vertexWeigths) {
        this.vertexWeigths = vertexWeigths;
        return this;
    }

    public Buffer getVertexWeights() {
        return vertexWeigths;
    }



    @Override
    public AnimatedModel clone() {
        final AnimatedModel ret = new AnimatedModel();
        super.copy(ret);
        ret.setJoints(this.getJointIds());
        ret.setWeights(this.getVertexWeights());
        ret.skeleton = this.skeleton;
        //ret.setBindShapeMatrix(this.getBindShapeMatrix());
        return ret;
    }


    public void setJointIdsComponents(int numComponents) {
        this.jointComponents = numComponents;
    }

    public void setWeightsComponents(int numComponents) {
        this.weightsComponents = numComponents;
    }

    public int getJointComponents() {
        return jointComponents;
    }

    public int getWeightsComponents() {
        return weightsComponents;
    }
}
