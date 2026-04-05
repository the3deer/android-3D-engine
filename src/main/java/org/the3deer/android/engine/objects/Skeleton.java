package org.the3deer.android.engine.objects;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Skin;
import org.the3deer.android.engine.util.Matrix;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * Utility class to build a visual representation of a skeleton.
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
public final class Skeleton {

    private static final int VERTICES_PER_BONE = 24; 

    public static AnimatedModel build(AnimatedModel animatedModel) {
        final AnimatedModel animSkeleton = animatedModel.clone();
        Skin skin = animSkeleton.getSkin();
        int jointCount = skin.getJointCount();

        animSkeleton.setVertexBuffer(IOUtils.createFloatBuffer(jointCount * VERTICES_PER_BONE * 3));
        animSkeleton.setNormalsBuffer(IOUtils.createFloatBuffer(jointCount * VERTICES_PER_BONE * 3));
        FloatBuffer colorBuffer = IOUtils.createFloatBuffer(jointCount * VERTICES_PER_BONE * 4);
        animSkeleton.setColorsBuffer(colorBuffer);
        animSkeleton.setMaterial(new Material());

        final Skin skeletonSkin = skin.clone();
        animSkeleton.setSkin(skeletonSkin);
        skeletonSkin.setJoints(IOUtils.createIntBuffer(jointCount * VERTICES_PER_BONE * 4));
        skeletonSkin.setWeights(IOUtils.createFloatBuffer(jointCount * VERTICES_PER_BONE * 4));

        animSkeleton.setId(animatedModel.getId() + "-skeleton");
        animSkeleton.setDrawMode(GLES20.GL_TRIANGLES);
        animSkeleton.setIndexed(false);
        animSkeleton.setDecorator(true);

        Node skinRoot = skin.getRootJoint();
        if (skinRoot == null) return animSkeleton;

        float[] rootPoint = new float[]{0, 0, 0, 1};
        Matrix.multiplyMV(rootPoint, 0, skinRoot.getWorldTransform(), 0, rootPoint, 0);
        
        for (Node child : skinRoot.getChildren()) {
            buildBones(animSkeleton, child, rootPoint, skinRoot.getJointIndex(), colorBuffer);
        }

        return animSkeleton;
    }

    private static void buildBones(AnimatedModel skeletonModel, Node node, float[] parentPoint, int parentIndex,
                                   FloatBuffer colorBuffer) {

        float[] point = new float[]{0, 0, 0, 1};
        Matrix.multiplyMV(point, 0, node.getWorldTransform(), 0, point, 0);

        float[] v = Math3DUtils.substract(point, parentPoint);
        float len = Math3DUtils.length(v);

        if (node.getJointIndex() >= 0 && len > 0.0001f) {
            float[] d = Math3DUtils.divide(v, len);
            float[] ref = Math.abs(d[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
            float[] u_vec = Math3DUtils.normalize2(Math3DUtils.crossProduct(d, ref));
            float[] v_perp = Math3DUtils.normalize2(Math3DUtils.crossProduct(d, u_vec));

            float width = len * 0.05f;
            float midDist = len * 0.2f; 
            float[] mid = Math3DUtils.add(parentPoint, Math3DUtils.multiply(d, midDist));

            float[] m1 = Math3DUtils.add(mid, Math3DUtils.multiply(u_vec, width));
            float[] m2 = Math3DUtils.add(mid, Math3DUtils.multiply(v_perp, width));
            float[] m3 = Math3DUtils.substract(mid, Math3DUtils.multiply(u_vec, width));
            float[] m4 = Math3DUtils.substract(mid, Math3DUtils.multiply(v_perp, width));

            float[] color = (parentIndex < 0) ? new float[]{1, 0, 0, 1} : new float[]{0, 0, 1, 1};

            // BASE: Parent joint ID if available, otherwise child joint ID
            int baseId = (parentIndex >= 0) ? parentIndex : node.getJointIndex();
            int tipId = node.getJointIndex();

            // LOWER PART (Base to Mid-section): All weighted to baseId (Parent)
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m2, baseId, 1.0f, m1, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m3, baseId, 1.0f, m2, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m4, baseId, 1.0f, m3, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m1, baseId, 1.0f, m4, baseId, 1.0f, color);

            // UPPER PART (Mid-section to Tip): Tip follows Child, Mid-section follows Parent
            // This allows the bone to "stretch" correctly without rotating out of place
            addTriangle(skeletonModel, point, tipId, 1.0f, m1, baseId, 1.0f, m2, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m2, baseId, 1.0f, m3, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m3, baseId, 1.0f, m4, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m4, baseId, 1.0f, m1, baseId, 1.0f, color);
        }

        for (Node child : node.getChildren()) {
            buildBones(skeletonModel, child, point, node.getJointIndex(), colorBuffer);
        }
    }

    private static void addTriangle(AnimatedModel model, 
                                    float[] p1, int joint1, float w1,
                                    float[] p2, int joint2, float w2,
                                    float[] p3, int joint3, float w3,
                                    float[] color) {
        float[] normal = Math3DUtils.calculateNormal(p1, p2, p3);
        Math3DUtils.normalizeVector(normal);
        putVertex(model, p1, normal, color, joint1, w1);
        putVertex(model, p2, normal, color, joint2, w2);
        putVertex(model, p3, normal, color, joint3, w3);
    }

    private static void putVertex(AnimatedModel model, float[] pos, float[] normal, float[] color, int jointId, float weight) {
        model.getVertexBuffer().put(pos[0]);
        model.getVertexBuffer().put(pos[1]);
        model.getVertexBuffer().put(pos[2]);
        model.getNormalsBuffer().put(normal[0]);
        model.getNormalsBuffer().put(normal[1]);
        model.getNormalsBuffer().put(normal[2]);
        ((FloatBuffer) model.getColorsBuffer()).put(color);
        IntBuffer jointIds = (IntBuffer) model.getSkin().getJointsBuffer();
        jointIds.put(Math.max(jointId, 0));
        jointIds.put(0); jointIds.put(0); jointIds.put(0);
        FloatBuffer weights = (FloatBuffer) model.getSkin().getWeightsBuffer();
        weights.put(weight);
        weights.put(0); weights.put(0); weights.put(0);
    }
}
