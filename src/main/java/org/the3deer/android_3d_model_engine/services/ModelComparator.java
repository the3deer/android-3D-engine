package org.the3deer.android_3d_model_engine.services;

import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Map;

public class ModelComparator {

    // --- PASTE THIS METHOD INTO YOUR ColladaLoaderTask.java CLASS ---
    public static void compareModels(Object3DData legacyModel, Object3DData newModel) {
        if (legacyModel == null || newModel == null) {
            Log.e("DEBUG_COMPARE", "One of the models is null. Cannot compare.");
            return;
        }

        Log.d("DEBUG_COMPARE", "--- Comparing models: " + legacyModel.getId() + " (Legacy) vs. " + newModel.getId() + " (New) ---");

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
            Log.d("DEBUG_COMPARE", "Elements are both null");
        } else if (legacyModel.getElements() == null || newModel.getElements() == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Element is null. Legacy: " + (legacyModel == null)
                    + ", New: " + (newModel == null));
        } else if (legacyModel.getElements().size() != newModel.getElements().size()) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Element count. Legacy: " + legacyModel.getElements().size()
                    + ", New: " + newModel.getElements().size());
        } else {
            Log.d("DEBUG_COMPARE", "Element count is the same: " + legacyModel.getElements().size());
            for (int i = 0; i < legacyModel.getElements().size(); i++) {
                Log.d("DEBUG_COMPARE", "--- Comparing element at index " + i + " ---");
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
                // compare inverse bind matrices
                compareArray("Inverse Bind Matrices", legacyAnimated.getSkin().getInverseBindMatrices(), newAnimated.getSkin().getInverseBindMatrices());


            } else if (legacyAnimated.getSkin() == null || newAnimated.getSkin() == null) {
                Log.e("DEBUG_COMPARE", "DIFFERENCE: Skin is null. Legacy: " + (legacyAnimated.getSkin() == null) + ", New: " + (newAnimated.getSkin() == null));
            }


        } else if (legacyModel instanceof AnimatedModel || newModel instanceof AnimatedModel) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Class is different. Legacy: " + legacyModel.getClass() + ", New: " + newModel.getClass());
        }

        if (legacyModel.getMaterial() == null && newModel.getMaterial() == null) {
            Log.d("DEBUG_COMPARE", "Material is both null");
        } else if (legacyModel.getMaterial() == null || newModel.getMaterial() == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Material is null. Legacy: " + (legacyModel.getMaterial() == null) + ", New: " + (newModel.getMaterial() == null));
        } else {
            // Compare material
            compareMaterial("Material", legacyModel.getMaterial(), newModel.getMaterial());
        }

        Log.d("DEBUG_COMPARE", "--- Comparison Finished ---");
    }

    // Helper methods for the comparator
    private static void compareField(String name, Object legacy, Object newObj) {
        if (legacy == null && newObj == null) {
            Log.d("DEBUG_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || !legacy.equals(newObj)) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + ". Legacy: " + legacy + ", New: " + newObj);

            if (legacy instanceof Map && newObj instanceof Map) {
                Map<?, ?> legacyMap = (Map<?, ?>) legacy;
                Map<?, ?> newMap = (Map<?, ?>) newObj;
                for (Object key : legacyMap.keySet()) {
                    if (!newMap.containsKey(key)) {
                        Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " missing key in new: " + key);
                    } else if (!legacyMap.get(key).equals(newMap.get(key))) {
                        Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " value mismatch for key " + key + ". Legacy: "
                                + legacyMap.get(key) + ", New: " + newMap.get(key));
                    } else {
                        Object legacyVal = legacyMap.get(key);
                        Object newVal = newMap.get(key);
                        if (legacyVal instanceof Buffer && newVal instanceof Buffer) {
                            compareGenericBuffer(name + " buffer for key " + key, (Buffer) legacyVal, (Buffer) newVal);
                        } else if (legacyVal instanceof float[] && newVal instanceof float[]) {
                            compareArray(name + " array for key " + key, (float[]) legacyVal, (float[]) newVal);
                        } else if (legacyVal instanceof JointTransform && newVal instanceof JointTransform) {
                            compareField(name + " JointTransform for key " + key, legacyVal.toString(), newVal.toString());
                        } else {
                            Log.d("DEBUG_COMPARE", "OK: " + name + " value for key " + key + " is the same.");
                        }
                    }
                }
                for (Object key : newMap.keySet()) {
                    if (!legacyMap.containsKey(key)) {
                        Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " extra key in new: " + key);
                    }
                }
            }
        } else {
            Log.d("DEBUG_COMPARE", "OK: " + name + " is the same.");
        }
    }

    private static void compareBuffer(String name, Buffer legacy, Buffer newBuf) {
        if (legacy == null && newBuf == null) {
            Log.d("DEBUG_COMPARE", "OK: " + name + " buffer is null in both.");
            return;
        }
        if (legacy == null || newBuf == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " buffer. One is null. Legacy: "
                    + (legacy != null) + ", New: " + (newBuf != null));
            return;
        }
        if (legacy.capacity() != newBuf.capacity()) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " buffer capacity. Legacy: "
                    + legacy.capacity() + ", New: " + newBuf.capacity());
            return;
        }
        Log.d("DEBUG_COMPARE", "OK: " + name + " buffer capacity is the same: " + legacy.capacity());
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
                Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                        + legacyVal + ", New: " + newVal);
                // Put positions back to 0 before returning
                legacy.position(0);
                newBuf.position(0);
                return;
            }
        }
        Log.d("DEBUG_COMPARE", "OK: " + name + " buffer contents are the same.");
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
                    Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                            + legacyVal + ", New: " + newVal + " (Type: " + legacy.getClass().getSimpleName() + ")");
                    return; // Exit on first difference
                }
            }
            Log.d("DEBUG_COMPARE", "OK: " + name + " buffer contents are the same.");

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
            Log.d("DEBUG_COMPARE", "OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || newMat == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + ". One is null. Legacy: "
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
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " texture. Legacy: " + legacyTexture + ", New: " + newTexture);
        } else {
            Log.d("DEBUG_COMPARE", "OK: " + name + " texture is the same.");
        }
    }

    private static void compareArray(String name, float[] legacyValue, float[] newValue) {
        if (legacyValue == null && newValue == null) {
            Log.d("DEBUG_COMPARE", "OK: " + name + " is null in both.");
        } else if (legacyValue != null && newValue != null) {
            if (legacyValue.length == newValue.length) {
                boolean ok = true;
                for (int i = 0; i < legacyValue.length; i++) {
                    if (Math.abs(legacyValue[i] - newValue[i]) > 0.0001f) {
                        Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + " array at index [" + i + "]. Legacy: "
                                + legacyValue[i] + ", New: " + newValue[i]);
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    Log.d("DEBUG_COMPARE", "OK: " + name + " is same in both.");
                }
            } else {
                Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + ". Lengths do not match. Legacy: "
                        + (legacyValue.length) + ", New: " + (newValue.length));
            }
        } else {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: " + name + ". One is null. Legacy: "
                    + (legacyValue != null) + ", New: " + (newValue != null));
        }
    }

    public void compareScenes(Scene legacy, Scene neww) {
        if (legacy == null && neww == null) {
            Log.d("DEBUG_COMPARE", "OK: Scene is null in both.");
            return;
        }
        if (legacy == null || neww == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Scene is null. Legacy: " + (legacy == null) + ", New: " + (neww == null));
            return;
        }

        // compare attributes
        compareField("Scene ID", legacy.getId(), neww.getId());
        compareField("Scene Name", legacy.getName(), neww.getName());

        // compare lists
        compareField("Scene Object Count", legacy.getObjects().size(), neww.getObjects().size());

        // compare nodes
        if (legacy.getRootNodes() == null && neww.getRootNodes() == null) {
            Log.d("DEBUG_COMPARE", "OK: Scene root nodes are null in both.");
        } else if (legacy.getRootNodes() == null || neww.getRootNodes() == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Scene root nodes. One is null. Legacy: " + (legacy.getRootNodes() == null) + ", New: " + (neww.getRootNodes() == null));
        } else {
            compareField("Scene Root Node Count", legacy.getRootNodes().size(), neww.getRootNodes().size());
            // You can add more detailed comparisons for nodes if needed

            // recursively compare root nodes
            doCompareNodes(legacy.getRootNodes(), neww.getRootNodes());

            for (int i = 0; i < legacy.getRootNodes().size(); i++) {
                Node nodeOld = legacy.getRootNodes().get(i);
                Node nodeNew = neww.getRootNodes().get(i);
                compareField("Root Node [" + i + "] Name", nodeOld.getName(), nodeNew.getName());
                // You can add more detailed comparisons for nodes if needed
            }
        }

        // compare skeletons
        if (legacy.getSkeletons() == null && neww.getSkeletons() == null) {
            Log.d("DEBUG_COMPARE", "OK: Scene skeletons are null in both.");
        } else if (legacy.getSkeletons() == null || neww.getSkeletons() == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Scene skeletons. One is null. Legacy: " + (legacy.getSkeletons() == null) + ", New: " + (neww.getSkeletons() == null));
        } else {
            compareField("Scene Skeleton Count", legacy.getSkeletons().size(), neww.getSkeletons().size());
            // You can add more detailed comparisons for skeletons if needed
            for (int i = 0; i < legacy.getSkeletons().size(); i++) {
                Skin skinOld = legacy.getSkeletons().get(i);
                Skin skinNew = neww.getSkeletons().get(i);
                compareField("Skeleton Name", skinOld.getName(), skinNew.getName());
            }
        }

        // compare animations
        if (legacy.getAnimations() == null && neww.getAnimations() == null) {
            Log.d("DEBUG_COMPARE", "OK: Scene animations are null in both.");
        } else if (legacy.getAnimations() == null || neww.getAnimations() == null) {
            Log.e("DEBUG_COMPARE", "DIFFERENCE: Scene animations. One is null. Legacy: " + (legacy.getAnimations() == null) + ", New: " + (neww.getAnimations() == null));
        } else {
            compareField("Scene Animation Count", legacy.getAnimations().size(), neww.getAnimations().size());
            // You can add more detailed comparisons for animations if needed
             for (int i = 0; i < legacy.getAnimations().size(); i++) {
                Animation animOld = legacy.getAnimations().get(i);
                Animation animNew = neww.getAnimations().get(i);
                compareField("Animation Name", animOld.getName(), animNew.getName());
                compareField("Animation Length", animOld.getLength(), animNew.getLength());

                // compare keyframes
                if (animOld.getKeyFrames() == null && animNew.getKeyFrames() == null) {
                    Log.d("DEBUG_COMPARE", "OK: Animation keyframes are null in both.");
                } else if (animOld.getKeyFrames() == null || animNew.getKeyFrames() == null) {
                    Log.e("DEBUG_COMPARE", "DIFFERENCE: Animation keyframes. One is null. Legacy: " + (animOld.getKeyFrames() == null) + ", New: " + (animNew.getKeyFrames() == null));
                } else if (animOld.getKeyFrames().length != animNew.getKeyFrames().length) {
                    Log.e("DEBUG_COMPARE", "DIFFERENCE: Animation keyframe count. Legacy: " + animOld.getKeyFrames().length + ", New: " + animNew.getKeyFrames().length);
                } else {
                    for(int k=0; k<animOld.getKeyFrames().length; k++){
                        compareField("Animation Keyframe ["+k+"] Time", animOld.getKeyFrames()[k].getTime(), animNew.getKeyFrames()[k].getTime());
                        // You can add more detailed comparisons for keyframe data if needed
                        compareField("Animation Transforms", animOld.getKeyFrames()[k].getPose(), animNew.getKeyFrames()[k].getPose());
                    }
                }
             }
        }
    }

    private void doCompareNodes(List<Node> rootNodes, List<Node> rootNodes1) {
        for (int i = 0; i < rootNodes.size() && i < rootNodes1.size(); i++) {
            Node nodeOld = rootNodes.get(i);
            Node nodeNew = rootNodes1.get(i);
            compareField("Node [" + i + "] Name", nodeOld.getName(), nodeNew.getName());
            compareField("Node [" + i + "] Joint Index", nodeOld.getJointIndex(), nodeNew.getJointIndex());
            // You can add more detailed comparisons for nodes if needed

            // compare transform
            if (nodeOld.getTransform() == null && nodeNew.getTransform() == null) {
                Log.d("DEBUG_COMPARE", "OK: Node [" + i + "] Transform is null in both.");
            } else if (nodeOld.getTransform() == null || nodeNew.getTransform() == null) {
                Log.e("DEBUG_COMPARE", "DIFFERENCE: Node [" + i + "] Transform. One is null. Legacy: " + (nodeOld.getTransform() == null) + ", New: " + (nodeNew.getTransform() == null));
            } else {
                compareArray("Node [" + i + "] Transform", nodeOld.getTransform(), nodeNew.getTransform());
            }

            // recursively compare child nodes
            doCompareNodes(nodeOld.getChildren(), nodeNew.getChildren());
        }
    }
}
