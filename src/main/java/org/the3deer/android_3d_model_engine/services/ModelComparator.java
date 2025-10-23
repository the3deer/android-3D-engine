package org.the3deer.android_3d_model_engine.services;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ModelComparator {

    // --- PASTE THIS METHOD INTO YOUR ColladaLoaderTask.java CLASS ---
    public static void compareModels(Object3DData legacyModel, Object3DData newModel) {
        if (legacyModel == null || newModel == null) {
            Log.e("MODEL_COMPARE", "One of the models is null. Cannot compare.");
            return;
        }

        Log.i("MODEL_COMPARE", "--- Comparing models: " + legacyModel.getId() + " (Legacy) vs. " + newModel.getId() + " (New) ---");

        // Compare simple properties
        compareField("ID", legacyModel.getId(), newModel.getId());
        compareField("isVisible", legacyModel.isVisible(), newModel.isVisible());
        compareField("isIndexed", legacyModel.isIndexed(), newModel.isIndexed());

        // Compare buffers by content
        compareFloatBuffer("Vertex Array", legacyModel.getVertexArrayBuffer(), newModel.getVertexArrayBuffer());
        compareFloatBuffer("Normals Array", legacyModel.getVertexNormalsArrayBuffer(), newModel.getVertexNormalsArrayBuffer());
        compareFloatBuffer("Texture Array", legacyModel.getTextureCoordsArrayBuffer(), newModel.getTextureCoordsArrayBuffer());
        compareFloatBuffer("Colors Array", (FloatBuffer) legacyModel.getVertexColorsArrayBuffer(), (FloatBuffer) newModel.getVertexColorsArrayBuffer());

        // Compare elements
        if (legacyModel.getElements() == null && newModel.getElements() == null){
            Log.i("MODEL_COMPARE", "Elements are both null");
        }
        else if (legacyModel.getElements() == null || newModel.getElements() == null){
            Log.e("MODEL_COMPARE", "DIFFERENCE: Element is null. Legacy: " + (legacyModel == null)
                    + ", New: " + (newModel == null));
        } else if (legacyModel.getElements().size() != newModel.getElements().size()) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: Element count. Legacy: " + legacyModel.getElements().size()
                    + ", New: " + newModel.getElements().size());
        } else {
            Log.i("MODEL_COMPARE", "Element count is the same: " + legacyModel.getElements().size());
            for (int i = 0; i < legacyModel.getElements().size(); i++) {
                Log.i("MODEL_COMPARE", "--- Comparing element at index " + i + " ---");
                Element legacyElement = legacyModel.getElements().get(i);
                Element newElement = newModel.getElements().get(i);

                // Compare the element's index buffer
                compareIntBuffer("Element Index Buffer", (IntBuffer) legacyElement.getIndexBuffer(), (IntBuffer) newElement.getIndexBuffer());

                // Compare material
                compareMaterial("Element Material", legacyElement.getMaterial(), newElement.getMaterial());
            }
        }

        Log.i("MODEL_COMPARE", "--- Comparison Finished ---");
    }

    // Helper methods for the comparator
    private static void compareField(String name, Object legacy, Object newObj) {
        if (legacy == null && newObj == null) {
            Log.i("MODEL_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || !legacy.equals(newObj)) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". Legacy: " + legacy + ", New: " + newObj);
        } else {
            Log.i("MODEL_COMPARE", "OK: " + name + " is the same.");
        }
    }

    private static void compareBuffer(String name, Buffer legacy, Buffer newBuf) {
        if (legacy == null && newBuf == null) {
            Log.i("MODEL_COMPARE", "OK: " + name + " buffer is null in both.");
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
        Log.i("MODEL_COMPARE", "OK: " + name + " buffer capacity is the same: " + legacy.capacity());
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
        Log.i("MODEL_COMPARE", "OK: " + name + " buffer contents are the same.");
        // Put positions back to 0
        legacy.position(0);
        newBuf.position(0);
    }

    private static void compareIntBuffer(String name, IntBuffer legacy, IntBuffer newBuf) {
        compareBuffer(name, legacy, newBuf);
        if (legacy == null || newBuf == null || legacy.capacity() != newBuf.capacity()) return;

        legacy.position(0);
        newBuf.position(0);

        for (int i = 0; i < legacy.capacity(); i++) {
            int legacyVal = legacy.get();
            int newVal = newBuf.get();
            if (legacyVal != newVal) {
                Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                        + legacyVal + ", New: " + newVal);
                // Put positions back to 0 before returning
                legacy.position(0);
                newBuf.position(0);
                return;
            }
        }
        Log.i("MODEL_COMPARE", "OK: " + name + " buffer contents are the same.");
        // Put positions back to 0
        legacy.position(0);
        newBuf.position(0);
    }

    private static void compareMaterial(String name, Material legacy, Material newMat) {
        if (legacy == null && newMat == null) {
            Log.i("MODEL_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || newMat == null) {
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + ". One is null. Legacy: "
                    + (legacy != null) + ", New: " + (newMat != null));
            return;
        }

        String legacyTexture = (legacy.getColorTexture() != null) ? legacy.getColorTexture().getFile() : "null";
        String newTexture = (newMat.getColorTexture() != null) ? newMat.getColorTexture().getFile() : "null";

        if (!legacyTexture.equals(newTexture)){
            Log.e("MODEL_COMPARE", "DIFFERENCE: " + name + " texture. Legacy: " + legacyTexture + ", New: " + newTexture);
        } else {
            Log.i("MODEL_COMPARE", "OK: " + name + " texture is the same.");
        }
    }
}
