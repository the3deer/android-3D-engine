package org.the3deer.android_3d_model_engine.services.gltf;

import android.util.Log;

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
import org.the3deer.android_3d_model_engine.model.Transform;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSamplerDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneData;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSkinDto;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.math.Quaternion;

import java.io.InputStream;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReferenceResolver;
import de.javagl.jgltf.model.io.IO;

public class GltfLoader {

    private static final String TAG = GltfLoader.class.getSimpleName();

    public GltfLoader() {
    }

    public GltfSceneData load(URI uri, LoadListener callback) throws Exception {

        try (InputStream stream = ContentUtils.getInputStream(uri)) {
            Log.i(TAG, "Loading and parsing model file... " + uri);
            callback.onProgress("Parsing " + uri);

            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(stream);
            URI baseUri = IO.getParent(uri);
            GltfReferenceResolver.resolveAll(gltfAsset.getReferences(), baseUri);
            GltfModel gltfModel = GltfModels.create(gltfAsset);
            GltfParser parser = new GltfParser(gltfAsset, gltfModel);
            GltfDto dto = parser.parse();

            Log.i(TAG, "Building engine objects from DTO...");
            callback.onProgress("Building objects");

            Map<Integer, GltfNodeDto> skinToNodeMap = new HashMap<>();
            for (GltfNodeDto nodeDto : dto.nodes) {
                if (nodeDto.skinIndex != null) {
                    skinToNodeMap.put(nodeDto.skinIndex, nodeDto);
                }
            }

            List<Material> materials = buildMaterialsFromDto(dto);
            Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap = new HashMap<>();
            List<Object3DData> meshes = buildMeshesFromDto(dto, materials, meshToPrimitiveMap);
            List<Skin> skins = buildSkinsFromDto(dto, skinToNodeMap, meshToPrimitiveMap);
            List<Node> nodes = buildNodesFromDto(dto, meshes, skins);
            linkSkinsToSkeletons(dto, skins, nodes);

            List<Animation> animations = loadAnimations(dto, nodes);

            return new GltfSceneData(dto, nodes, meshes, materials, skins, animations);
        }
    }

    private void linkSkinsToSkeletons(GltfDto dto, List<Skin> skins, List<Node> nodes) {
        for (int i=0; i<skins.size(); i++){
            GltfSkinDto skinDto = dto.skins.get(i);
            Skin skin = skins.get(i);
            if (skinDto.skeletonRootNodeIndex != null) {
                skin.setSkeleton(nodes.get(skinDto.skeletonRootNodeIndex));
            }
            String[] jointNames = new String[skinDto.jointNodeIndices.size()];
            for (int j=0; j<skinDto.jointNodeIndices.size(); j++){
                Integer nodeIndex = skinDto.jointNodeIndices.get(j);
                Node jointNode = nodes.get(nodeIndex);
                jointNames[j] = jointNode.getName();
                jointNode.setJointIndex(j);
            }
            skin.setJointNames(jointNames);
        }
    }

    private List<Material> buildMaterialsFromDto(GltfDto dto) {
        if (dto.materials == null || dto.materials.isEmpty()) {
            return Collections.emptyList();
        }

        List<Material> materials = new ArrayList<>(dto.materials.size());
        for (int i = 0; i < dto.materials.size(); i++) {
            GltfMaterialDto materialDto = dto.materials.get(i);
            Material material = new Material();
            material.setName(materialDto.name);
            material.setDiffuse(materialDto.baseColorFactor);

            if (materialDto.imageData != null) {
                material.setColorTexture(new Texture().setBuffer(materialDto.imageData));
            }

            materials.add(material);
        }

        return materials;
    }

    private List<Node> buildNodesFromDto(GltfDto dto, List<Object3DData> meshes, List<Skin> skins) {
        if (dto.nodes == null || dto.nodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Node> nodes = new ArrayList<>(dto.nodes.size());
        for (int i = 0; i < dto.nodes.size(); i++) {
            Node node = new Node(i);
            nodes.add(node);
        }

        for (int i = 0; i < dto.nodes.size(); i++) {
            GltfNodeDto nodeDto = dto.nodes.get(i);
            Node node = nodes.get(i);

            node.setName(nodeDto.name);

            if (nodeDto.matrix != null) {
                node.setMatrix(nodeDto.matrix);
            } else {
                node.setLocalTransform(new Transform(floatArrayToFloatWrapperArray(nodeDto.scale),
                        nodeDto.rotation != null ? new Quaternion(nodeDto.rotation) : null,
                        floatArrayToFloatWrapperArray(nodeDto.translation)
                ));
            }

            if (nodeDto.children != null) {
                for (Integer childIndex : nodeDto.children) {
                    Node childNode = nodes.get(childIndex);
                    node.addChild(childNode);
                    childNode.setParent(node);
                }
            }

            if (nodeDto.meshIndex != null) {
                Object3DData mesh = meshes.get(nodeDto.meshIndex);
                node.setMesh(mesh);
                mesh.setParentNode(node);

                if (nodeDto.skinIndex != null && mesh instanceof AnimatedModel) {

                    if (nodeDto.skinIndex >= skins.size()) {
                        Log.w(TAG, "Node " + i + " references skin index " + nodeDto.skinIndex +
                                " but only " + skins.size() + " skins were loaded. Skipping skin assignment.");
                    } else {
                        ((AnimatedModel) mesh).setSkin(skins.get(nodeDto.skinIndex));
                    }
                }
            }
        }
        return nodes;
    }

    private static Float[] floatArrayToFloatWrapperArray(float[] primitiveArray) {
        if (primitiveArray == null) return null;
        Float[] wrapperArray = new Float[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            wrapperArray[i] = primitiveArray[i];
        }
        return wrapperArray;
    }

    private List<Object3DData> buildMeshesFromDto(GltfDto dto, List<Material> materials,
                                                  Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap) {
        if (dto.meshes == null || dto.meshes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object3DData> allPrimitives = new ArrayList<>();
        for (int i = 0; i < dto.meshes.size(); i++) {
            GltfMeshDto meshDto = dto.meshes.get(i);
            if (meshDto.primitives == null) continue;

            if (!meshDto.primitives.isEmpty()){
                GltfPrimitiveDto primitiveDto = meshDto.primitives.get(0);

                AnimatedModel model = new AnimatedModel();
                model.setId(meshDto.name != null ? meshDto.name + "_" + i : "mesh_" + i);
                model.setVertexArrayBuffer((FloatBuffer) primitiveDto.positions);
                model.setVertexNormalsArrayBuffer((FloatBuffer) primitiveDto.normals);
                model.setTangentBuffer(primitiveDto.tangents);
                model.setTextureCoordsArrayBuffer((FloatBuffer) primitiveDto.texCoords);
                model.setVertexColorsArrayBuffer(primitiveDto.colors);
                model.setIndexBuffer(primitiveDto.indices);
                model.setDrawUsingArrays(primitiveDto.indices == null);
                model.setDrawMode(android.opengl.GLES20.GL_TRIANGLES);

                if (primitiveDto.materialIndex != null) {
                    model.setMaterial(materials.get(primitiveDto.materialIndex));
                }

                allPrimitives.add(model);

                meshToPrimitiveMap.put(i, primitiveDto);
            }
        }
        return allPrimitives;
    }


    private List<Skin> buildSkinsFromDto(GltfDto dto, Map<Integer, GltfNodeDto> skinToNodeMap,
                                         Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap) {
        if (dto.skins == null || dto.skins.isEmpty()) {
            return Collections.emptyList();
        }

        List<Skin> skins = new ArrayList<>();
        for (int i = 0; i < dto.skins.size(); i++) {
            GltfSkinDto skinDto = dto.skins.get(i);
            GltfNodeDto skinnedNodeDto = skinToNodeMap.get(i);

            if (skinnedNodeDto == null || skinnedNodeDto.meshIndex == null) {
                Log.w(TAG, "Skin " + i + " is not linked to a node with a mesh. Skipping.");
                skins.add(new Skin()); 
                continue;
            }

            GltfPrimitiveDto primitiveDto = meshToPrimitiveMap.get(skinnedNodeDto.meshIndex);
            if (primitiveDto == null) {
                Log.w(TAG, "Could not find mesh primitive for skin " + i + ". Skipping.");
                skins.add(new Skin()); 
                continue;
            }

            if (skinDto.inverseBindMatrices == null) continue;

            skinDto.inverseBindMatrices.rewind();
            float[] ibmArray = new float[skinDto.inverseBindMatrices.capacity()];
            ((FloatBuffer) skinDto.inverseBindMatrices).get(ibmArray);

            int[] joints = new int[skinDto.jointNodeIndices.size()];
            for (int j = 0; j < skinDto.jointNodeIndices.size(); j++) {
                joints[j] = skinDto.jointNodeIndices.get(j);
            }

            Skin skin = new Skin(
                    skinDto.name,
                    null, 
                    primitiveDto.jointIds,
                    primitiveDto.weights,
                    ibmArray,
                    joints
            );
            skin.setJointComponents(primitiveDto.jointIdsComponents);
            skin.setWeightsComponents(primitiveDto.weightsComponents);

            skins.add(skin);
        }
        return skins;
    }

    public List<Animation> loadAnimations(GltfDto dto, List<Node> nodes) {
        if (dto.animations == null || dto.animations.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Animation> animations = new ArrayList<>();
        for (GltfAnimationDto animDto : dto.animations) {

            if (animDto.channels == null || animDto.channels.isEmpty()) {
                continue;
            }

            final TreeMap<Float, KeyFrame> keyframes = new TreeMap<>();

            for (GltfChannelDto channel : animDto.channels) {
                final GltfSamplerDto sampler = animDto.samplers.get(channel.sampler);
                final FloatBuffer times = (FloatBuffer) sampler.input;
                final FloatBuffer values = (FloatBuffer) sampler.output;

                // Ensure buffers are ready for reading from the start
                //times.rewind();
                //values.rewind();

                final Node targetNode = nodes.get(channel.targetNode);
                final String jointId = targetNode.getId();

                for (int i = 0; i < times.capacity(); i++) {
                    final float timeStamp = times.get(i);

                    // Get or create the KeyFrame for this timestamp
                    KeyFrame keyFrame = keyframes.computeIfAbsent(timeStamp, k -> new KeyFrame(k, new TreeMap<>()));
                    Map<String, JointTransform> pose = keyFrame.getPose();

                    // Get or create the JointTransform for the target node in this pose
                    JointTransform jointTransform = pose.computeIfAbsent(jointId, k -> new JointTransform());

                    try {
                        if ("translation".equals(channel.targetPath)) {
                            float[] translation = new float[3];
                            values.position(i * 3);
                            values.get(translation);
                            jointTransform.setLocation(translation);
                        } else if ("rotation".equals(channel.targetPath)) {
                            float[] rotation = new float[4];
                            values.position(i * 4);
                            values.get(rotation);
                            // Assuming your JointTransform or Quaternion class can handle this
                            jointTransform.setQuaternion(new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]).normalize());
                        } else if ("scale".equals(channel.targetPath)) {
                            float[] scale = new float[3];
                            values.position(i * 3);
                            values.get(scale);
                            jointTransform.setScale(scale);
                        }
                    } catch (BufferUnderflowException e){
                        // This can happen if animation data is corrupt, ignore this keyframe for this channel
                    }
                }
            }

            if (!keyframes.isEmpty()) {
                final String animationName = animDto.name != null ? animDto.name : "Animation-" + System.identityHashCode(animDto);
                final Animation animation = new Animation(animationName, keyframes.lastKey(), keyframes.values().toArray(new KeyFrame[0]));
                animations.add(animation);
            }
        }
        return animations;
    }

    /**
     * Assembles a list of renderable Scene objects from the raw data loaded from a glTF file.
     *
     * @param sceneData The raw data container from the GltfLoader.
     * @return A list of fully assembled scenes.
     */
    public List<Scene> createScenes(GltfSceneData sceneData) {
        List<Scene> finalScenes = new ArrayList<>();
        final var dto = sceneData.dto;
        final var allNodes = sceneData.nodes;
        final var allMeshes = sceneData.meshes;

        // CASE 1: The glTF file defines scenes.
        if (dto.scenes != null && !dto.scenes.isEmpty()) {
            Log.i(TAG, "Found " + dto.scenes.size() + " scene(s) defined in the file.");
            for (GltfSceneDto sceneDto : dto.scenes) {
                Scene scene = new SceneImpl();
                scene.setName(sceneDto.name);

                // Add the root nodes for THIS specific scene
                List<Node> rootNodes = new ArrayList<>();
                if (sceneDto.nodes != null) {
                    for (Integer nodeIndex : sceneDto.nodes) {
                        if (nodeIndex < allNodes.size()) {
                            rootNodes.add(allNodes.get(nodeIndex));
                        }
                    }
                }
                scene.setRootNodes(rootNodes);
                scene.setCamera(new Camera()); // Give each scene a default camera

                // Traverse the node hierarchy and collect all drawable meshes
                List<Object3DData> sceneObjects = collectMeshes(rootNodes);
                scene.setObjects(sceneObjects);

                // --- register skins ---
                if (sceneData.skins != null && !sceneData.skins.isEmpty()) {
                    for (Skin skin : sceneData.skins) {
                        scene.addSkeleton(skin);
                    }
                }

                // --- START ANIMATION LINKING ---
                if (sceneData.animations != null && !sceneData.animations.isEmpty()) {

                    // Collect all nodes that are part of this scene's hierarchy
                    List<Node> sceneNodes = collectNodes(rootNodes);

                    // Create a Set of scene node IDs for quick lookups
                    Set<String> sceneNodeIds = new HashSet<>();
                    for (Node node : sceneNodes) {
                        sceneNodeIds.add(node.getId());
                    }

                    // Filter animations to only include those that target nodes within this scene
                    List<Animation> sceneAnimations = new ArrayList<>();
                    for (Animation anim : sceneData.animations) {
                        // Check if this animation affects any node in our current scene
                        boolean belongsToScene = false;
                        if (anim.getKeyFrames() != null && anim.getKeyFrames().length > 0) {
                            // We only need to check the first keyframe's pose
                            Map<String, JointTransform> firstPose = anim.getKeyFrames()[0].getPose();
                            if (firstPose != null) {
                                for (String jointId : firstPose.keySet()) {
                                    if (sceneNodeIds.contains(jointId)) {
                                        belongsToScene = true;
                                        Log.d(TAG, "Found animation belonging to the scene.");
                                        break; // Found a match, no need to check further for this animation
                                    }
                                }
                            }
                        }

                        if (belongsToScene) {
                            sceneAnimations.add(anim);
                        }
                    }

                    if (!sceneAnimations.isEmpty()) {
                        scene.setAnimations(sceneAnimations);
                    }
                }
                // --- END ANIMATION LINKING ---

                finalScenes.add(scene);

                scene.onLoadComplete();
            }
        }
        // CASE 2: No scenes are defined. Create a default scene with all root nodes.
        else {
            Log.w(TAG, "Gltf file has no scenes defined. Creating a default scene.");
            Scene defaultScene = new SceneImpl();
            defaultScene.setName("Default Scene");

            // Find all nodes that are not children of any other node.
            List<Node> rootNodes = new ArrayList<>();
            for(Node node : allNodes){
                if(node.getParent() == null){
                    rootNodes.add(node);
                }
            }
            defaultScene.setRootNodes(rootNodes);
            List<Object3DData> sceneObjects = collectMeshes(rootNodes);
            defaultScene.setObjects(sceneObjects);
            defaultScene.setCamera(new Camera());
            finalScenes.add(defaultScene);

            defaultScene.onLoadComplete();
        }
        return finalScenes;
    }

    /**
     * Recursively traverses a list of nodes and their children to collect
     * all associated drawable meshes (Object3DData).
     *
     * @param rootNodes The starting nodes of the hierarchy.
     * @return A flat list of all meshes found in the hierarchy.
     */
    private List<Object3DData> collectMeshes(List<Node> rootNodes) {
        List<Object3DData> collectedMeshes = new ArrayList<>();
        if (rootNodes == null) {
            return collectedMeshes;
        }

        Stack<Node> stack = new Stack<>();
        stack.addAll(rootNodes);

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();

            if (currentNode.getMesh() != null) {
                collectedMeshes.add(currentNode.getMesh());
            }

            if (currentNode.getChildren() != null) {
                for (Node childNode : currentNode.getChildren()) {
                    stack.push(childNode);
                }
            }
        }
        return collectedMeshes;
    }

    // In GltfSceneFactory.java, after collectMeshes()

    /**
     * Recursively traverses a list of nodes and their children to collect
     * all nodes in the hierarchy.
     *
     * @param rootNodes The starting nodes of the hierarchy.
     * @return A flat list of all nodes found in the hierarchy.
     */
    private List<Node> collectNodes(List<Node> rootNodes) {
        List<Node> collectedNodes = new ArrayList<>();
        if (rootNodes == null) {
            return collectedNodes;
        }

        Stack<Node> stack = new Stack<>();
        stack.addAll(rootNodes);

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            collectedNodes.add(currentNode);

            if (currentNode.getChildren() != null) {
                for (Node childNode : currentNode.getChildren()) {
                    stack.push(childNode);
                }
            }
        }
        return collectedNodes;
    }

}
