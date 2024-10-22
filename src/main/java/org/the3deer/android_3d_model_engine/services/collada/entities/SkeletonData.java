package org.the3deer.android_3d_model_engine.services.collada.entities;

import java.util.List;

public class SkeletonData {

    private JointData headJoint;
    private int jointCount;
    private int boneCount = 0;

    private List<JointData> joints;
    private List<JointData> bones;
    private float[] bindShapeMatrix;


    public SkeletonData(List<JointData> joints, List<JointData> bones, JointData headJoint) {
        this.joints = joints;
        this.bones = bones;
        this.headJoint = headJoint;
    }

    public List<JointData> getJoints() {
        return joints;
    }

    public List<JointData> getBones() {
        return bones;
    }

    public SkeletonData(int jointCount, JointData headJoint) {
        this.jointCount = jointCount;
        this.headJoint = headJoint;
    }

    public SkeletonData(int jointCount, int boneCount, JointData headJoint) {
        this.jointCount = jointCount;
        this.boneCount = boneCount;
        this.headJoint = headJoint;
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

    public JointData getHeadJoint() {
        return headJoint;
    }

    public int getJointCount() {
        if (joints != null){
            return joints.size();
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

    public JointData find(String geometryId) {
        if (joints != null){
            for (int i=0; i<joints.size(); i++){
                if (joints.get(i).getId() == null) continue;
                if (joints.get(i).getId().equals(geometryId)){
                    return joints.get(i);
                }
            }
        } else if (headJoint != null) {
            return headJoint.find(geometryId);
        }
        return null;
    }

}
