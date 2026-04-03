package org.the3deer.engine.services;

import org.the3deer.engine.animation.Animation;
import org.the3deer.engine.animation.JointTransform;
import org.the3deer.engine.model.AnimatedModel;
import org.the3deer.engine.model.Element;
import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.Node;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.model.Skin;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModelComparator {

    private static final Logger logger = Logger.getLogger(ModelComparator.class.getSimpleName());

    // --- PASTE THIS METHOD INTO YOUR ColladaLoaderTask.java CLASS ---
    public static void compareModels(Object3D legacyModel, Object3D newModel) {
        if (legacyModel == null || newModel == null) {
            logger.log(Level.SEVERE,  "One of the models is null. Cannot compare.");
            return;
        }

        logger.config("--- Comparing models: " + legacyModel.getId() + " (Legacy) vs. " + newModel.getId() + " (New) ---");

        // Compare simple properties
        compareField("ID", legacyModel.getId(), newModel.getId());
        compareField("isVisible", legacyModel.isVisible(), newModel.isVisible());
        compareField("isIndexed", legacyModel.isIndexed(), newModel.isIndexed());

        // Compare buffers by content
        compareFloatBuffer("Vertex Array", legacyModel.getVertexBuffer(), newModel.getVertexBuffer());
        compareFloatBuffer("Normals Array", legacyModel.getVertexNormalsArrayBuffer(), newModel.getVertexNormalsArrayBuffer());
        compareGenericBuffer("Colors Array", legacyModel.getColorsBuffer(), newModel.getColorsBuffer());
        compareFloatBuffer("Texture Array", legacyModel.getTextureCoordsArrayBuffer(), newModel.getTextureCoordsArrayBuffer());
        compareFloatBuffer("Colors Array", (FloatBuffer) legacyModel.getColorsBuffer(), (FloatBuffer) newModel.getColorsBuffer());

        // Compare elements
        if (legacyModel.getElements() == null && newModel.getElements() == null) {
            logger.config("Elements are both null");
        } else if (legacyModel.getElements() == null || newModel.getElements() == null) {
            logger.log(Level.SEVERE, "DIFFERENCE: Element is null. Legacy: " + (legacyModel == null)
                    + ", New: " + (newModel == null));
        } else if (legacyModel.getElements().size() != newModel.getElements().size()) {
            logger.log(Level.SEVERE, "DIFFERENCE: Element count. Legacy: " + legacyModel.getElements().size()
                    + ", New: " + newModel.getElements().size());
        } else {
            logger.config("Element count is the same: " + legacyModel.getElements().size());
            for (int i = 0; i < legacyModel.getElements().size(); i++) {
                logger.config("--- Comparing element at index " + i + " ---");
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
                logger.log(Level.SEVERE,  "DIFFERENCE: Skin is null. Legacy: " + (legacyAnimated.getSkin() == null) + ", New: " + (newAnimated.getSkin() == null));
            }


        } else if (legacyModel instanceof AnimatedModel || newModel instanceof AnimatedModel) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Class is different. Legacy: " + legacyModel.getClass() + ", New: " + newModel.getClass());
        }

        if (legacyModel.getMaterial() == null && newModel.getMaterial() == null) {
            logger.config("Material is both null");
        } else if (legacyModel.getMaterial() == null || newModel.getMaterial() == null) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Material is null. Legacy: " + (legacyModel.getMaterial() == null) + ", New: " + (newModel.getMaterial() == null));
        } else {
            // Compare material
            compareMaterial("Material", legacyModel.getMaterial(), newModel.getMaterial());
        }

        logger.config("--- Comparison Finished ---");
    }

    // Helper methods for the comparator
    private static void compareField(String name, Object legacy, Object newObj) {
        if (legacy == null && newObj == null) {
            logger.config("OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || !legacy.equals(newObj)) {
            logger.log(Level.SEVERE,  "DIFFERENCE: " + name + ". Legacy: " + legacy + ", New: " + newObj);

            if (legacy instanceof Map && newObj instanceof Map) {
                Map<?, ?> legacyMap = (Map<?, ?>) legacy;
                Map<?, ?> newMap = (Map<?, ?>) newObj;
                for (Object key : legacyMap.keySet()) {
                    if (!newMap.containsKey(key)) {
                        logger.log(Level.SEVERE,  "DIFFERENCE: " + name + " missing key in new: " + key);
                    } else if (!legacyMap.get(key).equals(newMap.get(key))) {
                        logger.log(Level.SEVERE,"DIFFERENCE: " + name + " value mismatch for key " + key + ". Legacy: "
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
                            logger.config("OK: " + name + " value for key " + key + " is the same.");
                        }
                    }
                }
                for (Object key : newMap.keySet()) {
                    if (!legacyMap.containsKey(key)) {
                        logger.log(Level.SEVERE,  "DIFFERENCE: " + name + " extra key in new: " + key);
                    }
                }
            }
        } else {
            logger.config("OK: " + name + " is the same.");
        }
    }

    private static void compareBuffer(String name, Buffer legacy, Buffer newBuf) {
        if (legacy == null && newBuf == null) {
            logger.config("OK: " + name + " buffer is null in both.");
            return;
        }
        if (legacy == null || newBuf == null) {
            logger.log(Level.SEVERE,"DIFFERENCE: " + name + " buffer. One is null. Legacy: "
                    + (legacy != null) + ", New: " + (newBuf != null));
            return;
        }
        if (legacy.capacity() != newBuf.capacity()) {
            logger.log(Level.SEVERE,"DIFFERENCE: " + name + " buffer capacity. Legacy: "
                    + legacy.capacity() + ", New: " + newBuf.capacity());
            return;
        }
        logger.config("OK: " + name + " buffer capacity is the same: " + legacy.capacity());
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
                logger.log(Level.SEVERE,"DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                        + legacyVal + ", New: " + newVal);
                // Put positions back to 0 before returning
                legacy.position(0);
                newBuf.position(0);
                return;
            }
        }
        logger.config("OK: " + name + " buffer contents are the same.");
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
                    logger.log(Level.SEVERE,"DIFFERENCE: " + name + " buffer content at index [" + i + "]. Legacy: "
                            + legacyVal + ", New: " + newVal + " (Type: " + legacy.getClass().getSimpleName() + ")");
                    return; // Exit on first difference
                }
            }
            logger.config("OK: " + name + " buffer contents are the same.");

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
            logger.config("OK: " + name + " is null in both.");
            return;
        }
        if (legacy == null || newMat == null) {
            logger.log(Level.SEVERE,"DIFFERENCE: " + name + ". One is null. Legacy: "
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
            logger.log(Level.SEVERE,  "DIFFERENCE: " + name + " texture. Legacy: " + legacyTexture + ", New: " + newTexture);
        } else {
            logger.config("OK: " + name + " texture is the same.");
        }
    }

    private static void compareArray(String name, float[] legacyValue, float[] newValue) {
        if (legacyValue == null && newValue == null) {
            logger.config("OK: " + name + " is null in both.");
        } else if (legacyValue != null && newValue != null) {
            if (legacyValue.length == newValue.length) {
                boolean ok = true;
                for (int i = 0; i < legacyValue.length; i++) {
                    if (Math.abs(legacyValue[i] - newValue[i]) > 0.0001f) {
                        logger.log(Level.SEVERE,"DIFFERENCE: " + name + " array at index [" + i + "]. Legacy: "
                                + legacyValue[i] + ", New: " + newValue[i]);
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    logger.config("OK: " + name + " is same in both.");
                }
            } else {
                logger.log(Level.SEVERE,"DIFFERENCE: " + name + ". Lengths do not match. Legacy: "
                        + (legacyValue.length) + ", New: " + (newValue.length));
            }
        } else {
            logger.log(Level.SEVERE,"DIFFERENCE: " + name + ". One is null. Legacy: "
                    + (legacyValue != null) + ", New: " + (newValue != null));
        }
    }

    public void compareScenes(Scene legacy, Scene neww) {
        if (legacy == null && neww == null) {
            logger.config("OK: Scene is null in both.");
            return;
        }
        if (legacy == null || neww == null) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Scene is null. Legacy: " + (legacy == null) + ", New: " + (neww == null));
            return;
        }

        // compare attributes
        compareField("Scene ID", legacy.getName(), neww.getName());
        compareField("Scene Name", legacy.getName(), neww.getName());

        // compare lists
        compareField("Scene Object Count", legacy.getObjects().size(), neww.getObjects().size());

        // compare nodes
        if (legacy.getRootNodes() == null && neww.getRootNodes() == null) {
            logger.config("OK: Scene root nodes are null in both.");
        } else if (legacy.getRootNodes() == null || neww.getRootNodes() == null) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Scene root nodes. One is null. Legacy: " + (legacy.getRootNodes() == null) + ", New: " + (neww.getRootNodes() == null));
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
        if (legacy.getSkins() == null && neww.getSkins() == null) {
            logger.config("OK: Scene skeletons are null in both.");
        } else if (legacy.getSkins() == null || neww.getSkins() == null) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Scene skeletons. One is null. Legacy: " + (legacy.getSkins() == null) + ", New: " + (neww.getSkins() == null));
        } else if (legacy.getSkins().size() != neww.getSkins().size()) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Scene skeleton count. Legacy: " + legacy.getSkins().size() + ", New: " + neww.getSkins().size());
        } else {
            // You can add more detailed comparisons for skeletons if needed
            for (int i = 0; i < legacy.getSkins().size(); i++) {
                Skin skinOld = legacy.getSkins().get(i);
                Skin skinNew = neww.getSkins().get(i);
                compareField("Skeleton Name", skinOld.getName(), skinNew.getName());
            }
        }

        // compare animations
        if (legacy.getAnimations() == null && neww.getAnimations() == null) {
            logger.config("OK: Scene animations are null in both.");
        } else if (legacy.getAnimations() == null || neww.getAnimations() == null) {
            logger.log(Level.SEVERE,  "DIFFERENCE: Scene animations. One is null. Legacy: " + (legacy.getAnimations() == null) + ", New: " + (neww.getAnimations() == null));
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
                    logger.config("OK: Animation keyframes are null in both.");
                } else if (animOld.getKeyFrames() == null || animNew.getKeyFrames() == null) {
                    logger.log(Level.SEVERE,  "DIFFERENCE: Animation keyframes. One is null. Legacy: " + (animOld.getKeyFrames() == null) + ", New: " + (animNew.getKeyFrames() == null));
                } else if (animOld.getKeyFrames().length != animNew.getKeyFrames().length) {
                    logger.log(Level.SEVERE,  "DIFFERENCE: Animation keyframe count. Legacy: " + animOld.getKeyFrames().length + ", New: " + animNew.getKeyFrames().length);
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
                logger.config("OK: Node [" + i + "] Transform is null in both.");
            } else if (nodeOld.getTransform() == null || nodeNew.getTransform() == null) {
                logger.log(Level.SEVERE,  "DIFFERENCE: Node [" + i + "] Transform. One is null. Legacy: " + (nodeOld.getTransform() == null) + ", New: " + (nodeNew.getTransform() == null));
            } else {
                compareArray("Node [" + i + "] Transform", nodeOld.getTransform(), nodeNew.getTransform());
            }

            // recursively compare child nodes
            doCompareNodes(nodeOld.getChildren(), nodeNew.getChildren());
        }
    }
}
