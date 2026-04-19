package org.the3deer.android.engine.objects;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Skin;
import org.the3deer.android.util.Matrix;
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
        if (animatedModel.getSkin() == null) return null;

        final AnimatedModel animSkeleton = animatedModel.clone();
        Skin skin = animatedModel.getSkin();
        Node skinRoot = skin.getRootJoint();
        if (skinRoot == null) return null;

        // Count all nodes to ensure buffer capacity. 
        // We use a 6x multiplier to be absolutely safe for bones, leaf tips, and complex branching.
        final int nodeCount = countNodes(skinRoot);
        final int vertexCapacity = nodeCount * VERTICES_PER_BONE * 6;

        animSkeleton.setVertexBuffer(IOUtils.createFloatBuffer(vertexCapacity * 3));
        animSkeleton.setNormalsBuffer(IOUtils.createFloatBuffer(vertexCapacity * 3));
        FloatBuffer colorBuffer = IOUtils.createFloatBuffer(vertexCapacity * 4);
        animSkeleton.setColorsBuffer(colorBuffer);
        
        // Material - ensure it uses vertex colors
        Material mat = new Material();
        mat.setColor(new float[]{1, 1, 1, 1}); 
        animSkeleton.setMaterial(mat);

        // Skin for the skeleton model
        final Skin skeletonSkin = skin.clone();
        animSkeleton.setSkin(skeletonSkin);
        
        // Provide fresh buffers for skeleton-specific rigging
        skeletonSkin.setJoints(IOUtils.createIntBuffer(vertexCapacity * 4));
        skeletonSkin.setWeights(IOUtils.createFloatBuffer(vertexCapacity * 4));

        animSkeleton.setId(animatedModel.getId() + "-skeleton");
        animSkeleton.setDrawMode(GLES20.GL_TRIANGLES);
        animSkeleton.setIndexed(false);
        animSkeleton.setDecorator(true);

        float[] rootPoint = new float[]{0, 0, 0, 1};
        Matrix.multiplyMV(rootPoint, 0, skinRoot.getWorldTransform(), 0, rootPoint, 0);
        
        for (Node child : skinRoot.getChildren()) {
            buildBones(animSkeleton, child, rootPoint, skinRoot.getJointIndex(), colorBuffer);
        }

        // Finalize and flip all buffers for OpenGL
        animSkeleton.getVertexBuffer().flip();
        animSkeleton.getNormalsBuffer().flip();
        colorBuffer.flip();
        skeletonSkin.getWeightsBuffer().flip();
        java.nio.Buffer jointsBuffer = skeletonSkin.getJointsBuffer();
        if (jointsBuffer instanceof IntBuffer) {
            ((IntBuffer) jointsBuffer).flip();
        } else if (jointsBuffer instanceof FloatBuffer) {
            ((FloatBuffer) jointsBuffer).flip();
        }
        
        // Set actual vertex count based on what was actually written
        animSkeleton.setVertexCount(animSkeleton.getVertexBuffer().limit() / 3);

        return animSkeleton;
    }

    private static int countNodes(Node node) {
        if (node == null) return 0;
        int count = 1;
        for (Node child : node.getChildren()) {
            count += countNodes(child);
        }
        return count;
    }

    private static void buildBones(AnimatedModel skeletonModel, Node node, float[] parentPoint, int parentIndex,
                                   FloatBuffer colorBuffer) {

        float[] point = new float[]{0, 0, 0, 1};
        Matrix.multiplyMV(point, 0, node.getWorldTransform(), 0, point, 0);

        float[] v = Math3DUtils.substract(point, parentPoint);
        float len = Math3DUtils.length(v);

        // Render a bone if there is distance, even if the child is not a "joint" 
        // (to capture head/tail tips)
        if (len > 0.0001f && parentIndex >= 0) {
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

            // Color: Distinct red for root, blue for others
            float[] color = (parentIndex == 0) ? new float[]{1, 0, 0, 1} : new float[]{0, 0.5f, 1, 1};

            // Rigging:
            // Base of bone bound to parent joint.
            // Tip of bone bound to current node (if it's a joint) otherwise bound to parent.
            int baseId = parentIndex;
            int tipId = (node.getJointIndex() >= 0) ? node.getJointIndex() : parentIndex;

            // LOWER PART: Base to Mid-section (follows Parent)
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m2, baseId, 1.0f, m1, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m3, baseId, 1.0f, m2, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m4, baseId, 1.0f, m3, baseId, 1.0f, color);
            addTriangle(skeletonModel, parentPoint, baseId, 1.0f, m1, baseId, 1.0f, m4, baseId, 1.0f, color);

            // UPPER PART: Mid-section (follows Parent) to Tip (follows tipId)
            addTriangle(skeletonModel, point, tipId, 1.0f, m1, baseId, 1.0f, m2, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m2, baseId, 1.0f, m3, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m3, baseId, 1.0f, m4, baseId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m4, baseId, 1.0f, m1, baseId, 1.0f, color);
        }

        // Propagate current joint index or keep parent index if this is a helper node
        int nextParentIndex = (node.getJointIndex() >= 0) ? node.getJointIndex() : parentIndex;

        // If this is a leaf node, draw a small "tip" bone to show the end of the chain
        if (node.getChildren().isEmpty() && nextParentIndex >= 0) {
            float tipLen = len > 0 ? len * 0.4f : 0.1f; // Slightly larger tip
            float[] d = len > 0 ? Math3DUtils.normalize2(v) : new float[]{0, 1, 0};
            float[] tipPoint = Math3DUtils.add(point, Math3DUtils.multiply(d, tipLen));
            
            float width = tipLen * 0.2f;
            float midDist = tipLen * 0.5f;
            float[] mid = Math3DUtils.add(point, Math3DUtils.multiply(d, midDist));
            
            // Re-calculate local axes for the tip
            float[] ref = Math.abs(d[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
            float[] u_vec = Math3DUtils.normalize2(Math3DUtils.crossProduct(d, ref));
            float[] v_perp = Math3DUtils.normalize2(Math3DUtils.crossProduct(d, u_vec));

            float[] m1 = Math3DUtils.add(mid, Math3DUtils.multiply(u_vec, width));
            float[] m2 = Math3DUtils.add(mid, Math3DUtils.multiply(v_perp, width));
            float[] m3 = Math3DUtils.substract(mid, Math3DUtils.multiply(u_vec, width));
            float[] m4 = Math3DUtils.substract(mid, Math3DUtils.multiply(v_perp, width));

            float[] color = new float[]{0, 0.8f, 0, 1}; // Green for tips
            int tipId = nextParentIndex;

            addTriangle(skeletonModel, point, tipId, 1.0f, m2, tipId, 1.0f, m1, tipId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m3, tipId, 1.0f, m2, tipId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m4, tipId, 1.0f, m3, tipId, 1.0f, color);
            addTriangle(skeletonModel, point, tipId, 1.0f, m1, tipId, 1.0f, m4, tipId, 1.0f, color);
            addTriangle(skeletonModel, tipPoint, tipId, 1.0f, m1, tipId, 1.0f, m2, tipId, 1.0f, color);
            addTriangle(skeletonModel, tipPoint, tipId, 1.0f, m2, tipId, 1.0f, m3, tipId, 1.0f, color);
            addTriangle(skeletonModel, tipPoint, tipId, 1.0f, m3, tipId, 1.0f, m4, tipId, 1.0f, color);
            addTriangle(skeletonModel, tipPoint, tipId, 1.0f, m4, tipId, 1.0f, m1, tipId, 1.0f, color);
        }

        for (Node child : node.getChildren()) {
            buildBones(skeletonModel, child, point, nextParentIndex, colorBuffer);
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
        
        final Skin skin = model.getSkin();
        final java.nio.Buffer joints = skin.getJointsBuffer();
        if (joints instanceof IntBuffer) {
            ((IntBuffer) joints).put(Math.max(jointId, 0));
            ((IntBuffer) joints).put(0); 
            ((IntBuffer) joints).put(0); 
            ((IntBuffer) joints).put(0);
        } else if (joints instanceof FloatBuffer) {
            ((FloatBuffer) joints).put((float)Math.max(jointId, 0));
            ((FloatBuffer) joints).put(0f); 
            ((FloatBuffer) joints).put(0f); 
            ((FloatBuffer) joints).put(0f);
        }

        FloatBuffer weights = skin.getWeightsBuffer();
        weights.put(weight);
        weights.put(0); weights.put(0); weights.put(0);
    }
}
