package org.the3deer.android_3d_model_engine.services.gltf;

import android.graphics.Bitmap;
import android.opengl.GLES20;
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
import org.the3deer.android_3d_model_engine.model.impl.OrthographicProjection;
import org.the3deer.android_3d_model_engine.model.impl.PerspectiveProjection;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfCameraDto;
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
import org.the3deer.util.android.AndroidUtils;
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
import de.javagl.jgltf.model.io.Buffers;
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


            // basic
            List<Material> materials = buildMaterialsFromDto(dto);

            // meshes / primitives mapping
            Map<Integer, List<Object3DData>> originalMeshes = buildMeshesFromDto(dto, materials);
            if (originalMeshes.isEmpty()){
                Log.w(TAG, "No meshes found in the DTO.");
                throw new Exception("No meshes found in the DTO.");
            }

            // cameras
            List<Camera> cameras = buildCamerasFromDto(dto);

            // node to mesh mapping
            Map<Integer, List<Object3DData>> meshInstances = buildMeshInstances(dto, originalMeshes);
            if (meshInstances.isEmpty()){
                Log.w(TAG, "No meshes were linked in the DTO. No mesh instances were created.");
                throw new Exception("No meshes were linked in the DTO. No mesh instances were created.");
            }

            // nodes - includes linking (mesh, skin, camera)
            List<Node> nodes = buildNodesFromDto(dto, meshInstances, cameras);

            // skins
            List<Skin> originalSkins = buildSkinsFromDto(dto);

            // node to skin mapping
            Map<Integer, List<Skin>> skinsMap = buildSkinInstances(dto, originalSkins, nodes, meshInstances);

            // load animations
            List<Animation> animations = loadAnimations(dto, nodes);

            // collect meshes
            List<Object3DData> allMeshes = new ArrayList<>();
            for (List<Object3DData> meshList : meshInstances.values()) {
                allMeshes.addAll(meshList);
            }

            // collect skins
            List<Skin> allSkins = new ArrayList<>();
            for (List<Skin> skins : skinsMap.values()){
                allSkins.addAll(skins);
            }

            return new GltfSceneData(dto, nodes, allMeshes, materials, allSkins, animations, cameras);
        }
    }

    private List<Material> buildMaterialsFromDto(GltfDto dto) {
        if (dto.materials == null || dto.materials.isEmpty()) {
            return Collections.emptyList();
        }

        List<Material> materials = new ArrayList<>(dto.materials.size());
        for (int i = 0; i < dto.materials.size(); i++) {
            GltfMaterialDto materialDto = dto.materials.get(i);
            Material material = new Material(materialDto.name, materialDto.name);

            // Base color and alpha
            material.setDiffuse(materialDto.baseColorFactor);
            material.setAlphaCutoff(materialDto.alphaCutoff);
            try {
                if (materialDto.alphaMode != null) {
                    material.setAlphaMode(Material.AlphaMode.valueOf(materialDto.alphaMode));
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to set alpha mode: " + materialDto.alphaMode);
            }

            // Base color texture
            if (materialDto.baseColorTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.baseColorTexture));
                    material.setColorTexture(new Texture(materialDto.name + "_color", bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decode base color texture for material: " + materialDto.name, e);
                }
            }

            // Normal map
            if (materialDto.normalTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.normalTexture));
                    material.setNormalTexture(new Texture(materialDto.name + "_normal", bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decode normal texture for material: " + materialDto.name, e);
                }
            }

            // Emissive map and factor
            material.setEmissiveFactor(materialDto.emissiveFactor);
            if (materialDto.emissiveTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.emissiveTexture));
                    material.setEmissiveTexture(new Texture(materialDto.name + "_emissive", bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decode emissive texture for material: " + materialDto.name, e);
                }
            }

            // KHR_materials_volume properties
            material.setThicknessFactor(materialDto.thicknessFactor);
            material.setAttenuationDistance(materialDto.attenuationDistance);
            material.setAttenuationColor(materialDto.attenuationColor);
            if (materialDto.thicknessTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.thicknessTexture));
                    material.setTransmissionTexture(new Texture(materialDto.name + "_thickness", bitmap));
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decode thickness texture for material: " + materialDto.name, e);
                }
            }

            materials.add(material);
        }

        return materials;
    }

    private List<Camera> buildCamerasFromDto(GltfDto dto) {
        if (dto.cameras == null || dto.cameras.isEmpty()) {
            return Collections.emptyList();
        }

        List<Camera> cameras = new ArrayList<>(dto.cameras.size());
        for (GltfCameraDto cameraDto : dto.cameras) {
            String cameraName = cameraDto.name;
            if (cameraName == null) {
                cameraName = "Camera " + dto.cameras.indexOf(cameraDto);
            }

            Camera camera = new Camera(cameraName);
            if ("perspective".equals(cameraDto.type)) {
                camera.setProjection(new PerspectiveProjection(
                        cameraDto.yfov != null ? cameraDto.yfov : (float) Math.toRadians(60.0),
                        cameraDto.aspectRatio != null ? cameraDto.aspectRatio : 1.0f,
                        cameraDto.znear != null ? cameraDto.znear : 0.1f,
                        cameraDto.zfar != null ? cameraDto.zfar : 1000.0f
                ));
            } else if ("orthographic".equals(cameraDto.type)) {
                camera.setProjection(new OrthographicProjection(
                        cameraDto.xmag != null ? cameraDto.xmag : 1.0f,
                        cameraDto.ymag != null ? cameraDto.ymag : 1.0f,
                        cameraDto.znear != null ? cameraDto.znear : 0.1f,
                        cameraDto.zfar != null ? cameraDto.zfar : 1000.0f
                ));
            }
            cameras.add(camera);
        }
        return cameras;
    }

    private List<Node> buildNodesFromDto(GltfDto dto, Map<Integer, List<Object3DData>> meshInstancesMap,
                                         List<Camera> cameras) {
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

            node.setName(nodeDto.name != null ? nodeDto.name : "Node_" + i);

            if (nodeDto.scale != null || nodeDto.translation != null || nodeDto.rotation != null) {
                node.setLocalTransform(new Transform(floatArrayToFloatWrapperArray(nodeDto.scale),
                        nodeDto.rotation != null ? new Quaternion(nodeDto.rotation) : null,
                        floatArrayToFloatWrapperArray(nodeDto.translation)
                ));
            } else if (nodeDto.matrix != null) {
                node.setLocalTransform(new Transform(nodeDto.matrix));
            }

            if (nodeDto.children != null) {
                for (Integer childIndex : nodeDto.children) {
                    Node childNode = nodes.get(childIndex);
                    node.addChild(childNode);
                    childNode.setParent(node);
                }
            }

            if (nodeDto.cameraIndex != null && nodeDto.cameraIndex < cameras.size()) {
                Camera camera = cameras.get(nodeDto.cameraIndex);
                node.setCamera(camera);
                camera.setNode(node);
            }

            if (nodeDto.meshIndex != null) {
                if (meshInstancesMap == null || !meshInstancesMap.containsKey(i)) {
                    Log.w(TAG, "Node " + i + " has mesh index but no mesh instances were registered for this node. This may indicate a mismatch between nodes and meshes in the DTO.");
                } else if (meshInstancesMap.get(i).isEmpty()) {
                    Log.w(TAG, "Node " + i + " has mesh index but no mesh instances were found for this node. This may indicate a mismatch between nodes and meshes in the DTO.");
                } else{
                    final List<Object3DData> meshInstances = meshInstancesMap.get(i);
                    node.setMeshes(meshInstances);

                    // mesh assignment
                    if (meshInstances != null) {
                        for (Object3DData meshInstance : meshInstances) {
                            meshInstance.setParentNode(node);
                        }
                    }
                }
            }
        }
        return nodes;
    }

    /**
     * After building the initial nodes and meshes, we need to loop through the nodes again
     * to assign mesh instances to each node that references a mesh.
     *
     * @param dto
     * @param originalMeshes
     * @return a map of node index to list of mesh instances assigned to that node.
     */
    private Map<Integer, List<Object3DData>> buildMeshInstances(GltfDto dto, Map<Integer, List<Object3DData>> originalMeshes) {

        // check if there are any nodes to process
        if (dto.nodes == null || dto.nodes.isEmpty()) {
            Log.w(TAG, "No nodes found in the DTO. No mesh instances will be created.");
            return Collections.emptyMap();
        }

        // mapping node->mesh
        final Map<Integer, List<Object3DData>> meshMap = new HashMap<>();

        // loop through nodes and assign mesh instances to each node that references a mesh
        for (int i = 0; i < dto.nodes.size(); i++) {
            GltfNodeDto nodeDto = dto.nodes.get(i);

            // check if this node has a mesh, if not skip mesh assignment for this node
            if (nodeDto.meshIndex == null) {
                continue;
            }

            // get mesh template for this node's mesh index
            // This is crucial for models like the chess set where multiple nodes use the same mesh.
            final List<Object3DData> meshTemplates = originalMeshes.get(nodeDto.meshIndex);
            if (meshTemplates == null || meshTemplates.isEmpty()) {
                Log.w(TAG, "Node " + i + " references mesh index " + nodeDto.meshIndex +
                        " but no meshes were loaded for this index. Skipping mesh assignment.");
                continue;
            }

            final List<Object3DData> meshes = new ArrayList<>(meshTemplates.size());
            for (int j = 0; j < meshTemplates.size(); j++) {
                final Object3DData meshTemplate = meshTemplates.get(j);
                Object3DData meshInstance = meshTemplate.clone();
                meshInstance.setId(meshTemplate.getId() + "_mesh_" + j + "_node_" + i);
                meshes.add(meshInstance);
            }

            // register meshes
            meshMap.put(i, meshes);
        }

        return meshMap;
    }

    private static Float[] floatArrayToFloatWrapperArray(float[] primitiveArray) {
        if (primitiveArray == null) return null;
        Float[] wrapperArray = new Float[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            wrapperArray[i] = primitiveArray[i];
        }
        return wrapperArray;
    }

    private Map<Integer, List<Object3DData>> buildMeshesFromDto(GltfDto dto, List<Material> materials) {
        if (dto.meshes == null || dto.meshes.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Integer, List<Object3DData>> ret = new TreeMap<>();

        for (int i = 0; i < dto.meshes.size(); i++) {
            GltfMeshDto meshDto = dto.meshes.get(i);
            if (meshDto.primitives == null) continue;
            if (meshDto.primitives.isEmpty()) continue;

            final List<Object3DData> allMeshes = new ArrayList<>(meshDto.primitives.size());
            for (GltfPrimitiveDto primitiveDto : meshDto.primitives) {

                AnimatedModel model = new AnimatedModel();
                model.setId(meshDto.name != null ? meshDto.name + "_" + i : "mesh_" + i);
                model.setVertexBuffer((FloatBuffer) primitiveDto.positions);
                model.setNormalsBuffer((FloatBuffer) primitiveDto.normals);
                model.setTangentBuffer(primitiveDto.tangents);
                model.setTextureCoordsArrayBuffer((FloatBuffer) primitiveDto.texCoords);
                model.setColorsBuffer(primitiveDto.colors);
                model.setIndexBuffer(primitiveDto.indices);
                model.setDrawUsingArrays(primitiveDto.indices == null);
                model.setDrawMode(GLES20.GL_TRIANGLES);

                if (primitiveDto.materialIndex != null) {
                    model.setMaterial(materials.get(primitiveDto.materialIndex));
                }

                allMeshes.add(model);
            }

            ret.put(i, allMeshes);
        }
        return ret;
    }


    private List<Skin> buildSkinsFromDto(GltfDto dto) {
        if (dto.skins == null || dto.skins.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Skin> skins = new ArrayList<>();
        for (int i = 0; i < dto.skins.size(); i++) {
            GltfSkinDto skinDto = dto.skins.get(i);

            float[] ibmArray = null;
            if (skinDto.inverseBindMatrices != null) {
                skinDto.inverseBindMatrices.rewind();
                ibmArray = new float[skinDto.inverseBindMatrices.capacity()];
                try {
                    ((FloatBuffer) skinDto.inverseBindMatrices).get(ibmArray);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to read inverse bind matrix for skin " + skinDto.name, e);
                }
            }

            // Convert the inverse bind matrix buffer to a flat, transposed array
            /*float[] ibmArrayTransposed = new float[ibmArray.length];
            int matrixCount = ibmArray.length / 16;
            for (int m = 0; m < matrixCount; m++) {
                Matrix.transposeM(ibmArrayTransposed, m * 16, ibmArray, m * 16);
            }*/

            int[] joints = new int[skinDto.jointNodeIndices.size()];
            for (int j = 0; j < skinDto.jointNodeIndices.size(); j++) {
                joints[j] = skinDto.jointNodeIndices.get(j);
            }

            String skinName = skinDto.name != null ? skinDto.name : "Skin_" + i;
            Skin skin = new Skin(
                    skinName,
                    ibmArray,
                    joints
            );

            skins.add(skin);
        }
        return skins;
    }

    private Map<Integer,List<Skin>> buildSkinInstances(GltfDto dto, List<Skin> skins, List<Node> nodes, Map<Integer,List<Object3DData>> meshInstancesMap) {

        // Iterate over each skin DTO
        for (int i = 0; i < dto.skins.size(); i++) {
            GltfSkinDto skinDto = dto.skins.get(i);

            // get the Skin(s) corresponding to this skinDto
            final Skin skin = skins.get(i);
            if (skin == null) {
                Log.e(TAG, "Skin " + i + " is null. Skipping index assignment for this skin.");
                continue;
            }

            // link root node
            if (skinDto.skeletonRootNodeIndex != null) {
                skin.setRootJoint(nodes.get(skinDto.skeletonRootNodeIndex));
            }

            // link indexes
            final String[] jointNames = new String[skinDto.jointNodeIndices.size()];
            for (int j = 0; j < skinDto.jointNodeIndices.size(); j++) {
                Integer nodeIndex = skinDto.jointNodeIndices.get(j);
                Node jointNode = nodes.get(nodeIndex);
                jointNames[j] = jointNode.getName();
                jointNode.setJointIndex(j);
            }
            skin.setJointNames(jointNames);
        }

        final Map<Integer,List<Skin>> ret = new HashMap<>();

        for (int i = 0; i < dto.nodes.size(); i++) {

            GltfNodeDto nodeDto = dto.nodes.get(i);
            Node node = nodes.get(i);

            if (nodeDto.skinIndex != null) {

                // check if meshes are assigned to this node, if not skip skin assignment for this node
                if (node.getMeshes() == null || node.getMeshes().isEmpty() || meshInstancesMap == null || meshInstancesMap.isEmpty()) {
                    Log.w(TAG, "Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no meshes we loaded for this node. Skipping skin assignment for this node.");
                    continue;
                }

                // check if skins are available for this skin index
                if (skins == null || skins.isEmpty()) {
                    Log.e(TAG, "Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no skins were loaded. Skipping skin assignment for this node.");
                    continue;
                }

                // get the corresponding skin for this skin index
                final Skin skinTemplate = skins.get(nodeDto.skinIndex);
                if (skinTemplate == null) {
                    Log.e(TAG, "Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no skin was found for this index. Skipping skin assignment for this node.");
                    continue;
                }

                // get mesh instances for this node
                final List<Object3DData> meshInstances = meshInstancesMap.get(i);

                // check if mesh instances are available for this node
                if (meshInstances == null || meshInstances.isEmpty()) {
                    Log.e(TAG, "Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no mesh instances were found for this node. Skipping skin assignment for this node.");
                    continue;
                }

                // get mesh primitive
                GltfMeshDto meshDto = dto.meshes.get(nodeDto.meshIndex);
                if (meshDto.primitives == null || meshDto.primitives.isEmpty()) {
                    Log.w(TAG, "Mesh index " + nodeDto.meshIndex + " has no primitives defined. Skipping skin assignment for meshes of this node.");
                    continue;
                }

                // check coherence between gltf meshes and mesh instances for this node
                if (meshInstances.size() != meshDto.primitives.size()) {
                    Log.e(TAG, "Node " + i + " ("+node.getName()+") references skin index " + nodeDto.skinIndex +
                            " but the number of mesh instances (" + meshInstances.size() + ") does not match the expected number of primitives based on the mesh index (" + meshDto.primitives.size() + "). This may indicate a mismatch between nodes and meshes in the DTO.");
                    continue;
                }

                final List<Skin> cloneSkins = new ArrayList<>();

                // loop over all mesh instances of this node and assign the skin to each mesh instance
                for (int m = 0; m < meshDto.primitives.size(); m++) {

                    // get primitive for this mesh instance
                    final GltfPrimitiveDto primitiveDto = meshDto.primitives.get(m);
                    if (primitiveDto == null) {
                        Log.w(TAG, "Primitive index " + m + " for mesh index " + nodeDto.meshIndex + " is null. Skipping skin assignment for this mesh instance.");
                        continue;
                    }

                    // get the corresponding mesh instance for this primitive
                    final Object3DData object3DData = meshInstances.get(m);
                    if (object3DData instanceof AnimatedModel) {

                        // clone skin
                        final Skin clone = skinTemplate.clone();

                        // link buffers
                        clone.setWeightsBuffer(primitiveDto.weights);
                        clone.setJointsBuffer(primitiveDto.jointIds);
                        clone.setJointComponents(primitiveDto.jointIdsComponents);
                        clone.setWeightsComponents(primitiveDto.weightsComponents);

                        // assign skin to mesh instance
                        ((AnimatedModel) object3DData).setSkin(clone);

                        // register skin
                        cloneSkins.add(clone);

                    } else {
                        Log.w(TAG, "Mesh instance for node " + i + " and mesh index " + nodeDto.meshIndex +
                                " is not an AnimatedModel. Skipping skin assignment for this mesh instance.");
                    }
                }

                // register skin list
                ret.put(i, cloneSkins);
            }
        }

        return ret;
    }

    private List<Animation> loadAnimations(GltfDto dto, List<Node> nodes) {
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

                final Node targetNode = nodes.get(channel.targetNode);
                final String jointId = targetNode.getId();

                for (int i = 0; i < times.capacity(); i++) {
                    final float timeStamp = times.get(i);

                    KeyFrame keyFrame = keyframes.computeIfAbsent(timeStamp, k -> new KeyFrame(k, new TreeMap<>()));
                    Map<String, JointTransform> pose = keyFrame.getPose();

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
                            jointTransform.setQuaternion(new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]));
                        } else if ("scale".equals(channel.targetPath)) {
                            float[] scale = new float[3];
                            values.position(i * 3);
                            values.get(scale);
                            jointTransform.setScale(scale);
                        }
                    } catch (BufferUnderflowException e) {
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

    public List<Scene> createScenes(GltfSceneData sceneData) {
        List<Scene> finalScenes = new ArrayList<>();
        final var dto = sceneData.dto;
        final var allNodes = sceneData.nodes;

        if (dto.scenes != null && !dto.scenes.isEmpty()) {
            Log.i(TAG, "Found " + dto.scenes.size() + " scene(s) defined in the file.");
            for (GltfSceneDto sceneDto : dto.scenes) {
                Scene scene = new SceneImpl();
                scene.setName(sceneDto.name);

                List<Node> rootNodes = new ArrayList<>();
                if (sceneDto.nodes != null) {
                    for (Integer nodeIndex : sceneDto.nodes) {
                        if (nodeIndex < allNodes.size()) {
                            rootNodes.add(allNodes.get(nodeIndex));
                        }
                    }
                }
                scene.setRootNodes(rootNodes);

                // camera configuration
                scene.setCameras(sceneData.cameras);
                // set default camera from the gltf model
                Camera defaultCamera = findFirstCamera(rootNodes);
                if (defaultCamera != null) {
                    scene.setCamera(defaultCamera);
                } else {
                    Log.i(TAG, "No camera found in the scene. Using default camera.");
                }

                List<Object3DData> sceneObjects = collectMeshes(rootNodes);
                scene.setObjects(sceneObjects);

                if (sceneData.skins != null && !sceneData.skins.isEmpty()) {
                    for (Skin skin : sceneData.skins) {
                        scene.addSkeleton(skin);
                    }
                }

                if (sceneData.animations != null && !sceneData.animations.isEmpty()) {

                    List<Node> sceneNodes = collectNodes(rootNodes);

                    Set<String> sceneNodeIds = new HashSet<>();
                    for (Node node : sceneNodes) {
                        sceneNodeIds.add(node.getId());
                    }

                    List<Animation> sceneAnimations = new ArrayList<>();
                    for (Animation anim : sceneData.animations) {
                        boolean belongsToScene = false;
                        if (anim.getKeyFrames() != null && anim.getKeyFrames().length > 0) {
                            Map<String, JointTransform> firstPose = anim.getKeyFrames()[0].getPose();
                            if (firstPose != null) {
                                for (String jointId : firstPose.keySet()) {
                                    if (sceneNodeIds.contains(jointId)) {
                                        belongsToScene = true;
                                        Log.d(TAG, "Found animation belonging to the scene.");
                                        break;
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

                finalScenes.add(scene);

                scene.onLoadComplete();
            }
        } else {
            Log.w(TAG, "Gltf file has no scenes defined. Creating a default scene.");
            Scene defaultScene = new SceneImpl();
            defaultScene.setName("Default Scene");

            List<Node> rootNodes = new ArrayList<>();
            for (Node node : allNodes) {
                if (node.getParent() == null) {
                    rootNodes.add(node);
                }
            }
            defaultScene.setRootNodes(rootNodes);
            List<Object3DData> sceneObjects = collectMeshes(rootNodes);
            defaultScene.setObjects(sceneObjects);
            finalScenes.add(defaultScene);

            defaultScene.onLoadComplete();
        }
        return finalScenes;
    }

    private Camera findFirstCamera(List<Node> nodes) {
        Stack<Node> stack = new Stack<>();
        stack.addAll(nodes);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node.getCamera() != null) {
                return node.getCamera();
            }
            if (node.getChildren() != null) {
                stack.addAll(node.getChildren());
            }
        }
        return null;
    }

    private List<Object3DData> collectMeshes(List<Node> nodes) {
        List<Object3DData> collected = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.addAll(nodes);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            if (node.getMeshes() != null) {
                collected.addAll(node.getMeshes());
            }
            if (node.getChildren() != null) {
                stack.addAll(node.getChildren());
            }
        }
        return collected;
    }

    private List<Node> collectNodes(List<Node> nodes) {
        List<Node> collected = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        stack.addAll(nodes);
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            collected.add(node);
            if (node.getChildren() != null) {
                stack.addAll(node.getChildren());
            }
        }
        return collected;
    }
}
