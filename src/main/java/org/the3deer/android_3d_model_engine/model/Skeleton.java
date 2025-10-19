package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import java.util.List;

public class Skeleton {

    private Node sceneRoot;
    private Node rootJoint;

    private int jointCount;
    private int boneCount = 0;

    private List<Node> nodes;
    private List<Node> bones;

    private float[] bindShapeMatrix;

    private float[][] jointMatrices;

    public Skeleton(List<Node> nodes, List<Node> bones, Node sceneRoot, Node rootJoint) {
        this.nodes = nodes;
        this.bones = bones;
        this.sceneRoot = sceneRoot;
        this.rootJoint = rootJoint;

        // init skinning matrix
        this.setJointMatrices(new float[getBoneCount()][16]);  // 16 is the size of the matrix
        for (int i = 0; i < this.getJointMatrices().length; i++) {
            Matrix.setIdentityM(this.getJointMatrices()[i], 0);
        }
    }

    public List<Node> getJoints() {
        return nodes;
    }

    public List<Node> getBones() {
        return bones;
    }

    public Skeleton(int jointCount, Node sceneRoot) {
        this.jointCount = jointCount;
        this.sceneRoot = sceneRoot;
    }

    public Skeleton(int jointCount, int boneCount, Node sceneRoot) {
        this.jointCount = jointCount;
        this.boneCount = boneCount;
        this.sceneRoot = sceneRoot;
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
        return bindShapeMatrix;
    }

    public Skeleton setBindShapeMatrix(float[] bindShapeMatrix) {
        this.bindShapeMatrix = bindShapeMatrix;
        return this;
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

        final int jointIndex = jointNode.getJointIndex();

        // Only do calculations for nodes that are actual, indexed joints.
        if (jointIndex != -1) {

            // Get the final world transform (calculated in Phase 2).
            final float[] finalAnimatedWorldTransform = jointNode.getAnimatedWorldTransform();

            // Get this joint's inverse bind matrix.
            final float[] inverseBindMatrix = jointNode.getInverseBindMatrix();

            if (finalAnimatedWorldTransform != null && inverseBindMatrix != null && jointIndex < getJointMatrices().length) {

                // Get the specific matrix from our array that we need to update.
                final float[] targetSkinningMatrix = getJointMatrices()[jointIndex];

                // Final Skinning Matrix = finalAnimatedWorldTransform * inverseBindMatrix
                Matrix.multiplyMM(targetSkinningMatrix, 0, finalAnimatedWorldTransform, 0, inverseBindMatrix, 0);
            }
        }

        // --- 2. Recurse for all children of this joint ---
        if (jointNode.getChildren() == null) return;
        for (Node childJoint : jointNode.getChildren()) {
            recursiveSkinMatrixUpdate(childJoint);
        }
    }

    public void updateSkinMatrices2() {

        // Check if the joint matrices buffer exists.
        final List<Node> nodeList = nodes != null? nodes : bones;

        if (nodeList == null || getJointMatrices() == null) return;

        // The 'nodes' list contains all the joints for this skeleton.
        for (Node jointNode : nodeList) {

            // 1. Get the final animated world transform for this joint.
            // This value was correctly calculated in the previous phase and stored on the node.
            float[] finalAnimatedWorldTransform = jointNode.getAnimatedWorldTransform();

            // This should never be null if the update phases are correct, but it's a good safety check.
            if (finalAnimatedWorldTransform == null) continue;

            // 2. Get the inverse bind matrix for THIS specific joint.
            // I assume this method exists on your Node class.
            float[] inverseBindMatrix = jointNode.getInverseBindMatrix();

            // 3. Get the target array for this joint from our uniform buffer.
            // The joint's index determines its slot in the array.
            int jointIndex = jointNode.getJointIndex();
            if (jointIndex >= 0 && jointIndex < getJointMatrices().length) {
                //getJointMatrices()[jointIndex] = finalAnimatedWorldTransform;
                float[] targetSkinningMatrix = getJointMatrices()[jointIndex];


                // 4. THIS IS THE FIX: Calculate the final skinning matrix and store it.
                // Skinning Matrix = finalAnimatedWorldTransform * inverseBindMatrix
                Matrix.multiplyMM(targetSkinningMatrix, 0, finalAnimatedWorldTransform, 0, inverseBindMatrix, 0);
            }
             else {
                Matrix.multiplyMM(finalAnimatedWorldTransform, 0, finalAnimatedWorldTransform, 0, inverseBindMatrix, 0);
            }
        }
    }
}
