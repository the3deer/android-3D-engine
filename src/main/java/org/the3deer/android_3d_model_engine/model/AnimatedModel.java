package org.the3deer.android_3d_model_engine.model;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.Joint;
import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;
import org.the3deer.android_3d_model_engine.services.collada.entities.SkeletonData;
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
    private SkeletonData skeleton;

    // bind_shape_matrix
	/* The bind shape matrix describes how to transform the geometry into the right
	coordinate system for use with the joints */
    private float[] bindShapeMatrix;

    private Buffer jointIds;
    private Buffer vertexWeigths;
    private Animation animation;

    // cache
    private Joint rootJoint;
    private float[][] jointMatrices;
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

    /**
     * This is the bind shape transform found in sking (ie. {@code <library_controllers><skin><bind_shape_matrix>}
     * The issue on handling this in the shader, is that we lose the transformation and cannot calculate further attributes (i.e. dimension)
     */
    public void setBindShapeMatrix(float[] matrix) {
        //this.bindShapeMatrix = matrix;
        super.setBindShapeMatrix(matrix);
    }

    public float[] getBindShapeMatrix() {
        if (bindShapeMatrix == null) {
            return Math3DUtils.IDENTITY_MATRIX;
        }
        return bindShapeMatrix;
    }

    public AnimatedModel setRootJoint(Joint rootJoint) {
        this.rootJoint = rootJoint;
        return this;
    }

    public void setSkeleton(SkeletonData jointsData) {
        this.skeleton = jointsData;
    }

    public SkeletonData getSkeleton() {
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

    public AnimatedModel setAnimation(Animation animation) {
        this.animation = animation;
        propagate(new ChangeEvent(this));
        return this;
    }

    public Animation getAnimation() {
        return animation;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     * and every other joint in the skeleton is a descendant of this
     * joint.
     */
    public Joint getRootJoint() {
        if (this.rootJoint == null && this.skeleton != null) {
            if (this.skeleton.getHeadJoint() != null) {
                this.rootJoint = Joint.buildJoints(this.skeleton.getHeadJoint());
            } else if (this.skeleton != null){
                this.rootJoint = Joint.buildJoints(this.skeleton.getJoints().get(0));
            }
        }
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
        if (getJointMatrices() == null) {
            this.setJointMatrices(new float[getBoneCount()][16]);  // 16 is the size of the matrix
        }
        return getJointMatrices();
    }

    public void updateAnimatedTransform(Joint joint) {

        // check
        if (joint.getIndex() >= getBoneCount()) return;

        // update
        getJointTransforms()[joint.getIndex()] = joint.getAnimatedTransform();
    }

    @Override
    public AnimatedModel clone() {
        final AnimatedModel ret = new AnimatedModel();
        super.copy(ret);
        ret.setJoints(this.getJointIds());
        ret.setWeights(this.getVertexWeights());
        ret.setRootJoint(this.getRootJoint());
        ret.setSkeleton(this.getSkeleton());
        ret.setAnimation(this.getAnimation());
        ret.setJointMatrices(this.getJointTransforms());
        //ret.setBindShapeMatrix(this.getBindShapeMatrix());
        return ret;
    }


    public float[][] getJointMatrices() {
        return jointMatrices;
    }

    public void setJointMatrices(float[][] jointMatrices) {
        this.jointMatrices = jointMatrices;
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

    public JointData getBone(String jointId) {
        return skeleton.find(jointId);
    }
}
