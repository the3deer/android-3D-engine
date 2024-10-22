package org.the3deer.android_3d_model_engine.objects;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class Wireframe {

    /**
     * Builds a wireframe of the model by drawing all lines (3) of the triangles.
     *
     * @param objData the 3d model
     * @return the 3d wireframe using indices
     */
    public static Object3DData build(Object3DData objData) {

        // TODO: create several wireframes elements instead of only 1 ?

        // log event
        Log.i("Wireframe", "Building wireframe... " + objData);

        // clone object
        final Object3DData ret = objData.clone();

        try {

            if (!objData.isDrawUsingArrays()) {

                // process all elements
                for (int i=0; i<objData.getElements().size(); i++) {
                    Element element = objData.getElements().get(i);
                    Element newElement = ret.getElements().get(i);

                    int totalIndex = element.getIndexBuffer().capacity();
                    if (totalIndex % 3 != 0){
                        Log.w("Wireframe", "Element "+i+" has " + totalIndex + " indices. That is not a x3 factor. Fixing...");
                        totalIndex = totalIndex + (totalIndex % 3);
                    }

                    // we need 2 points x face side
                    Log.i("Wireframe", "Building wireframe... Element: "+i+", Total indices: " + totalIndex);
                    final IntBuffer newBuffer = IOUtils.createIntBuffer(totalIndex * 2);
                    newElement.setIndexBuffer(newBuffer);

                    final Buffer drawBuffer = element.getIndexBuffer();
                    for (int j = 0; j < totalIndex; j += 3) {
                        final int v0;
                        final int v1;
                        final int v2;
                        if (drawBuffer instanceof IntBuffer){
                            v0 = ((IntBuffer)drawBuffer).get(j);
                            v1 = ((IntBuffer)drawBuffer).get(j + 1);
                            v2 = ((IntBuffer)drawBuffer).get(j + 2);
                        }else if (drawBuffer instanceof ShortBuffer){
                            v0 = ((ShortBuffer)drawBuffer).get(j);
                            v1 = ((ShortBuffer)drawBuffer).get(j + 1);
                            v2 = ((ShortBuffer)drawBuffer).get(j + 2);
                        } else if (drawBuffer instanceof ByteBuffer){
                            v0 = ((ByteBuffer)drawBuffer).get(j);
                            v1 = ((ByteBuffer)drawBuffer).get(j + 1);
                            v2 = ((ByteBuffer)drawBuffer).get(j + 2);
                        }else {
                            throw new IllegalStateException("The IndexBuffer is of unknown type");
                        }

                        newBuffer.put(v0);
                        newBuffer.put(v1);
                        newBuffer.put(v1);
                        newBuffer.put(v2);
                        newBuffer.put(v2);
                        newBuffer.put(v0);
                    }
                }

            } else {

                final FloatBuffer vertexBuffer = ret.getVertexBuffer();
                Log.i("Wireframe", "Building wireframe... Total vertices: " + vertexBuffer.capacity()/3);

                final IntBuffer newBuffer = IOUtils.createIntBuffer(vertexBuffer.capacity()/3 * 2);
                ret.setDrawOrder(newBuffer);
                Log.i("Wireframe", "Wireframe: Total indices: " + newBuffer.capacity());

                for (int i = 0; i < vertexBuffer.capacity() / 3 - 2; i += 3) {
                    newBuffer.put(i);
                    newBuffer.put(i + 1);
                    newBuffer.put(i + 1);
                    newBuffer.put(i + 2);
                    newBuffer.put(i + 2);
                    newBuffer.put(i);
                }
            }

            // listen for changes
            if (ret instanceof AnimatedModel) {
                objData.addListener(event -> {
                    if (event instanceof Object3DData.ChangeEvent) {
                        Object3DData source = ((Object3DData)event.getSource());
                        AnimatedModel animSource = (AnimatedModel)source;
                        AnimatedModel retA = (AnimatedModel) ret;

                        retA.setCentered(animSource.isCentered());

                        retA.setSkeleton(animSource.getSkeleton());
                        retA.setRootJoint(animSource.getRootJoint());
                        retA.setJoints(animSource.getJointIds());
                        retA.setWeights(animSource.getVertexWeights());
                        retA.setJointMatrices(animSource.getJointMatrices());
                        retA.setAnimation(animSource.getAnimation());
                        retA.refresh();
                    }
                    return false;
                });
            }

            // update
            ret.refresh();

            // return
            Material material = new Material();
            material.setDiffuse(Constants.COLOR_WHITE);
            return ret
                    .setReadOnly(true)
                    .setDrawMode(GLES20.GL_LINES)
                    .setDrawUsingArrays(false)
                    .setMaterial(material)
                    .setModelMatrix(objData.getModelMatrix())
                    .setId(objData.getId() + "_wireframe");
        } catch (Exception ex) {
            Log.e("Wireframe", ex.getMessage(), ex);
            throw new RuntimeException("Problem building wireframe", ex);
        }
    }
}
