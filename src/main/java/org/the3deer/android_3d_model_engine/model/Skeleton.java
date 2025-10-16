package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import java.util.List;

public class Skeleton {

    private Node headNode;
    private int jointCount;
    private int boneCount = 0;

    private List<Node> nodes;
    private List<Node> bones;
    private float[] bindShapeMatrix;

    private float[][] jointMatrices;

    public Skeleton(List<Node> nodes, List<Node> bones, Node headNode) {
        this.nodes = nodes;
        this.bones = bones;
        this.headNode = headNode;

        // init skinning matrix
        this.setJointMatrices(new float[getBoneCount()][16]);  // 16 is the size of the matrix
        for(int i = 0; i < this.getJointMatrices().length; i++){
            Matrix.setIdentityM(this.getJointMatrices()[i], 0);
        }
    }

    public List<Node> getJoints() {
        return nodes;
    }

    public List<Node> getBones() {
        return bones;
    }

    public Skeleton(int jointCount, Node headNode) {
        this.jointCount = jointCount;
        this.headNode = headNode;
    }

    public Skeleton(int jointCount, int boneCount, Node headNode) {
        this.jointCount = jointCount;
        this.boneCount = boneCount;
        this.headNode = headNode;
    }

    public void incrementBoneCount() {
        this.boneCount++;
    }

    public void setBoneCount(int boneCount) {
        this.boneCount = boneCount;
    }

    public int getBoneCount() {
        if (bones != null){
            return bones.size();
        } else {
            return boneCount;
        }
    }

    public Node getHeadJoint() {
        return headNode;
    }

    public int getJointCount() {
        if (nodes != null){
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
        if (nodes != null){
            for (int i = 0; i< nodes.size(); i++){
                if (nodes.get(i).getId() == null) continue;
                if (nodes.get(i).getId().equals(geometryId)){
                    return nodes.get(i);
                }
            }
        } else if (headNode != null) {
            return headNode.find(geometryId);
        }
        return null;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     * and every other joint in the skeleton is a descendant of this
     * joint.
     */
    public Node getRootJoint() {
        return headNode;
    }

    // In your model class (e.g., Object3DData or AnimatedModel)

    public void update() {
        // Start the recursive update for the entire model's node hierarchy.
        // We begin at the root joint, and its parent is the world origin (Identity Matrix).
        if (headNode != null) {
            float[] identityMatrix = new float[16];
            Matrix.setIdentityM(identityMatrix, 0);
            headNode.updateBindWorldTransform(identityMatrix);
        }
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

    public void updateSkinTransform(Node node, float[] animatedWorldTransform) {

        // check
        if (node.getIndex() >= getBoneCount()) return;

        // update
        getJointTransforms()[node.getIndex()] = animatedWorldTransform;
    }



    public float[][] getJointMatrices() {

        // FIXME: legacy - collada
        if (jointMatrices == null && boneCount > 0) {
            // init skinning matrix
            this.setJointMatrices(new float[getBoneCount()][16]);  // 16 is the size of the matrix
            for(int i = 0; i < this.getJointMatrices().length; i++){
                Matrix.setIdentityM(this.getJointMatrices()[i], 0);
            }
        }

        return jointMatrices;
    }

    public void setJointMatrices(float[][] jointMatrices) {
        this.jointMatrices = jointMatrices;
    }

}
