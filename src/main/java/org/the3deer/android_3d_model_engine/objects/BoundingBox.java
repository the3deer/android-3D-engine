package org.the3deer.android_3d_model_engine.objects;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.util.io.IOUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public final class BoundingBox {

    public static Object3DData build(Object3DData obj) {
        if (obj instanceof AnimatedModel && ((AnimatedModel) obj).getSkin() != null) {
            return buildSkinned((AnimatedModel) obj);
        }
        return buildStatic(obj);
    }

    public static Object3DData buildSkinned(AnimatedModel sourcePrimitive) {

        Log.d("BoundingBox", "Building SKINNED bounding box for: " + sourcePrimitive.getId());

        AnimatedModel boundingBox = sourcePrimitive.clone();

        boundingBox.setId(sourcePrimitive.getId() + "_boundingBox_skinned");
        boundingBox.setParent(sourcePrimitive);
        boundingBox.setDecorator(true);

        Dimensions box = sourcePrimitive.getDimensions();
        FloatBuffer vertices = IOUtils.createFloatBuffer(8 * 3);
        //@formatter:off
        vertices.put(box.getMin()[0]).put(box.getMin()[1]).put(box.getMin()[2]);
        vertices.put(box.getMin()[0]).put(box.getMax()[1]).put(box.getMin()[2]);
        vertices.put(box.getMax()[0]).put(box.getMax()[1]).put(box.getMin()[2]);
        vertices.put(box.getMax()[0]).put(box.getMin()[1]).put(box.getMin()[2]);
        vertices.put(box.getMin()[0]).put(box.getMin()[1]).put(box.getMax()[2]);
        vertices.put(box.getMin()[0]).put(box.getMax()[1]).put(box.getMax()[2]);
        vertices.put(box.getMax()[0]).put(box.getMax()[1]).put(box.getMax()[2]);
        vertices.put(box.getMax()[0]).put(box.getMin()[1]).put(box.getMax()[2]);
        //@formatter:on
        vertices.flip();
        boundingBox.setVertexBuffer(vertices);

        // --- ROBUST SKINNING --- //
        // Bind all 8 vertices of the bounding box to the root joint of the skeleton.
        // This makes the box move as a rigid unit with the model's root, which is the correct behavior.
        Node rootJoint = sourcePrimitive.getParentNode();
        if (rootJoint == null) {
            Log.e("BoundingBox", "Source primitive " + sourcePrimitive.getId() + " has no root joint!");
            return buildStatic(sourcePrimitive); // Fallback to a static box
        }
        Log.d("BoundingBox", "Root node: " + rootJoint.getId()+", Joint index: "+rootJoint.getJointIndex());
        int rootJointId = Math.max(rootJoint.getJointIndex(), 0);

        IntBuffer bboxJoints = IOUtils.createIntBuffer(8 * 4);
        FloatBuffer bboxWeights = IOUtils.createFloatBuffer(8 * 4);

        for (int i = 0; i < 8; i++) {
            // Bind to root joint with 100% weight
            bboxJoints.put(rootJointId).put(0).put(0).put(0);
            bboxWeights.put(1.0f).put(0f).put(0f).put(0f);
        }

        bboxJoints.flip();
        bboxWeights.flip();

        // create bbox skin
        final Skin bboxSkin = sourcePrimitive.getSkin().clone();
        bboxSkin.setName(sourcePrimitive.getSkin().getName() + "_boundingBox");
        bboxSkin.setJointComponents(4);
        bboxSkin.setWeightsComponents(4);
        bboxSkin.setJoints(bboxJoints);
        bboxSkin.setWeights(bboxWeights);
        bboxSkin.setRootJoint(rootJoint);
        boundingBox.setSkin(bboxSkin);

        final IntBuffer indexBuffer = IOUtils.createIntBuffer(24);
        //@formatter:off
        // back face
        indexBuffer.put(0).put(1); indexBuffer.put(1).put(2); indexBuffer.put(2).put(3); indexBuffer.put(3).put(0);
        // front face
        indexBuffer.put(4).put(5); indexBuffer.put(5).put(6); indexBuffer.put(6).put(7); indexBuffer.put(7).put(4);
        // connectors
        indexBuffer.put(0).put(4); indexBuffer.put(1).put(5); indexBuffer.put(2).put(6); indexBuffer.put(3).put(7);
        //@formatter:on
        indexBuffer.flip();
        boundingBox.setIndexBuffer(indexBuffer);
        boundingBox.getElements().get(0).setIndexBuffer(indexBuffer);
        boundingBox.setDrawMode(GLES20.GL_LINES);
        boundingBox.setDrawUsingArrays(false);
        boundingBox.setDrawModeList(null);

        return boundingBox;
    }

    public static Object3DData buildStatic(Object3DData obj) {

        Log.v("BoundingBox", "Building STATIC bounding box for: " + obj.getId());

        Dimensions box = obj.getDimensions();

        final FloatBuffer vertices = IOUtils.createFloatBuffer(8 * 3);
        //@formatter:off
        vertices.put(box.getMin()[0]).put(box.getMin()[1]).put(box.getMin()[2]);
        vertices.put(box.getMin()[0]).put(box.getMax()[1]).put(box.getMin()[2]);
        vertices.put(box.getMax()[0]).put(box.getMax()[1]).put(box.getMin()[2]);
        vertices.put(box.getMax()[0]).put(box.getMin()[1]).put(box.getMin()[2]);
        vertices.put(box.getMin()[0]).put(box.getMin()[1]).put(box.getMax()[2]);
        vertices.put(box.getMin()[0]).put(box.getMax()[1]).put(box.getMax()[2]);
        vertices.put(box.getMax()[0]).put(box.getMax()[1]).put(box.getMax()[2]);
        vertices.put(box.getMax()[0]).put(box.getMin()[1]).put(box.getMax()[2]);
        //@formatter:on
        vertices.flip();

        final IntBuffer indexBuffer = IOUtils.createIntBuffer(24);
        //@formatter:off
        // back face
        indexBuffer.put(0).put(1); indexBuffer.put(1).put(2); indexBuffer.put(2).put(3); indexBuffer.put(3).put(0);
        // front face
        indexBuffer.put(4).put(5); indexBuffer.put(5).put(6); indexBuffer.put(6).put(7); indexBuffer.put(7).put(4);
        // connectors
        indexBuffer.put(0).put(4); indexBuffer.put(1).put(5); indexBuffer.put(2).put(6); indexBuffer.put(3).put(7);
        //@formatter:on
        indexBuffer.flip();

        // prefer topmost node, because some nodes carries a transform + inverse
        final Node parentNode = obj.getParentNode() != null? obj.getParentNode() : null;
        Log.v("BoundingBox", "Bounding box Node: " + parentNode);

        return new Object3DData(vertices, indexBuffer)
                .setDrawMode(GLES20.GL_LINES)
                .setDrawUsingArrays(false)
                .setDecorator(true)
                .setId(obj.getId() + "_boundingBox_static").setParent(obj).setParentNode(parentNode);
    }
}
