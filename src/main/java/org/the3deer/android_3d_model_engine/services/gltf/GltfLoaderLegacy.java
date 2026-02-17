package org.the3deer.android_3d_model_engine.services.gltf;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.animation.KeyFrame;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.math.Quaternion;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.CameraModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReference;
import de.javagl.jgltf.model.io.GltfReferenceResolver;
import de.javagl.jgltf.model.io.IO;
import de.javagl.jgltf.model.v2.MaterialModelV2;

public final class GltfLoaderLegacy {

    public static final String TAG = GltfLoaderLegacy.class.getSimpleName();

    public static List<String> getAllReferences(Uri uri) {

        final List<String> ret = new ArrayList<>();
        // final List<MeshData> allMeshes = new ArrayList<>();

        try (InputStream is = ContentUtils.getInputStream(uri)) {

            Log.i(TAG, "Loading model file... " + uri);

            // gltf ...
            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(is);

            for (GltfReference ref : gltfAsset.getReferences()) {
                ret.add(ref.getUri());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ret;
    }

    @NonNull
    public List<Object3DData> load(URI uri, LoadListener callback) {

        callback.onProgress("Loading file...");

        final List<Object3DData> ret = new ArrayList<>();
        // final List<MeshData> allMeshes = new ArrayList<>();

        try (InputStream is = ContentUtils.getInputStream(uri)) {

            Log.i(TAG, "Loading model file... " + uri);
            callback.onProgress("Loading " + uri);

            // gltf ...
            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(is);
            URI baseUri = IO.getParent(uri);
            GltfReferenceResolver.resolveAll(gltfAsset.getReferences(), baseUri);
            GltfModel gltfModel = GltfModels.create(gltfAsset);


            // --- 1. Load All Primitives into a Flat List ---
            // This creates one Object3DData for every drawable primitive in the file.
            final Map<MeshModel, List<Object3DData>> meshesListMap = loadMeshModel(gltfModel, callback);
            if (meshesListMap == null || meshesListMap.isEmpty()) {
                // No meshes to render
                return new ArrayList<>();
            }
            for (Map.Entry<MeshModel, List<Object3DData>> entry : meshesListMap.entrySet()) {
                ret.addAll(entry.getValue());
            }

            callback.onProgress("Loading cameras...");

            // --- 2. load camera list
            final List<Camera> cameraList = loadCameraModel(gltfModel, callback);

            // --- 3. load node hierarchy
            final List<Node> nodeList = loadNodes(gltfModel, meshesListMap, cameraList);

            // --- 4. load scenes
            final List<Scene> scenes = loadSceneModel(callback, gltfModel, nodeList, meshesListMap);

            callback.onProgress("Loading skeletons...");
            final List<Skin> skins = loadSkeletons(gltfModel, nodeList);

            callback.onProgress("Loading animations...");
            final List<Animation> animations = loadAnimations(gltfModel, nodeList, callback);

        } catch (Exception ex) {
            Log.e(TAG, "Problem loading model", ex);
        }
        return ret;
    }

    private List<Scene> loadSceneModel(LoadListener callback, GltfModel gltfModel,
                                       List<Node> nodeList, Map<MeshModel, List<Object3DData>> meshListMap) {

        if (gltfModel.getSceneModels() == null || gltfModel.getSceneModels().isEmpty()) {
            return null;
        }

        // ret
        final List<Scene> scenes = new ArrayList<>();

        // load scene...
        Log.d(TAG, "Loading scenes...");
        for (SceneModel sceneModel : gltfModel.getSceneModels()) {

            Log.v(TAG, "Loading scene: " + sceneModel.getName());
            callback.onProgress("Loading scene: " + sceneModel.getName());

            final Scene scene = new SceneImpl();
            scenes.add(scene);
            if (sceneModel.getName() != null) {
                scene.setName(sceneModel.getName());
            }
            callback.onLoad(scene);

            // Create a temporary list to hold all objects for THIS scene.
            final List<Object3DData> sceneObjects = new ArrayList<>();

            // For each ROOT node of the scene, recursively collect all objects.
            for (NodeModel rootNodeModel : sceneModel.getNodeModels()) {
                Log.v(TAG, "Traversing scene graph from root node: " + rootNodeModel.getName());
                final Node node = nodeList.get(gltfModel.getNodeModels().indexOf(rootNodeModel));
                collectObjects(scene, node, sceneObjects);
                Log.v(TAG, "Traversing scene collected objects: " + sceneObjects);
            }

            // Now add all collected objects to the scene at once.
            if (!sceneObjects.isEmpty()) {
                //scene.addObjects(sceneObjects);
                for (Object3DData obj : sceneObjects) {
                    callback.onLoad(scene, obj);
                }
            }

            // A scene's root nodes are those nodes that have no parent.
            // Find them and add them to the scene object.
            for (NodeModel rootNodeModel : sceneModel.getNodeModels()) {
                int rootNodeIndex = gltfModel.getNodeModels().indexOf(rootNodeModel);
                if (rootNodeIndex != -1) {
                    scene.addRootNode(nodeList.get(rootNodeIndex));
                }
            }

            scene.onLoadComplete();
        }
        return scenes;
    }

    /**
     * Recursively traverses a node and all its children, collecting all associated
     * Object3DData from the provided map.
     *
     * @param scene            the scene to add objects to.
     * @param node             The starting node to process.
     * @param collectedObjects The list where all found objects will be added.
     */
    private void collectObjects(Scene scene, Node node,
                                List<Object3DData> collectedObjects) {

        node.setScene(scene);

        // 1. Get objects from the CURRENT node
        if (node.getMeshes() != null) {
            collectedObjects.addAll(node.getMeshes());
        }

        // 2. Recursively call this method for all children
        if (node.getChildren() != null) {
            for (Node childNode : node.getChildren()) {
                collectObjects(scene, childNode, collectedObjects);
            }
        }
    }


    private List<Camera> loadCameraModel(GltfModel gltfModel, LoadListener callback) {

        // check
        if (gltfModel.getCameraModels() == null || gltfModel.getCameraModels().isEmpty())
            return null;

        // load
        final List<Camera> cameraList = new ArrayList<>();
        for (CameraModel cameraModel : gltfModel.getCameraModels()) {
            float[] floats = cameraModel.computeProjectionMatrix(null, 1.0f);
            // TODO: load parameters
            final Camera camera = new Camera();
            cameraList.add(camera);

            callback.onLoad(camera);
        }
        return cameraList;
    }

    private Map<MeshModel, List<Object3DData>> loadMeshModel(GltfModel gltfModel, LoadListener callback) {

        final List<MeshModel> meshModels = gltfModel.getMeshModels();
        if (meshModels == null || meshModels.isEmpty()) {
            Log.w(TAG, "No meshes found.");
            return null;
        }

        final Map<MeshModel, List<Object3DData>> meshes = new HashMap<>();
        for (MeshModel meshModel : gltfModel.getMeshModels()) {
            List<Object3DData> listObjs = loadMeshModel(gltfModel, meshModel, callback);
            meshes.put(meshModel, listObjs);
        }
        return meshes;
    }

    private List<Node> loadNodes(GltfModel gltfModel, Map<MeshModel, List<Object3DData>> meshesListMap, List<Camera> cameraList) {
        Log.d(TAG, "Loading nodes...");

        // nodes
        final List<NodeModel> nodeModels = gltfModel.getNodeModels();
        if (nodeModels == null || nodeModels.isEmpty()) {
            Log.e(TAG, "GltfModel contains no nodes.");
            return null;
        }

        // --- 1. Create a flat list of our engine's Node objects from the glTF nodes ---
        // This preserves the original file's node hierarchy and local transforms.
        final List<Node> nodeList = new ArrayList<>();
        for (int i = 0; i < nodeModels.size(); i++) {
            final NodeModel nodeModel = nodeModels.get(i);

            final Node node;
            if (nodeModel.getMatrix() != null) {
                node = Node.fromMatrix(nodeModel.getMatrix());
            } else if (nodeModel.getScale() != null || nodeModel.getTranslation() != null || nodeModel.getRotation() != null) {
                node = Node.fromTransforms(nodeModel.getScale(), Quaternion.fromArray(nodeModel.getRotation()), nodeModel.getTranslation());
            } else {
                node = Node.fromMatrix(nodeModel.computeLocalTransform(null));
            }

            String name = nodeModel.getName();
            if (name == null) {
                name = String.valueOf(i);
            }
            node.setId(String.valueOf(i));
            node.setName(name);

            List<Object3DData> meshes = null;
            if (nodeModel.getMeshModels() != null && !nodeModel.getMeshModels().isEmpty()) {
                meshes = new ArrayList<>();
                for (MeshModel meshModel : nodeModel.getMeshModels()) {
                    final List<Object3DData> originalObjsList = meshesListMap.get(meshModel);

                    // check
                    if (originalObjsList == null) continue;

                    // process
                    for (Object3DData originalObj : originalObjsList) {

                        // If the original object already has a parent, it means this is a shared mesh.
                        // We need to create a clone for this new node.
                        if (originalObj.getParentNode() != null) {
                            // You need to implement a clone() method in Object3DData/AnimatedModel
                            Object3DData clonedObj = ((AnimatedModel) originalObj).clone();
                            clonedObj.setParentNode(node);

                            // flag skinned
                            if (nodeModel.getSkinModel() != null) {
                                clonedObj.setSkined(true);
                            }

                            // Add the clone to the scene so it gets rendered
                            // This part is tricky. You need to collect ALL objects, originals and clones.
                            // A better way might be to add it to a new "finalObjects" list.
                            meshes.add(clonedObj);
                        } else {
                            // This is the first time we've seen this object.
                            // Assign it directly.
                            originalObj.setParentNode(node);
                            meshes.add(originalObj);
                        }
                    }
                }
            }

            /*// meshes
            if (nodeModel.getMeshModels() != null && !nodeModel.getMeshModels().isEmpty()) {
                for (MeshModel meshModel : nodeModel.getMeshModels()) {
                    final List<Object3DData> objsList = meshesListMap.get(meshModel);
                    if (objsList != null) {
                        for (Object3DData obj : objsList) {
                            obj.setParentNode(node);
                        }
                    }
                }
            }*/
            node.setMeshes(meshes);

            // camera
            if (nodeModel.getCameraModel() != null && gltfModel.getCameraModels() != null) {
                int cameraIdx = gltfModel.getCameraModels().indexOf(nodeModel.getCameraModel());
                final Camera camera = cameraList.get(cameraIdx);
                node.setCamera(camera);
                camera.setNode(node);
            }

            // skin
            if (nodeModel.getSkinModel() != null && gltfModel.getSkinModels() != null) {
                int skinIdx = gltfModel.getSkinModels().indexOf(nodeModel.getSkinModel());
                node.setSkinIndex(skinIdx);
            }

            nodeList.add(node);
        }

        // --- 2. Reconstruct the parent-child hierarchy in our Node objects ---
        for (int i = 0; i < nodeModels.size(); i++) {
            final NodeModel nodeModel = nodeModels.get(i);
            final Node parentNode = nodeList.get(i);
            final List<NodeModel> children = nodeModel.getChildren();
            if (children == null || children.isEmpty()) continue;

            for (NodeModel childNodeModel : children) {
                final int indexOfChild = nodeModels.indexOf(childNodeModel);
                final Node childNode = nodeList.get(indexOfChild);
                childNode.setParent(parentNode);
                parentNode.addChild(childNode);
            }
        }

        return nodeList;
    }

    private List<Object3DData> loadMeshModel(GltfModel gltfModel, MeshModel meshModel, LoadListener callback) {

        final List<MeshPrimitiveModel> meshPrimitiveModels = meshModel.getMeshPrimitiveModels();

        Log.d(TAG, "Loading mesh primitives...");
        callback.onProgress("Loading mesh primitives...");

        List<Object3DData> meshes = new ArrayList<>();
        for (MeshPrimitiveModel meshPrimitiveModel : meshPrimitiveModels) {
            Object3DData model = loadMeshPrimitive(gltfModel, meshModel, meshPrimitiveModel);

            // Give it a unique ID for easier debugging
            String id = (meshModel.getName() != null ? meshModel.getName() : "mesh" + gltfModel.getMeshModels().indexOf(meshModel))
                    + "_prim" + meshModel.getMeshPrimitiveModels().indexOf(meshPrimitiveModel);
            model.setId(id);
            model.setName(id);

            meshes.add(model);
        }

        return meshes;
    }

    private Object3DData loadMeshPrimitive(GltfModel gltfModel, MeshModel meshModel, MeshPrimitiveModel meshPrimitiveModel) {
        Log.d(TAG, "Loading mesh primitive...");

        // build model
        final AnimatedModel model = new AnimatedModel();

        AccessorModel position = meshPrimitiveModel.getAttributes().get("POSITION");
        FloatBuffer vertexBuffer = position.getAccessorData().createByteBuffer().asFloatBuffer();

        FloatBuffer normalBuffer = null;
        AccessorModel normal = meshPrimitiveModel.getAttributes().get("NORMAL");
        if (normal != null) {
            normalBuffer = normal.getAccessorData().createByteBuffer().asFloatBuffer();
        }

        FloatBuffer tangentBuffer = null;
        AccessorModel tangent = meshPrimitiveModel.getAttributes().get("TANGENT");
        if (tangent != null) {
            tangentBuffer = tangent.getAccessorData().createByteBuffer().asFloatBuffer();
        }

        Buffer colorBuffer = null;
        AccessorModel color = meshPrimitiveModel.getAttributes().get("COLOR_0");
        if (color != null) {
            if (color.getAccessorData().getComponentType() == short.class) {
                colorBuffer = color.getAccessorData().createByteBuffer().asShortBuffer();
            } else if (color.getAccessorData().getComponentType() == float.class) {
                colorBuffer = color.getAccessorData().createByteBuffer().asFloatBuffer();
            }
        }

        Buffer drawBuffer = null;
        if (meshPrimitiveModel.getIndices() != null) {
            if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == short.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer().asShortBuffer();
            } else if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == int.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer().asIntBuffer();
            } else if (meshPrimitiveModel.getIndices().getAccessorData().getComponentType() == byte.class) {
                drawBuffer = meshPrimitiveModel.getIndices().getAccessorData().createByteBuffer();
            } else {
                Log.e("GltfLoader", "unknown buffer type: " + meshPrimitiveModel.getIndices().getAccessorData().getComponentType());
            }
        }

        // build 3d model
        model.setVertexArrayBuffer(vertexBuffer);
        model.setVertexNormalsArrayBuffer(normalBuffer);
        model.setTangentBuffer(tangentBuffer);
        model.setVertexColorsArrayBuffer(colorBuffer);
        model.setIndexBuffer(drawBuffer);
        model.setDrawUsingArrays(drawBuffer == null);
        model.setDrawMode(meshPrimitiveModel.getMode());

        // init normals
        model.initNormals();

        // check
        if (drawBuffer != null) {
            int min = Integer.MAX_VALUE;
            int max = -Integer.MAX_VALUE;
            for (int i = 0; i < drawBuffer.capacity(); i++) {
                if (drawBuffer instanceof IntBuffer) {
                    min = Math.min(((IntBuffer) drawBuffer).get(i), min);
                    max = Math.max(((IntBuffer) drawBuffer).get(i), max);
                }
                if (drawBuffer instanceof ShortBuffer) {
                    int tempShort = Short.toUnsignedInt(((ShortBuffer) drawBuffer).get(i));
                    min = Math.min(tempShort, min);
                    max = Math.max(tempShort, max);
                } else if (drawBuffer instanceof ByteBuffer) {
                    min = Math.min(((ByteBuffer) drawBuffer).get(i), min);
                    max = Math.max(((ByteBuffer) drawBuffer).get(i), max);
                }
            }

            if (min != 0) {
                Log.e("GltfLoader", "Index not starting in zero: " + min);
            }
        }

        //final Element.Builder elementBuilder = new Element.Builder();

        // parse material
        MaterialModelV2 materialModel = (MaterialModelV2) meshPrimitiveModel.getMaterialModel();
        if (materialModel != null) {
            final Material material = new Material(materialModel.getName(), materialModel.getName());

            // map color
            material.setDiffuse(materialModel.getBaseColorFactor());
            material.setAlphaCutoff(materialModel.getAlphaCutoff());
            try {
                material.setAlphaMode(Material.AlphaMode.valueOf(materialModel.getAlphaMode().name()));
            } catch (Exception e) {
                // ignore
            }

            // map texture
            if (materialModel.getBaseColorTexture() != null) {
                ByteBuffer imageData = materialModel.getBaseColorTexture().getImageModel().getImageData();

                Log.v(TAG, "Decoding diffuse bitmap... " + materialModel.getBaseColorTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setColorTexture(new Texture().setBitmap(bitmap).setExtensions(materialModel.getExtensions()));
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // map normal map
            if (materialModel.getNormalTexture() != null) {
                ByteBuffer imageData = materialModel.getNormalTexture().getImageModel().getImageData();

                Log.i(TAG, "Decoding normal bitmap... " + materialModel.getNormalTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setNormalTexture(new Texture().setBitmap(bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // map emissive map
            if (materialModel.getEmissiveTexture() != null) {
                ByteBuffer imageData = materialModel.getEmissiveTexture().getImageModel().getImageData();

                Log.i(TAG, "Decoding emissive bitmap... " + materialModel.getEmissiveTexture().getName());
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                    material.setEmissiveTexture(new Texture().setBitmap(bitmap));
                    material.setEmissiveFactor(materialModel.getEmissiveFactor());
                } catch (Exception e) {
                    Log.e(TAG, "Issue decoding bitmap... " + materialModel.getBaseColorTexture().getName());
                }
            }

            // extensions
            try {
                final Map<String, Object> extensions = materialModel.getExtensions();
                if (extensions != null) {
                    final Map<String, Object> o = (Map<String, Object>) extensions.get("KHR_materials_volume");
                    if (o != null) {
                        final Map<String, Object> o1 = (Map<String, Object>) o.get("thicknessTexture");
                        Double o2 = (Double) o.get("thicknessFactor");
                        Double o3 = (Double) o.get("attenuationDistance");
                        List<Double> o4 = (List<Double>) o.get("attenuationColor");

                        if (o1 != null) {
                            final Integer texIdx = (Integer) o1.get("index");
                            final TextureModel textureModel = gltfModel.getTextureModels().get(texIdx);
                            final ByteBuffer imageData = textureModel.getImageModel().getImageData();
                            Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(imageData));
                            material.setTransmissionTexture(new Texture().setBitmap(bitmap));
                        }
                        if (o2 != null) material.setThicknessFactor(o2.floatValue());
                        if (o3 != null) material.setAttenuationDistance(o3.floatValue());
                        if (o4 != null)
                            material.setAttenuationColor(new float[]{o4.get(0).floatValue(), o4.get(1).floatValue(),
                                    o4.get(2).floatValue()});
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Issue decoding extensions... " + e.getMessage(), e);
            }

            model.setMaterial(material);
        }

        FloatBuffer textureBuffer = null;
        if (meshPrimitiveModel.getAttributes().containsKey("TEXCOORD_0")) {
            AccessorData texture_0 = meshPrimitiveModel.getAttributes().get("TEXCOORD_0").getAccessorData();
            textureBuffer = texture_0.createByteBuffer().asFloatBuffer();
            model.setTextureCoordsArrayBuffer(textureBuffer);
        }

        // load skinning data
        try {
            AccessorModel joints = meshPrimitiveModel.getAttributes().get("JOINTS");
            if (joints == null) {
                joints = meshPrimitiveModel.getAttributes().get("JOINTS_0");
            }
            AccessorModel weights = meshPrimitiveModel.getAttributes().get("WEIGHTS");
            if (weights == null) {
                weights = meshPrimitiveModel.getAttributes().get("WEIGHTS_0");
            }
            if (joints != null && weights != null) {
                final ByteBuffer byteBuffer = joints.getAccessorData().createByteBuffer();
                if (joints.getAccessorData().getComponentType() == int.class) {
                    model.setJoints(byteBuffer.asIntBuffer());
                } else if (joints.getAccessorData().getComponentType() == short.class) {
                    model.setJoints(byteBuffer.asShortBuffer());
                } else if (joints.getAccessorData().getComponentType() == byte.class) {
                    model.setJoints(byteBuffer);
                }

                // Read the VERY FIRST joint index directly from the buffer.
                // This is more robust than relying on model.getJointIds().
                int firstJointIndex = -1;
                final int originalPosition = byteBuffer.position(); // Save original position
                if (byteBuffer.remaining() > 0) {
                    if (joints.getAccessorData().getComponentType() == int.class) {
                        firstJointIndex = byteBuffer.asIntBuffer().get(0);
                    } else if (joints.getAccessorData().getComponentType() == short.class) {
                        // GLTF stores joint indices as UNSIGNED_SHORT, so we read it and convert to int.
                        firstJointIndex = byteBuffer.asShortBuffer().get(0) & 0xFFFF;
                    } else if (joints.getAccessorData().getComponentType() == byte.class) {
                        // GLTF stores joint indices as UNSIGNED_BYTE.
                        firstJointIndex = byteBuffer.get(0) & 0xFF;
                    }
                }
                byteBuffer.position(originalPosition); // Restore original position

                // Store this crucial piece of information!
                model.setPrimaryJointIndex(firstJointIndex);
                if (firstJointIndex != -1) {
                    Log.v(TAG, "Primitive " + model.getId() + " is primarily influenced by Joint Index: " + firstJointIndex);
                } else {
                    Log.w(TAG, "Primitive " + model.getId() + " has skinning data but could not determine a primary joint index.");
                }

                model.setJointComponents(joints.getElementType().getNumComponents());
                final ByteBuffer weightsBuffer = weights.getAccessorData().createByteBuffer();
                if (weights.getAccessorData().getComponentType() == float.class) {
                    model.setWeights(weightsBuffer.asFloatBuffer());
                    model.setWeightsComponents(weights.getElementType().getNumComponents());
                } else {
                    Log.e(TAG, "Unknown weights type: " + weights.getAccessorData().getComponentType());
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "Issue loading skinning data: " + ex.getMessage(), ex);
        }

        return model;
    }


    private List<Skin> loadSkeletons(GltfModel gltfModel, List<Node> nodeDataList) {

        final List<NodeModel> nodeModels = gltfModel.getNodeModels();

        Log.d(TAG, "Loading skin...");

        final List<SkinModel> skinModels = gltfModel.getSkinModels();
        if (skinModels == null || skinModels.isEmpty()) {
            // This model has no skinning/skeleton. Return the node list without bone data.
            // We still need a "head" node for positioning, so we'll pick the first node of the default scene.
            Node headNode = null;
            if (gltfModel.getSceneModels() != null && !gltfModel.getSceneModels().iterator().next().getNodeModels().isEmpty()) {
                final SceneModel firstScene = gltfModel.getSceneModels().iterator().next();
                for (int i = 0; i < firstScene.getNodeModels().size(); i++) {
                    final NodeModel nodeModel = firstScene.getNodeModels().get(i);
                    headNode = nodeDataList.get(nodeModels.indexOf(nodeModel));
                    headNode.setParent(headNode);
                }
            }
            Log.d(TAG, "No skins found. Returning scene graph without skeleton.");

            if (headNode == null) {
                Log.e(TAG, "CRITICAL: Could not determine scene root node!");
                // You might want to throw an exception here as this is a fatal loading error for animated models.
                return null;
            }

            Skin skin = new Skin(nodeDataList, Collections.emptyList(), headNode, null);

            // register skeleton
            headNode.getScene().getSkeletons().add(skin);

            return Collections.singletonList(skin);
        }

        final List<Skin> skins = new ArrayList<>();
        for (int s = 0; s < skinModels.size(); s++) {

            // --- 3. Process skin data: assign inverse bind matrices and create bone list ---
            final SkinModel skinModel = skinModels.get(s); // Assuming one skin for now

            final List<NodeModel> jointNodeModels = skinModel.getJoints();
            final Node[] boneDataList = new Node[jointNodeModels.size()];
            for (int i = 0; i < jointNodeModels.size(); i++) {
                NodeModel jointNodeModel = jointNodeModels.get(i);
                int index = nodeModels.indexOf(jointNodeModel);
                if (index != -1) {
                    Node node = nodeDataList.get(index);
                    node.setJointIndex(i);
                    node.setInverseBindMatrix(skinModel.getInverseBindMatrix(i, null));
                    boneDataList[i] = node;
                }
            }

            // --- 4. ROBUST SKELETON ROOT FINDING LOGIC ---
            Node rootJoint = null;

// FIRST, try to get the starting node from the explicit 'skeleton' property.
            final NodeModel skeletonRootNodeModel = skinModel.getSkeleton();
            if (skeletonRootNodeModel != null) {
                int skeletonRootIndex = nodeModels.indexOf(skeletonRootNodeModel);
                if (skeletonRootIndex != -1) {
                    rootJoint = nodeDataList.get(skeletonRootIndex);
                    Log.v(TAG, "Found starting point for hierarchy climb: explicit 'skeleton' property '" + rootJoint.getName() + "'");
                }
            }

// SECOND, if we still don't have a starting point, use the first joint in the list.
            if (rootJoint == null) {
                if (!jointNodeModels.isEmpty()) {
                    NodeModel firstJointNodeModel = jointNodeModels.get(0);
                    int firstJointIndex = nodeModels.indexOf(firstJointNodeModel);
                    if (firstJointIndex != -1) {
                        rootJoint = nodeDataList.get(firstJointIndex);
                        Log.v(TAG, "No explicit skeleton root. Found starting point for hierarchy climb: first joint '" + rootJoint.getName() + "'");
                    }
                }
            }

            if (rootJoint == null) {
                Log.e(TAG, "CRITICAL: Could not find any valid joint to begin hierarchy climb!");
                // This is a fatal error, you should probably throw an exception.
                continue; // Or return null
            }

// NOW, ALWAYS CLIMB TO THE TOP OF THE TREE from the starting node.
            Node headNode = rootJoint;
            while (headNode.getParent() != null && headNode.getParent() != headNode) { // Safety check for self-parenting
                headNode = headNode.getParent();
            }
            Log.v(TAG, "Final skeleton root found by climbing hierarchy to the top: '" + rootJoint.getName() + "'");

// This headNode is now the true root of the scene graph for this skeleton.
            final Skin skin = new Skin(nodeDataList, Arrays.asList(boneDataList), headNode, rootJoint)
                    .setBindShapeMatrix(skinModel.getBindShapeMatrix(null));

            // link skeleton to nodes / meshes
            for (Node node : nodeDataList) {
                node.setSkin(skin);
                if (node.getMeshes() != null) {
                    for (Object3DData mesh : node.getMeshes()) {
                        ((AnimatedModel) mesh).setSkin(skin);
                        skin.setWeightsBuffer(((AnimatedModel) mesh).getVertexWeights());
                        skin.setJointsBuffer(((AnimatedModel) mesh).getJointIds());
                        skin.setWeightsComponents(((AnimatedModel) mesh).getWeightsComponents());
                        skin.setJointComponents(((AnimatedModel) mesh).getJointComponents());

                        AccessorModel inverseBindMatrices = skinModel.getInverseBindMatrices();
                        FloatBuffer floatBuffer = inverseBindMatrices.getAccessorData().createByteBuffer().asFloatBuffer();
                        float[] inverseMatrices = new float[floatBuffer.capacity()];
                        for (int i=0; i<floatBuffer.capacity(); i++){
                            inverseMatrices[i] = floatBuffer.get(i);
                        }
                        skin.setInverseBindMatrices(inverseMatrices);
                    }
                }
            }

            // register skeleton
            skins.add(skin);

            // add skeleton
            headNode.getScene().getSkeletons().add(skin);

            Log.d(TAG, "Skeleton loaded successfully. Joints: " + nodeDataList.size() + ", Bones: " + boneDataList.length + ", Head: '" + headNode.getName() + "'");
        }

        return skins;
    }

    private List<Animation> loadAnimations(GltfModel gltfModel, List<Node> nodeList, LoadListener callback) {
        callback.onProgress("Loading animation data...");
        if (gltfModel.getAnimationModels() == null || gltfModel.getAnimationModels().isEmpty())
            return null;

        final List<Animation> animations = new ArrayList<>();
        for (AnimationModel an : gltfModel.getAnimationModels()) {

            // load animation
            AnimationModel animationModel = an;

            /*if (gltfModel.getAnimationModels().size() > 2){
                animationModel = gltfModel.getAnimationModels().get(2);
            }*/

            List<AnimationModel.Channel> channels = animationModel.getChannels();
            if (channels.isEmpty()) break;


            final TreeMap<Float, KeyFrame> times = new TreeMap<>();
            final List<Node> nodesFound = new ArrayList<>();

            for (int ch = 0; ch < channels.size(); ch++) {

                final AnimationModel.Channel animChannel = channels.get(ch);

                final AccessorModel input = animChannel.getSampler().getInput();
                final FloatBuffer bufferData = input.getAccessorData().createByteBuffer().asFloatBuffer();

                final AccessorModel output = animChannel.getSampler().getOutput();
                final FloatBuffer transformData = output.getAccessorData().createByteBuffer().asFloatBuffer();

                final String nodeName = animChannel.getNodeModel().getName();
                final int nodeIdx = gltfModel.getNodeModels().indexOf(animChannel.getNodeModel());

                for (int idx = 0; idx < input.getCount(); idx++) {
                    float timeStamp = bufferData.get(idx);

                    KeyFrame keyFrame = times.get(timeStamp);
                    Map<String, JointTransform> transformMap;
                    if (keyFrame != null) {
                        transformMap = keyFrame.getPose();
                    } else {
                        transformMap = new TreeMap<>();
                        keyFrame = new KeyFrame(timeStamp, transformMap);
                        times.put(timeStamp, keyFrame);
                    }

                    String id = String.valueOf(nodeIdx);
                    String name = nodeName;
                    if (name == null) {
                        //int nodeIdx = gltfModel.getNodeModels().indexOf(animChannel.getNodeModel());
                        name = String.valueOf(nodeIdx);
                    }
                    JointTransform jointTransform = transformMap.get(id);
                    if (jointTransform == null) {
                        jointTransform = new JointTransform();
                        transformMap.put(id, jointTransform);
                    }

                    /*if (idx * 3 >= transformData.capacity() - 2){
                        Log.e(TAG, "BufferUnderFlowException: "+idx+", name: "+animChannel.getNodeModel().getName());
                        break;
                    }*/

                    try {
                        if ("translation".equals(animChannel.getPath())) {
                            float[] transform = new float[3];
                            transformData.get(transform, 0, 3); // 3 components for translation (float x,float y,float z)
                            jointTransform.setLocation(transform);
                        } else if ("rotation".equals(animChannel.getPath())) {
                            float[] transform = new float[4];
                            transformData.get(transform, 0, 4); // 4 components for quaternion
                            jointTransform.setRotation(new Quaternion(transform[0], transform[1], transform[2], transform[3]).normalize().toAnglesF(null));
                            jointTransform.setQuaternion(new Quaternion(transform[0], transform[1], transform[2], transform[3]));
                        } else if ("scale".equals(animChannel.getPath())) {
                            float[] transform = new float[3];
                            transformData.get(transform, 0, 3); // 3 components to scale (float x,float y,float z)
                            jointTransform.setScale(transform);
                        } else {
                            Log.e(TAG, "Unknown transform: " + animChannel.getPath());
                        }
                    } catch (BufferUnderflowException e) {
                        // ignore
                    }
                }

                if (nodeIdx >= 0 && nodeIdx < nodeList.size()) {
                    nodesFound.add(nodeList.get(nodeIdx));
                }
            }

            final String animationName = animationModel.getName() != null ? animationModel.getName() : "Animation-" + System.identityHashCode(animationModel);
            final Animation animation = new Animation(animationName, times.lastKey(), times.values().toArray(new KeyFrame[0]));

            // register animation
            for (Node node : nodesFound) {
                final Scene scene = node.getScene();
                if (scene.getAnimations() == null || !scene.getAnimations().contains(animation)) {
                    scene.addAnimation(animation);
                }
            }

            // collect animation
            animations.add(animation);
        }

        return animations;
    }
}
