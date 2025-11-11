package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class Skin {



    private Node sceneRoot;
    private Node rootJoint;

    private int jointCount;
    private int boneCount = 0;

    private List<Node> nodes;
    private List<Node> bones;
    private List<String> jointNames;

    private int jointComponents = Constants.MAX_VERTEX_WEIGHTS;
    private int weightsComponents = Constants.MAX_VERTEX_WEIGHTS;
    private Buffer jointsBuffer;
    private Buffer weightsBuffer;
    private float[] bindShapeMatrix;
    private float[][] jointMatrices;
    private float[] inverseBindMatrices;

    private boolean doInverseBindTranspose;
    private float[] inverseBindMatrices_transposed;


    public Skin() {
    }

    public Skin(List<Node> nodes, List<Node> bones, Node sceneRoot, Node rootJoint) {
        this.nodes = nodes;
        this.bones = bones;
        this.sceneRoot = sceneRoot;
        this.rootJoint = rootJoint;

        // init skinning matrix
        this.jointMatrices = new float[bones.size()][16];  // 16 is the size of the matrix
        for (int i = 0; i < this.jointMatrices.length; i++) {
            Matrix.setIdentityM(this.jointMatrices[i], 0);
        }
    }

    public Skin(float[] bindShapeMatrix, Buffer jointIds, Buffer weights, float[] inverseBindMatrices, List<String> jointNames) {
        this.bindShapeMatrix = bindShapeMatrix;
        this.jointsBuffer = jointIds;
        this.weightsBuffer = weights;
        this.inverseBindMatrices = inverseBindMatrices;
        this.jointNames = jointNames;

        // init skinning matrix
        this.jointMatrices = new float[jointNames.size()][16];  // 16 is the size of the matrix
        for (int i = 0; i < this.jointMatrices.length; i++) {
            Matrix.setIdentityM(this.jointMatrices[i], 0);
        }
    }


    public List<Node> getJoints() {
        return nodes;
    }

    public List<Node> getBones() {
        return bones;
    }

    public Skin(int jointCount, Node sceneRoot) {
        this.jointCount = jointCount;
        this.sceneRoot = sceneRoot;
        this.rootJoint = sceneRoot;
    }

    public Skin(int jointCount, int boneCount, Node sceneRoot) {
        this.jointCount = jointCount;
        this.boneCount = boneCount;
        this.sceneRoot = sceneRoot;
        this.rootJoint = sceneRoot;
    }

    public void incrementBoneCount() {
        this.boneCount++;
    }

    public void setBoneCount(int boneCount) {
        this.boneCount = boneCount;
    }

    public int getBoneCount() {
        if (bones != null) {
            return bones.size();
        } else {
            return boneCount;
        }
    }

    public Node getSceneRoot() {
        return sceneRoot;
    }

    public int getJointCount() {
        if (nodes != null) {
            return nodes.size();
        } else {
            return jointCount;
        }
    }

    public float[] getBindShapeMatrix() {
        if (bindShapeMatrix == null) {
            return Math3DUtils.IDENTITY_MATRIX;
        }
        return bindShapeMatrix;
    }

    public Skin setBindShapeMatrix(float[] bindShapeMatrix) {
        this.bindShapeMatrix = bindShapeMatrix;
        return this;
    }

    public int getJointComponents() {
        return jointComponents;
    }

    public void setJointComponents(int jointComponents) {
        this.jointComponents = jointComponents;
    }

    public int getWeightsComponents() {
        return weightsComponents;
    }

    public void setWeightsComponents(int weightsComponents) {
        this.weightsComponents = weightsComponents;
    }

    public Buffer getJointsBuffer() {
        return jointsBuffer;
    }

    public void setJointsBuffer(Buffer jointsBuffer) {
        this.jointsBuffer = jointsBuffer;
    }

    public Buffer getWeightsBuffer() {
        return weightsBuffer;
    }

    public void setWeightsBuffer(Buffer weightsBuffer) {
        this.weightsBuffer = weightsBuffer;
    }

    public float[] getInverseBindMatrices() {
        return inverseBindMatrices;
    }

    public void setInverseBindMatrices(float[] inverseBindMatrices) {
        this.inverseBindMatrices = inverseBindMatrices;
    }

    public void setDoInverseBindTranspose(boolean flag){
        this.doInverseBindTranspose = flag;
    }

    public Node find(String geometryId) {
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                if (nodes.get(i).getId() == null) continue;
                if (nodes.get(i).getId().equals(geometryId)) {
                    return nodes.get(i);
                }
            }
        } else if (sceneRoot != null) {
            return sceneRoot.find(geometryId);
        }
        return null;
    }

    public void setRootJoint(Node rootJoint) {
        this.rootJoint = rootJoint;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     * and every other joint in the skeleton is a descendant of this
     * joint.
     */
    public Node getRootJoint() {
        return rootJoint;
    }

    /**
     * Gets an array of the all important model-space transforms of all the
     * joints (with the current animation pose applied) in the entity. The
     * joints are ordered in the array based on their joint index. The position
     * of each joint's transform in the array is equal to the joint's index.
     *
     * @return The array of model-space transforms of the joints in the current
     * animation pose.
     */
    public float[][] getJointTransforms() {
        return getJointMatrices();
    }

    public float[][] getJointMatrices() {

        // FIXME: legacy - collada
        if (jointMatrices == null && boneCount > 0) {
            // init skinning matrix
            this.jointMatrices = new float[getBoneCount()][16];  // 16 is the size of the matrix
            for (int i = 0; i < this.jointMatrices.length; i++) {
                Matrix.setIdentityM(this.jointMatrices[i], 0);
            }
        }

        return jointMatrices;
    }

    public void setJointMatrices(float[][] jointMatrices) {
        this.jointMatrices = jointMatrices;
    }

    // In Skeleton.java

    /**
     * The public entry point for updating the skinning matrices for the GPU.
     * This method is the "Phase 3" of the animation update, after world transforms have been calculated.
     * It efficiently starts the recursive update from the root of the joint hierarchy.
     */
    public void updateSkinMatrices() {
        // 1. Get the root of the JOINT hierarchy (e.g., torso_joint_1).
        final Node root = getRootJoint();

        // 2. Safety check and start the recursion.
        if (root == null || getJointMatrices() == null) {
            return;
        }
        recursiveSkinMatrixUpdate(root);
    }

    /**
     * The private, recursive worker method that traverses the joint hierarchy.
     *
     * @param jointNode The current joint node in the traversal.
     */
    private void recursiveSkinMatrixUpdate(Node jointNode) {

        // --- 1. Process the current joint ---

        int jointIndex = jointNode.getJointIndex();

        // Check if we have a list of joints (collada)
        if (jointIndex == -1 && jointNames != null) {
            jointIndex = jointNames.indexOf(jointNode.getId());
            jointNode.setJointIndex(jointIndex);
        }

        // Only do calculations for nodes that are actual, indexed joints.
        if (jointIndex != -1) {

            // Get the final world transform (calculated in Phase 2).
            final float[] finalAnimatedWorldTransform = jointNode.getAnimatedWorldTransform();

            // Get the target matrix from our skinning array.
            final float[] targetSkinningMatrix = getJointMatrices()[jointIndex];

            // Calculate the position in the flat array for this joint's inverse bind matrix.
            final int inverseBindMatrixOffset = jointIndex * 16;

            //
            if (doInverseBindTranspose){
                if (inverseBindMatrices != null && inverseBindMatrices_transposed == null){
                    this.inverseBindMatrices_transposed = new float[this.inverseBindMatrices.length];
                    for (int i = 0; i <= inverseBindMatrices.length - 16; i+=16) {
                        // Transpose the i-th matrix from the source array into the destination array.
                        Matrix.transposeM(this.inverseBindMatrices_transposed, i, this.inverseBindMatrices, i);
                    }
                }
            }

            // Ensure all data is valid and within bounds.
            if (doInverseBindTranspose && finalAnimatedWorldTransform != null && this.inverseBindMatrices_transposed != null
                    && (inverseBindMatrixOffset + 15) < this.inverseBindMatrices_transposed.length) {

                // --- THIS IS THE CLEAN, STRATEGIC FIX ---
                // Final Skinning Matrix = finalAnimatedWorldTransform * inverseBindMatrix
                // We multiply directly from the offset in our large, flat inverseBindMatrices array.
                Matrix.multiplyMM(targetSkinningMatrix, 0, finalAnimatedWorldTransform, 0, this.inverseBindMatrices_transposed, inverseBindMatrixOffset);

            }
            // Ensure all data is valid and within bounds.
            else if (finalAnimatedWorldTransform != null && this.inverseBindMatrices != null
                    && (inverseBindMatrixOffset + 15) < this.inverseBindMatrices.length) {

                // --- THIS IS THE CLEAN, STRATEGIC FIX ---
                // Final Skinning Matrix = finalAnimatedWorldTransform * inverseBindMatrix
                // We multiply directly from the offset in our large, flat inverseBindMatrices array.
                Matrix.multiplyMM(targetSkinningMatrix, 0, finalAnimatedWorldTransform, 0, this.inverseBindMatrices, inverseBindMatrixOffset);

            } else {
                // This case indicates a problem, either in animation update or data loading.
                // For safety, we can set the skinning matrix to the animated pose to avoid catastrophic deformation.
                if (finalAnimatedWorldTransform != null){
                    System.arraycopy(finalAnimatedWorldTransform, 0, targetSkinningMatrix, 0, 16);
                }
            }
        }

        // --- 2. Recurse for all children of this joint ---
        if (jointNode.getChildren() == null) return;
        for (Node childJoint : jointNode.getChildren()) {
            recursiveSkinMatrixUpdate(childJoint);
        }
    }
}
