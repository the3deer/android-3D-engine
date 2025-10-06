package org.the3deer.android_3d_model_engine.services.collada.entities;

import org.the3deer.android_3d_model_engine.model.Node;

import java.util.List;

public class SkeletonData {

    private Node headNode;
    private int jointCount;
    private int boneCount = 0;

    private List<Node> nodes;
    private List<Node> bones;
    private float[] bindShapeMatrix;


    public SkeletonData(List<Node> nodes, List<Node> bones, Node headNode) {
        this.nodes = nodes;
        this.bones = bones;
        this.headNode = headNode;
    }

    public List<Node> getJoints() {
        return nodes;
    }

    public List<Node> getBones() {
        return bones;
    }

    public SkeletonData(int jointCount, Node headNode) {
        this.jointCount = jointCount;
        this.headNode = headNode;
    }

    public SkeletonData(int jointCount, int boneCount, Node headNode) {
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

    public SkeletonData setBindShapeMatrix(float[] bindShapeMatrix) {
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

}
