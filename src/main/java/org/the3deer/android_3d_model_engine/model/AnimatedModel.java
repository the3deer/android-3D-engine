package org.the3deer.android_3d_model_engine.model;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.services.collada.entities.SkeletonData;
import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.List;

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
    private List<Animation> animations;
    private Animation currentAnimation;

    // cache
    private Node rootNode;
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

    public AnimatedModel setRootJoint(Node rootNode) {
        this.rootNode = rootNode;
        return this;
    }

    public AnimatedModel setSkeleton(SkeletonData jointsData) {
        this.skeleton = jointsData;
        this.setJointMatrices(new float[skeleton.getBoneCount()][16]);  // 16 is the size of the matrix
        return this;
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


    public AnimatedModel setAnimations(List<Animation> animations) {
        this.animations = animations;
        if (animations != null && !animations.isEmpty()){
            setCurrentAnimation(animations.get(0));
        }
        
        propagate(new ChangeEvent(this));
        return this;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public AnimatedModel setCurrentAnimation(Animation currentAnimation) {
        this.currentAnimation = currentAnimation;
        return this;
    }

    /**
     * @return The root joint of the joint hierarchy. This joint has no parent,
     * and every other joint in the skeleton is a descendant of this
     * joint.
     */
    public Node getRootJoint() {
        if (this.rootNode == null && this.skeleton != null) {
            if (this.skeleton.getHeadJoint() != null) {
                this.rootNode = this.skeleton.getHeadJoint();
            } else if (this.skeleton != null){
                this.rootNode = this.skeleton.getJoints().get(0);
            }
        }
        //Log.v("Animator", "Root joint: "+rootJoint);
        return rootNode;
    }

    // In your model class (e.g., Object3DData or AnimatedModel)

    public void update() {
        // Start the recursive update for the entire model's node hierarchy.
        // We begin at the root joint, and its parent is the world origin (Identity Matrix).
        if (rootNode != null) {
            float[] identityMatrix = new float[16];
            Matrix.setIdentityM(identityMatrix, 0);
            rootNode.updateWorldTransform(identityMatrix);
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

    public void updateAnimatedTransform(Node node) {

        // check
        if (node.getIndex() >= getBoneCount()) return;

        // update
        getJointTransforms()[node.getIndex()] = node.getAnimatedWorldTransform();
    }

    @Override
    public AnimatedModel clone() {
        final AnimatedModel ret = new AnimatedModel();
        super.copy(ret);
        ret.setJoints(this.getJointIds());
        ret.setWeights(this.getVertexWeights());
        ret.setRootJoint(this.getRootJoint());
        ret.setSkeleton(this.getSkeleton());
        ret.setAnimations(this.getAnimations());
        ret.setJointMatrices(this.getJointTransforms());
        //ret.setBindShapeMatrix(this.getBindShapeMatrix());
        return ret;
    }


    public float[][] getJointMatrices() {
        return jointMatrices;
    }

    public AnimatedModel setJointMatrices(float[][] jointMatrices) {
        this.jointMatrices = jointMatrices;
        return this;
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
