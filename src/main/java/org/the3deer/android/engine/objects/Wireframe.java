package org.the3deer.android.engine.objects;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Element;
import org.the3deer.android.engine.model.Material;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Wireframe {

    private static final Logger logger = Logger.getLogger(Wireframe.class.getSimpleName());

    /**
     * Builds a wireframe of the model by drawing all lines (3) of the triangles.
     *
     * @param objData the 3d model
     * @return the 3d wireframe using indices
     */
    public static Object3D build(Object3D objData) {

        // log event
        logger.info("Building wireframe... " + objData);

        // clone object
        final Object3D ret = objData.clone();

        try {

            if (objData.isIndexed()) {

                // process all elements
                for (int i=0; i<objData.getElements().size(); i++) {
                    Element element = objData.getElements().get(i);
                    Element newElement = ret.getElements().get(i);

                    int totalIndex = element.getIndexBuffer().capacity();
                    if (totalIndex % 3 != 0){
                        logger.warning("Element "+i+" has " + totalIndex + " indices. That is not a x3 factor. Fixing...");
                        totalIndex = totalIndex - (totalIndex % 3);
                    }

                    // Match the original index buffer type to prevent engine confusion
                    final Buffer newBuffer;
                    if (element.getIndexBuffer() instanceof IntBuffer) {
                        newBuffer = IOUtils.createIntBuffer(totalIndex * 2);
                    } else if (element.getIndexBuffer() instanceof ShortBuffer) {
                        newBuffer = IOUtils.createShortBuffer(totalIndex * 2);
                    } else {
                        newBuffer = ByteBuffer.allocateDirect(totalIndex * 2).order(java.nio.ByteOrder.nativeOrder());
                    }
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

                        putIndex(newBuffer, v0);
                        putIndex(newBuffer, v1);
                        putIndex(newBuffer, v1);
                        putIndex(newBuffer, v2);
                        putIndex(newBuffer, v2);
                        putIndex(newBuffer, v0);
                    }
                }

            } else {

                final FloatBuffer vertexBuffer = ret.getVertexBuffer();
                logger.info("Building wireframe... Total vertices: " + vertexBuffer.capacity()/3);

                final IntBuffer newBuffer = IOUtils.createIntBuffer(vertexBuffer.capacity()/3 * 2);
                ret.setIndexBuffer(newBuffer);
                logger.info("Wireframe: Total indices: " + newBuffer.capacity());

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
                    if (event instanceof Object3D.ChangeEvent) {
                        Object3D source = ((Object3D)event.getSource());
                        AnimatedModel animSource = (AnimatedModel)source;
                        AnimatedModel retA = (AnimatedModel) ret;

                        retA.setCentered(animSource.isCentered());

                        // Sync animation data
                        retA.setSkin(animSource.getSkin());
                        retA.refresh();
                    }
                    return false;
                });
                ((AnimatedModel) ret).setSkin(((AnimatedModel)objData).getSkin());
            }

            // update
            ret.refresh();

            // return
            Material material = new Material();
            material.setDiffuse(Constants.COLOR_WHITE);
            // DO NOT NULL OUT BUFFERS - IT CAN CAUSE ATTRIBUTE ALIGNMENT ISSUES IN THE SHADER
            // ret.setColorsBuffer(null);
            // ret.setTextureCoordsArrayBuffer(null);
            return ret
                    .setReadOnly(true)
                    .setDrawMode(GLES20.GL_LINES)
                    .setIndexed(true)
                    .setMaterial(material)
                    .setId(objData.getId() + "_wireframe");
        } catch (Exception ex) {
            logger.log(Level.SEVERE,  ex.getMessage(), ex);
            throw new RuntimeException("Problem building wireframe", ex);
        }
    }

    private static void putIndex(Buffer buffer, int value) {
        if (buffer instanceof IntBuffer) {
            ((IntBuffer) buffer).put(value);
        } else if (buffer instanceof ShortBuffer) {
            ((ShortBuffer) buffer).put((short) value);
        } else if (buffer instanceof ByteBuffer) {
            ((ByteBuffer) buffer).put((byte) value);
        }
    }
}
