package org.the3deer.android_3d_model_engine.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;

public final class Skeleton {

    public static AnimatedModel build(AnimatedModel animatedModel) {

        // reserve buffers
        AnimatedModel animSkeleton = animatedModel.clone();
        animSkeleton.setVertexBuffer(IOUtils.createFloatBuffer(animSkeleton.getJointCount() * 9));   // 4 floats (i,j,k,l) x 3 vertex = 12 floats / joint
        animSkeleton.setNormalsBuffer(IOUtils.createFloatBuffer(animSkeleton.getJointCount() * 9));
        // reserve color buffer (ie. to draw "JOINT" types of a different color from others)
        FloatBuffer colorBuffer = IOUtils.createFloatBuffer(animSkeleton.getJointCount() * 12);  // 4 floats (rgba) x 3 vertex = 12 floats / joint
        animSkeleton.setColorsBuffer(colorBuffer);

        // reserve joint buffers
        animSkeleton.setJoints(IOUtils.createFloatBuffer(animSkeleton.getJointCount() * 3 * 4));
        animSkeleton.setWeights(IOUtils.createFloatBuffer(animSkeleton.getJointCount() * 3 * 4));



        // set own data
        animSkeleton.setId(animatedModel.getId() + "-skeleton");
        animSkeleton.setDrawMode(GLES20.GL_TRIANGLES);
        animSkeleton.setDrawUsingArrays(true);
        animSkeleton.setModelMatrix(animatedModel.getModelMatrix());
        animSkeleton.setReadOnly(true);

        // log event
        Log.i("Skeleton", "Building skeleton... joints: " + animSkeleton.getJointCount());

        // build
        JointData joint = animSkeleton.getSkeleton().getHeadJoint();

        // point
        float[] point = new float[]{0,0,0,1};
        float[] inverted = new float[16];
        Matrix.invertM(inverted,0, joint.getInverseBindTransform(), 0);
        Matrix.multiplyMV(point, 0, inverted, 0, point, 0);
        point[3] = 1;
        buildBones(animSkeleton, joint, point, joint.getIndex(), colorBuffer);

        //skeleton.setBindShapeMatrix(animatedModel.getBindShapeMatrix22222());

        return animSkeleton;
    }

    private static void buildBones(AnimatedModel animatedModel, JointData joint,
                                   float[] parentPoint, int parentJoinIndex, FloatBuffer colorBuffer) {

        float[] point = new float[4];
        point[3] = 1;

        //point[0] = joint.getBindTransform()[12];
        //point[1] = joint.getBindTransform()[13];
        //point[2] = joint.getBindTransform()[14];

        float[] inverted = new float[16];
        Matrix.invertM(inverted,0, joint.getInverseBindTransform(), 0);
        Matrix.multiplyMV(point, 0, inverted, 0, point, 0);
        point[3] = 1;

        float[] v = Math3DUtils.substract(point, parentPoint);
        float[] point1 = new float[]{point[0], point[1], point[2] - Matrix.length(v[0], v[1], v[2]) * 0.05f};
        float[] point2 = new float[]{point[0], point[1], point[2] + Matrix.length(v[0], v[1], v[2]) * 0.05f};

        float[] normal = Math3DUtils.calculateNormal(parentPoint, point1, point2);
        if (Math3DUtils.length(normal) == 0) {
            // this may happen in first iteration - root point == root joint
            // do nothing
            normal = new float[]{0,1,0};
        } else {
            Math3DUtils.normalizeVector(normal);
        }
        animatedModel.getNormalsBuffer().put(normal);
        animatedModel.getNormalsBuffer().put(normal);
        animatedModel.getNormalsBuffer().put(normal);

        // parent point
        animatedModel.getVertexBuffer().put(parentPoint[0]);
        animatedModel.getVertexBuffer().put(parentPoint[1]);
        animatedModel.getVertexBuffer().put(parentPoint[2]);
        ((FloatBuffer)animatedModel.getJointIds()).put(Math.max(parentJoinIndex, 0));
        ((FloatBuffer)animatedModel.getJointIds()).put(0);
        ((FloatBuffer)animatedModel.getJointIds()).put(0);
        ((FloatBuffer)animatedModel.getJointIds()).put(0);
        ((FloatBuffer)animatedModel.getVertexWeights()).put(Math.max(parentJoinIndex,0));
        ((FloatBuffer)animatedModel.getVertexWeights()).put(0);
        ((FloatBuffer)animatedModel.getVertexWeights()).put(0);
        ((FloatBuffer)animatedModel.getVertexWeights()).put(0);

        // child point(s)
        animatedModel.getVertexBuffer().put(point1[0]);
        animatedModel.getVertexBuffer().put(point1[1]);
        animatedModel.getVertexBuffer().put(point1[2]);
        animatedModel.getVertexBuffer().put(point2[0]);
        animatedModel.getVertexBuffer().put(point2[1]);
        animatedModel.getVertexBuffer().put(point2[2]);
        for (int i = 0; i < 2; i++) {
            ((FloatBuffer)animatedModel.getJointIds()).put(Math.max(joint.getIndex(), 0));
            ((FloatBuffer)animatedModel.getJointIds()).put(0);
            ((FloatBuffer)animatedModel.getJointIds()).put(0);
            ((FloatBuffer)animatedModel.getJointIds()).put(0);
            ((FloatBuffer)animatedModel.getVertexWeights()).put(Math.max(joint.getIndex(), 0));
            ((FloatBuffer)animatedModel.getVertexWeights()).put(0);
            ((FloatBuffer)animatedModel.getVertexWeights()).put(0);
            ((FloatBuffer)animatedModel.getVertexWeights()).put(0);
        }

        if (joint.getIndex()<0){
            final float color = 0.75f;
            colorBuffer.put(new float[]{color,0,0,1f});
            colorBuffer.put(new float[]{color,0,0,1f});
            colorBuffer.put(new float[]{color,0,0,1f});
        } else {
            // you can change the color to red for example, to see linked bones in different color
            final float color = 1;//-(float)joint.getIndex()/(float)jointCount;
            colorBuffer.put(new float[]{0,0,color,1});
            colorBuffer.put(new float[]{0,0,color,1});
            colorBuffer.put(new float[]{0,0,color,1});
        }

        for (JointData child : joint.getChildren()) {
            buildBones(animatedModel, child, point, joint.getIndex(), colorBuffer);
        }
    }
}
