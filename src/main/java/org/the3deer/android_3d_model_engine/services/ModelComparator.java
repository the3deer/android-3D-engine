package org.the3deer.android_3d_model_engine.services;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class ModelComparator {

    // --- PASTE THIS METHOD INTO YOUR ColladaLoaderTask.java CLASS ---
    public static void compareModels(Object3DData legacyModel, Object3DData newModel) {
        if (legacyModel == null || newModel == null) {
            Log.e("MODEL_COMPARE", "One of the models is null. Cannot compare.");
            return;
        }

        Log.d("MODEL_COMPARE", "--- Comparing models: " + legacyModel.getId() + " (Legacy) vs. " + newModel.getId() + " (New) ---");

        // Compare simple properties
        compareField("ID", legacyModel.getId(), newModel.getId());
        compareField("isVisible", legacyModel.isVisible(), newModel.isVisible());
        compareField("isIndexed", legacyModel.isIndexed(), newModel.isIndexed());

        // Compare buffers by content
        compareFloatBuffer("Vertex Array", legacyModel.getVertexArrayBuffer(), newModel.getVertexArrayBuffer());
        compareFloatBuffer("Normals Array", legacyModel.getVertexNormalsArrayBuffer(), newModel.getVertexNormalsArrayBuffer());
        compareGenericBuffer("Colors Array", legacyModel.getVertexColorsArrayBuffer(), newModel.getVertexColorsArrayBuffer());
        compareFloatBuffer("Texture Array", legacyModel.getTextureCoordsArrayBuffer(), newModel.getTextureCoordsArrayBuffer());
        compareFloatBuffer("Colors Array", (FloatBuffer) legacyModel.getVertexColorsArrayBuffer(), (FloatBuffer) newModel.getVertexColorsArrayBuffer());

        // Compare elements
        if (legacyModel.getElements() == null && newModel.getElements() == null) {
            Log.d("MODEL_COMPARE", "Elements are both null");
        } else if (legacyModel.getElements() == null || newModel.getElements() == null) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: Element is null. Legacy: " + (legacyModel == null)
                    + ", New: " + (newModel == null));
        } else if (legacyModel.getElements().size() != newModel.getElements().size()) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: Element count. Legacy: " + legacyModel.getElements().size()
                    + ", New: " + newModel.getElements().size());
        } else {
            Log.d("MODEL_COMPARE", "Element count is the same: " + legacyModel.getElements().size());
            for (int i = 0; i < legacyModel.getElements().size(); i++) {
                Log.d("MODEL_COMPARE", "--- Comparing element at index " + i + " ---");
                Element legacyElement = legacyModel.getElements().get(i);
                Element newElement = newModel.getElements().get(i);

                // Compare the element's index buffer
                compareGenericBuffer("Element Index Buffer", legacyElement.getIndexBuffer(), newElement.getIndexBuffer());

                // Compare material
                compareMaterial("Element Material", legacyElement.getMaterial(), newElement.getMaterial());
            }
        }

        if (legacyModel instanceof AnimatedModel && newModel instanceof AnimatedModel) {
            AnimatedModel legacyAnimated = (AnimatedModel) legacyModel;
            AnimatedModel newAnimated = (AnimatedModel) newModel;
            if (legacyAnimated.getSkin() != null && newAnimated.getSkin() != null) {
                compareGenericBuffer("Joints", legacyAnimated.getSkin().getJointsBuffer(), newAnimated.getSkin().getJointsBuffer());
                compareGenericBuffer("Weights", legacyAnimated.getSkin().getWeightsBuffer(), newAnimated.getSkin().getWeightsBuffer());
            } else if (legacyAnimated.getSkin() == null || newAnimated.getSkin() == null) {
                Log.e("MODEL_COMPARE", "DIFFERENCE: Skin is null. Legacy: " + (legacyAnimated.getSkin() == null) + ", New: " + (newAnimated.getSkin() == null));
            }


        } else if (legacyModel instanceof AnimatedModel || newModel instanceof AnimatedModel) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: Class is different. Legacy: " + legacyModel.getClass() + ", New: " + newModel.getClass());
        }

        if (legacyModel.getMaterial() == null && newModel.getMaterial() == null) {
            Log.d("MODEL_COMPARE", "Material is both null");
        } else if (legacyModel.getMaterial() == null || newModel.getMaterial() == null) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: Material is null. Legacy: " + (legacyModel.getMaterial() == null) + ", New: " + (newModel.getMaterial() == null));
        } else {
            // Compare material
            compareMaterial("Material", legacyModel.getMaterial(), newModel.getMaterial());
        }

        Log.d("MODEL_COMPARE", "--- Comparison Finished ---");
    }

    // Helper methods for the comparator
    private static void compareField(String name, Object legacy, Object newObj) {
        if (legacy == null && newObj == null) {
            Log.d("MODEL_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || !legacy.equals(newObj)) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". Legacy: " + legacy + ", New: " + newObj);
        } else {
            Log.d("MODEL_COMPARE", "OK: " + name + " is the same.");
        }
    }

    private static void compareBuffer(String name, Buffer legacy, Buffer newBuf) {
        if (legacy == null && newBuf == null) {
            Log.d("MODEL_COMPARE", "OK: " + name + " buffer is null in both.");
            return;
        }
        if (legacy == null || newBuf == null) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " buffer. One is null. Legacy: "
                    + (legacy != null) + ", New: " + (newBuf != null));
            return;
        }
        if (legacy.capacity() != newBuf.capacity()) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " buffer capacity. Legacy: "
                    + legacy.capacity() + ", New: " + newBuf.capacity());
            return;
        }
        Log.d("MODEL_COMPARE", "OK: " + name + " buffer capacity is the same: " + legacy.capacity());
    }

    private static void compareFloatBuffer(String name, FloatBuffer legacy, FloatBuffer newBuf) {
        compareBuffer(name, legacy, newBuf);
        if (legacy == null || newBuf == null || legacy.capacity() != newBuf.capacity()) return;

        legacy.position(0);
        newBuf.position(0);

        for (int i = 0; i < legacy.capacity(); i++) {
            float legacyVal = legacy.get();
            float newVal = newBuf.get();
            if (Math.abs(legacyVal - newVal) > 0.0001f) {
                Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                        + legacyVal + ", New: " + newVal);
                // Put positions back to 0 before returning
                legacy.position(0);
                newBuf.position(0);
                return;
            }
        }
        Log.d("MODEL_COMPARE", "OK: " + name + " buffer contents are the same.");
        // Put positions back to 0
        legacy.position(0);
        newBuf.position(0);
    }

    // Replace the old compareIntBuffer with this generic version
    private static void compareGenericBuffer(String name, Buffer legacy, Buffer newBuf) {
        compareBuffer(name, legacy, newBuf); // This already handles null and capacity checks
        if (legacy == null || newBuf == null || legacy.capacity() != newBuf.capacity()) return;

        // Make sure we put the buffer position back to 0, even if something goes wrong
        try {
            legacy.position(0);
            newBuf.position(0);

            for (int i = 0; i < legacy.capacity(); i++) {
                // Read value as Long to handle all integer types (byte, short, int) without data loss
                long legacyVal = getLongFromBuffer(legacy, i);
                long newVal = getLongFromBuffer(newBuf, i);

                if (legacyVal != newVal) {
                    Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                            + legacyVal + ", New: " + newVal + " (Type: " + legacy.getClass().getSimpleName() + ")");
                    return; // Exit on first difference
                }
            }
            Log.d("MODEL_COMPARE", "OK: " + name + " buffer contents are the same.");

        } finally {
            // Always reset buffer positions
            if (legacy != null) legacy.position(0);
            if (newBuf != null) newBuf.position(0);
        }
    }

    // Helper method to read from any integer-based buffer type
    private static long getLongFromBuffer(Buffer buf, int index) {
        if (buf instanceof ByteBuffer) {
            // Convert unsigned byte to long
            return ((ByteBuffer) buf).get(index) & 0xFFL;
        } else if (buf instanceof ShortBuffer) {
            // Convert unsigned short to long
            return ((ShortBuffer) buf).get(index) & 0xFFFFL;
        } else if (buf instanceof IntBuffer) {
            // Convert unsigned int to long
            return ((IntBuffer) buf).get(index) & 0xFFFFFFFFL;
        }
        // Return a sentinel value for unsupported types to make errors obvious
        return -1L;
    }


    private static void compareMaterial(String name, Material legacy, Material newMat) {
        if (legacy == null && newMat == null) {
            Log.d("MODEL_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || newMat == null) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". One is null. Legacy: "
                    + (legacy != null) + ", New: " + (newMat != null));
            return;
        }

        //compareArray("diffuse", legacy.getDiffuse(), newMat.getDiffuse());
        compareArray("color", legacy.getColor(), newMat.getColor());

        String legacyTexture = (legacy.getColorTexture() != null) ? legacy.getColorTexture().getFile() : "null";
        String newTexture = (newMat.getColorTexture() != null) ? newMat.getColorTexture().getFile() : "null";
        if (legacyTexture == null) legacyTexture = "null";
        if (newTexture == null) newTexture = "null";

        if (!legacyTexture.equals(newTexture)) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " texture. Legacy: " + legacyTexture + ", New: " + newTexture);
        } else {
            Log.d("MODEL_COMPARE", "OK: " + name + " texture is the same.");
        }
    }

    private static void compareArray(String name, float[] legacyValue, float[] newValue) {
        if (legacyValue == null && newValue == null) {
            Log.d("MODEL_COMPARE", "OK: " + name + " is null in both.");
        } else if (legacyValue != null && newValue != null) {
            if (legacyValue.length == newValue.length) {
                boolean ok = true;
                for (int i = 0; i < legacyValue.length; i++) {
                    if (Math.abs(legacyValue[i] - newValue[i]) > 0.0001f) {
                        Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " array at index [" + i + "]. Legacy: "
                                + legacyValue[i] + ", New: " + newValue[i]);
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    Log.d("MODEL_COMPARE", "OK: " + name + " is same in both.");
                }
            } else {
                Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". Lengths do not match. Legacy: "
                        + (legacyValue.length) + ", New: " + (newValue.length));
            }
        } else {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". One is null. Legacy: "
                    + (legacyValue != null) + ", New: " + (newValue != null));
        }
    }
}
