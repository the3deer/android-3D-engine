package org.the3deer.android_3d_model_engine.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

public final class Skeleton {

    public static AnimatedModel build(AnimatedModel animatedModel) {

        // reserve buffers
        final AnimatedModel animSkeleton = animatedModel.clone();
        animSkeleton.setVertexBuffer(IOUtils.createFloatBuffer(animSkeleton.getSkin().getJointCount() * 9));   // 4 floats (i,j,k,l) x 3 vertex = 12 floats / joint
        animSkeleton.setNormalsBuffer(IOUtils.createFloatBuffer(animSkeleton.getSkin().getJointCount() * 9));
        // reserve color buffer (i.e. to draw "JOINT" types of a different color from others)
        FloatBuffer colorBuffer = IOUtils.createFloatBuffer(animSkeleton.getSkin().getJointCount() * 12);  // 4 floats (rgba) x 3 vertex = 12 floats / joint
        animSkeleton.setColorsBuffer(colorBuffer);

        // Create new skin
        final Skin skeletonSkin = animatedModel.getSkin().clone();
        animSkeleton.setSkin(skeletonSkin);

        // override data
        skeletonSkin.setJoints(IOUtils.createIntBuffer(animSkeleton.getSkin().getJointCount() * 3 * 4));
        skeletonSkin.setWeights(IOUtils.createFloatBuffer(animSkeleton.getSkin().getJointCount() * 3 * 4));

        // set properties
        animSkeleton.setId(animatedModel.getId() + "-skeleton");
        animSkeleton.setDrawMode(GLES20.GL_TRIANGLES);
        animSkeleton.setDrawUsingArrays(true);
        animSkeleton.setDecorator(true);

        // build
        Node skinRoot = animatedModel.getParentNode().getRoot();

        // debug
        Log.d("Skeleton", "Building skeleton... skin root: "+skinRoot.getId()+", joints: " + animSkeleton.getSkin().getJointCount());

        // build
        buildBones(animSkeleton, skinRoot, new float[]{0,0,0,1}, -1, colorBuffer);

        return animSkeleton;
    }

    private static void buildBones(AnimatedModel skeletonModel, Node node, float [] parentPoint, int parentIndex,
                                   FloatBuffer colorBuffer) {

        float[] point = new float[]{0,0,0,1};
        Matrix.multiplyMV(point, 0, node.getWorldTransform(), 0, point, 0);


        Log.v("Skeleton", "Building bones....  joint: "+node.getId()+", point: "+ Arrays.toString(point));

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

        if (node.getJointIndex() >= 0) {
            skeletonModel.getNormalsBuffer().put(normal);
            skeletonModel.getNormalsBuffer().put(normal);
            skeletonModel.getNormalsBuffer().put(normal);

            // parent point
            skeletonModel.getVertexBuffer().put(parentPoint[0]);
            skeletonModel.getVertexBuffer().put(parentPoint[1]);
            skeletonModel.getVertexBuffer().put(parentPoint[2]);

            // parent influences
            final IntBuffer jointIds = (IntBuffer) skeletonModel.getSkin().getJointsBuffer();
            jointIds.put(Math.max(parentIndex, 0));
            jointIds.put(0);
            jointIds.put(0);
            jointIds.put(0);
            final FloatBuffer vertexWeights = (FloatBuffer) skeletonModel.getSkin().getWeightsBuffer();
            vertexWeights.put(1);
            vertexWeights.put(0);
            vertexWeights.put(0);
            vertexWeights.put(0);

            // child point(s)
            skeletonModel.getVertexBuffer().put(point1[0]);
            skeletonModel.getVertexBuffer().put(point1[1]);
            skeletonModel.getVertexBuffer().put(point1[2]);
            skeletonModel.getVertexBuffer().put(point2[0]);
            skeletonModel.getVertexBuffer().put(point2[1]);
            skeletonModel.getVertexBuffer().put(point2[2]);
            for (int i = 0; i < 2; i++) {
                jointIds.put(node.getJointIndex());
                jointIds.put(0);
                jointIds.put(0);
                jointIds.put(0);
                vertexWeights.put(1);
                vertexWeights.put(0);
                vertexWeights.put(0);
                vertexWeights.put(0);
            }

            if (node.getJointIndex() < 0) {
                Log.w("Skeleton", "no index");
                final float color = 1;
                colorBuffer.put(new float[]{color, 0, 0, 1f});
                colorBuffer.put(new float[]{color, 0, 0, 1f});
                colorBuffer.put(new float[]{color, 0, 0, 1f});
            } else {
                // you can change the color to red for example, to see linked bones in different color
                final float color = 1;//-(float)joint.getIndex()/(float)jointCount;
                colorBuffer.put(new float[]{0, 0, color, 1});
                colorBuffer.put(new float[]{0, 0, color, 1});
                colorBuffer.put(new float[]{0, 0, color, 1});
            }
        }

        for (Node child : node.getChildren()) {
            buildBones(skeletonModel, child, point, node.getJointIndex(), colorBuffer);
        }
    }
}
