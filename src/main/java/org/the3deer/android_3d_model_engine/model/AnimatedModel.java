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
