package org.the3deer.android.engine.services.gltf;

import android.graphics.Bitmap;
import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;

import org.the3deer.android.util.AndroidUtils;
import org.the3deer.android.engine.animation.Animation;
import org.the3deer.android.engine.animation.JointTransform;
import org.the3deer.android.engine.animation.KeyFrame;
import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.OrthographicProjection;
import org.the3deer.android.engine.model.PerspectiveProjection;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Skin;
import org.the3deer.android.engine.model.Texture;
import org.the3deer.android.engine.model.Transform;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android.engine.services.gltf.dto.GltfCameraDto;
import org.the3deer.android.engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android.engine.services.gltf.dto.GltfDto;
import org.the3deer.android.engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android.engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android.engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android.engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSamplerDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSceneData;
import org.the3deer.android.engine.services.gltf.dto.GltfSceneDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSkinDto;
import org.the3deer.util.math.Quaternion;

import java.io.InputStream;
import java.net.URI;
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
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReferenceResolver;
import de.javagl.jgltf.model.io.IO;

public class GltfLoader {

    private static final Logger logger = Logger.getLogger(GltfLoader.class.getSimpleName());

    public GltfLoader() {
    }

    public GltfSceneData load(URI url, LoadListener callback) throws Exception {

        try (InputStream stream = url.toURL().openStream()) {
            logger.info("Loading and parsing model file... " + url);
            callback.onProgress("Parsing " + url);

            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(stream);
            URI baseUri = IO.getParent(URI.create(url.toString()));
            GltfReferenceResolver.resolveAll(gltfAsset.getReferences(), baseUri);
            GltfModel gltfModel = GltfModels.create(gltfAsset);
            GltfParser parser = new GltfParser(gltfAsset, gltfModel);
            GltfDto dto = parser.parse();

            logger.info("Building engine objects from DTO...");
            callback.onProgress("Building objects");


            // basic
            List<Material> materials = buildMaterialsFromDto(dto);

            // meshes / primitives mapping
            Map<Integer, List<Object3D>> originalMeshes = buildMeshesFromDto(dto, materials);
            if (originalMeshes.isEmpty()){
                logger.warning("No meshes found in the DTO.");
                throw new Exception("No meshes found in the DTO.");
            }

            // cameras
            List<Camera> cameras = buildCamerasFromDto(dto);

            // node to mesh mapping
            Map<Integer, List<Object3D>> meshInstances = buildMeshInstances(dto, originalMeshes);
            if (meshInstances.isEmpty()){
                logger.warning("No meshes were linked in the DTO. No mesh instances were created.");
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
            List<Object3D> allMeshes = new ArrayList<>();
            for (List<Object3D> meshList : meshInstances.values()) {
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
                logger.warning("Failed to set alpha mode: " + materialDto.alphaMode);
            }

            // Base color texture
            if (materialDto.baseColorTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.baseColorTexture));
                    material.setColorTexture(new Texture(materialDto.name + "_color", bitmap));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to decode base color texture for material: " + materialDto.name, e);
                }
            }

            // Normal map
            if (materialDto.normalTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.normalTexture));
                    material.setNormalTexture(new Texture(materialDto.name + "_normal", bitmap));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to decode normal texture for material: " + materialDto.name, e);
                }
            }

            // Emissive map and factor
            material.setEmissiveFactor(materialDto.emissiveFactor);
            if (materialDto.emissiveTexture != null) {
                try {
                    Bitmap bitmap = AndroidUtils.decodeBitmap(Buffers.createByteBufferInputStream(materialDto.emissiveTexture));
                    material.setEmissiveTexture(new Texture(materialDto.name + "_emissive", bitmap));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to decode emissive texture for material: " + materialDto.name, e);
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
                    logger.log(Level.SEVERE, "Failed to decode thickness texture for material: " + materialDto.name, e);
                }
            }

            materials.add(material);
        }

        return materials;
    }

    private List<Camera> buildCamerasFromDto(GltfDto dto) {
        if (dto.cameras == null || dto.cameras.isEmpty()) {
            return null;
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
                        cameraDto.yfov != null ? cameraDto.yfov : 60.0f,
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

    private List<Node> buildNodesFromDto(GltfDto dto, Map<Integer, List<Object3D>> meshInstancesMap,
                                         List<Camera> cameras) {
        if (dto.nodes == null || dto.nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // create nodes
        List<Node> nodes = new ArrayList<>(dto.nodes.size());
        for (int i = 0; i < dto.nodes.size(); i++) {
            final GltfNodeDto nodeDto = dto.nodes.get(i);
            final Node node = new Node(i);
            node.setName(nodeDto.name != null ? nodeDto.name : "Node " + i);
            nodes.add(node);
        }

        // configure nodes
        for (int i = 0; i < dto.nodes.size(); i++) {
            GltfNodeDto nodeDto = dto.nodes.get(i);
            Node node = nodes.get(i);

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

            if (nodeDto.cameraIndex != null && cameras != null && nodeDto.cameraIndex < cameras.size()) {
                Camera camera = cameras.get(nodeDto.cameraIndex);
                node.setCamera(camera);
                camera.setNode(node);
            }

            if (nodeDto.meshIndex != null) {
                if (meshInstancesMap == null || !meshInstancesMap.containsKey(i)) {
                    logger.warning("Node " + i + " has mesh index but no mesh instances were registered for this node. This may indicate a mismatch between nodes and meshes in the DTO.");
                } else if (meshInstancesMap.get(i).isEmpty()) {
                    logger.warning("Node " + i + " has mesh index but no mesh instances were found for this node. This may indicate a mismatch between nodes and meshes in the DTO.");
                } else{
                    final List<Object3D> meshInstances = meshInstancesMap.get(i);
                    node.setMeshes(meshInstances);

                    // mesh assignment
                    if (meshInstances != null) {
                        for (Object3D meshInstance : meshInstances) {
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
    private Map<Integer, List<Object3D>> buildMeshInstances(GltfDto dto, Map<Integer, List<Object3D>> originalMeshes) {

        // check if there are any nodes to process
        if (dto.nodes == null || dto.nodes.isEmpty()) {
            logger.warning("No nodes found in the DTO. No mesh instances will be created.");
            return Collections.emptyMap();
        }

        // mapping node->mesh
        final Map<Integer, List<Object3D>> meshMap = new HashMap<>();

        // loop through nodes and assign mesh instances to each node that references a mesh
        for (int i = 0; i < dto.nodes.size(); i++) {
            GltfNodeDto nodeDto = dto.nodes.get(i);

            // check if this node has a mesh, if not skip mesh assignment for this node
            if (nodeDto.meshIndex == null) {
                continue;
            }

            // get mesh template for this node's mesh index
            // This is crucial for models like the chess set where multiple nodes use the same mesh.
            final List<Object3D> meshTemplates = originalMeshes.get(nodeDto.meshIndex);
            if (meshTemplates == null || meshTemplates.isEmpty()) {
                logger.warning( "Node " + i + " references mesh index " + nodeDto.meshIndex +
                        " but no meshes were loaded for this index. Skipping mesh assignment.");
                continue;
            }

            final List<Object3D> meshes = new ArrayList<>(meshTemplates.size());
            for (int j = 0; j < meshTemplates.size(); j++) {
                final Object3D meshTemplate = meshTemplates.get(j);
                Object3D meshInstance = meshTemplate.clone();
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

    private Map<Integer, List<Object3D>> buildMeshesFromDto(GltfDto dto, List<Material> materials) {
        if (dto.meshes == null || dto.meshes.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Integer, List<Object3D>> ret = new TreeMap<>();

        for (int i = 0; i < dto.meshes.size(); i++) {
            GltfMeshDto meshDto = dto.meshes.get(i);
            if (meshDto.primitives == null) continue;
            if (meshDto.primitives.isEmpty()) continue;

            final List<Object3D> allMeshes = new ArrayList<>(meshDto.primitives.size());
            for (GltfPrimitiveDto primitiveDto : meshDto.primitives) {

                AnimatedModel model = new AnimatedModel();
                model.setId(meshDto.name != null ? meshDto.name + "_" + i : "mesh_" + i);
                model.setVertexBuffer((FloatBuffer) primitiveDto.positions);
                model.setNormalsBuffer((FloatBuffer) primitiveDto.normals);
                model.setTangentBuffer(primitiveDto.tangents);
                model.setTextureCoordsArrayBuffer((FloatBuffer) primitiveDto.texCoords);
                model.setColorsBuffer(primitiveDto.colors);
                model.setIndexBuffer(primitiveDto.indices);
                model.setIndexed(primitiveDto.indices != null);
                model.setDrawMode(GLES20.GL_TRIANGLES);

                if (primitiveDto.materialIndex != null) {
                    if (materials == null || primitiveDto.materialIndex >= materials.size()) {
                        logger.log(Level.SEVERE, "Material index " + primitiveDto.materialIndex + " is out of bounds.");
                    } else{
                        model.setMaterial(materials.get(primitiveDto.materialIndex));
                    }
                }

                // If no normals were provided, generate them (facet normals for low-poly models)
                if (primitiveDto.normals == null && primitiveDto.positions != null) {
                    logger.info("Generating normals for primitive of mesh: " + model.getId());
                    model.initNormals();
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
                    logger.log(Level.SEVERE, "Failed to read inverse bind matrix for skin " + skinDto.name, e);
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

    private Map<Integer,List<Skin>> buildSkinInstances(GltfDto dto, List<Skin> skins, List<Node> nodes, Map<Integer,List<Object3D>> meshInstancesMap) {

        // Iterate over each skin DTO
        for (int i = 0; i < dto.skins.size(); i++) {
            GltfSkinDto skinDto = dto.skins.get(i);

            // get the Skin(s) corresponding to this skinDto
            final Skin skin = skins.get(i);
            if (skin == null) {
                logger.log(Level.SEVERE, "Skin " + i + " is null. Skipping index assignment for this skin.");
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
                    logger.warning("Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no meshes we loaded for this node. Skipping skin assignment for this node.");
                    continue;
                }

                // check if skins are available for this skin index
                if (skins == null || skins.isEmpty()) {
                    logger.log(Level.SEVERE, "Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no skins were loaded. Skipping skin assignment for this node.");
                    continue;
                }

                // get the corresponding skin for this skin index
                final Skin skinTemplate = skins.get(nodeDto.skinIndex);
                if (skinTemplate == null) {
                    logger.log(Level.SEVERE,"Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no skin was found for this index. Skipping skin assignment for this node.");
                    continue;
                }

                // get mesh instances for this node
                final List<Object3D> meshInstances = meshInstancesMap.get(i);

                // check if mesh instances are available for this node
                if (meshInstances == null || meshInstances.isEmpty()) {
                    logger.log(Level.SEVERE,"Node " + i + " references skin index " + nodeDto.skinIndex +
                            " but no mesh instances were found for this node. Skipping skin assignment for this node.");
                    continue;
                }

                // get mesh primitive
                GltfMeshDto meshDto = dto.meshes.get(nodeDto.meshIndex);
                if (meshDto.primitives == null || meshDto.primitives.isEmpty()) {
                    logger.warning("Mesh index " + nodeDto.meshIndex + " has no primitives defined. Skipping skin assignment for meshes of this node.");
                    continue;
                }

                // check coherence between gltf meshes and mesh instances for this node
                if (meshInstances.size() != meshDto.primitives.size()){
                    logger.log(Level.SEVERE, "Mesh instance count mismatch for node " + i + ". Expected " + meshDto.primitives.size() + " but found " + meshInstances.size() + ". Skipping skin assignment.");
                    continue;
                }

                final List<Skin> nodeSkins = new ArrayList<>();
                for (int j = 0; j < meshInstances.size(); j++) {
                    final Object3D meshInstance = meshInstances.get(j);
                    final GltfPrimitiveDto primitiveDto = meshDto.primitives.get(j);

                    // clone skin for each mesh instance
                    final Skin skin = skinTemplate.clone();

                    // link joint/weight buffers (unrolled in the parser)
                    skin.setJoints(primitiveDto.jointIds);
                    skin.setWeights(primitiveDto.weights);

                    // assign skin to mesh
                    if (meshInstance instanceof AnimatedModel){
                        ((AnimatedModel) meshInstance).setSkin(skin);
                    }

                    nodeSkins.add(skin);
                }
                ret.put(i, nodeSkins);
            }
        }
        return ret;
    }

    private List<Animation> loadAnimations(GltfDto dto, List<Node> nodes) {
        if (dto.animations == null || dto.animations.isEmpty()) {
            return Collections.emptyList();
        }

        logger.info("Loading animations... Total: " + dto.animations.size());
        final List<Animation> animations = new ArrayList<>();
        for (int a=0; a<dto.animations.size(); a++) {

            // get animation
            final GltfAnimationDto animDto = dto.animations.get(a);

            // animation name
            final String animName = animDto.name != null ? animDto.name : "Animation " + a;

            // initialize keyframes
            final List<KeyFrame> keyframes = new ArrayList<>();

            // check samplers
            if (animDto.samplers == null || animDto.samplers.isEmpty()){
                logger.warning("Animation " + animDto.name + " has no samplers.");
                continue;
            }

            // 1. Identify all unique timestamps across all channels
            final TreeSet<Float> timestamps = new TreeSet<>();
            for (GltfChannelDto channel : animDto.channels) {
                GltfSamplerDto sampler = animDto.samplers.get(channel.samplerIndex);
                if (sampler.input != null) {
                    sampler.input.rewind();
                    while (sampler.input.hasRemaining()) {
                        timestamps.add(sampler.input.get());
                    }
                }
            }

            // 2. Create pose for each timestamp
            for (float time : timestamps) {
                keyframes.add(new KeyFrame(time, new HashMap<>()));
            }

            // 3. Fill pose with channel data
            for (GltfChannelDto channel : animDto.channels) {
                GltfSamplerDto sampler = animDto.samplers.get(channel.samplerIndex);
                Node node = nodes.get(channel.targetNodeIndex);

                sampler.input.rewind();
                sampler.output.rewind();

                for (KeyFrame keyFrame : keyframes) {
                    float time = keyFrame.getTime();

                    // Find value for this time or interpolate
                    // For now, let's assume gltf keyframes align with our timestamps (common)
                    // or just take the first matching or closest one.
                    float[] value = null;
                    sampler.input.rewind();
                    int index = -1;
                    float minDiff = Float.MAX_VALUE;
                    for (int i = 0; sampler.input.hasRemaining(); i++){
                        float t = sampler.input.get();
                        float diff = Math.abs(t - time);
                        if (diff < 0.001f){
                            index = i;
                            break;
                        }
                    }

                    if (index != -1){
                        int stride = 0;
                        if ("translation".equals(channel.targetPath) || "scale".equals(channel.targetPath)) stride = 3;
                        else if ("rotation".equals(channel.targetPath)) stride = 4;

                        if (stride > 0){
                            value = new float[stride];
                            sampler.output.position(index * stride);
                            sampler.output.get(value);
                        }
                    }

                    if (value != null){
                        JointTransform jt = keyFrame.getTransforms().computeIfAbsent(node.getId(), k -> new JointTransform());
                        if ("translation".equals(channel.targetPath)) jt.setLocation(value);
                        else if ("rotation".equals(channel.targetPath)) jt.setRotation(new Quaternion(value));
                        else if ("scale".equals(channel.targetPath)) jt.setScale(value);
                    }
                }
            }

            float duration = timestamps.isEmpty() ? 0 : timestamps.last();
            animations.add(new Animation(animName, duration, keyframes.toArray(new KeyFrame[0])));
        }
        return animations;
    }

    public List<Scene> createScenes(GltfSceneData sceneData) {
        List<Scene> finalScenes = new ArrayList<>();
        final var dto = sceneData.dto;
        final var allNodes = sceneData.nodes;

        if (dto.scenes != null && !dto.scenes.isEmpty()) {
            logger.info("Found " + dto.scenes.size() + " scene(s) defined in the file.");
            for (GltfSceneDto sceneDto : dto.scenes) {
                Scene scene = new Scene();
                if (sceneDto.name != null) {
                    scene.setName(sceneDto.name);
                }

                List<Node> rootNodes = new ArrayList<>();
                if (sceneDto.nodes != null) {
                    for (Integer nodeIndex : sceneDto.nodes) {
                        if (nodeIndex < allNodes.size()) {
                            rootNodes.add(allNodes.get(nodeIndex));
                        }
                    }
                }
                scene.getRootNodes().addAll(rootNodes);

                // camera configuration
                if (sceneData.cameras != null) {
                    scene.getCameras().addAll(sceneData.cameras);
                }

                // set default camera from the gltf model
                /*Camera defaultCamera = findFirstCamera(rootNodes);
                if (defaultCamera != null) {
                    scene.setCamera(defaultCamera);
                } else {
                    logger.info("No camera found in the scene. Using default camera.");
                }*/

                List<Object3D> sceneObjects = collectMeshes(rootNodes);
                scene.getObjects().addAll(sceneObjects);

                if (sceneData.skins != null && !sceneData.skins.isEmpty()) {
                    for (Skin skin : sceneData.skins) {
                        scene.getSkins().add(skin);
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
                                        logger.config("Found animation belonging to the scene.");
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

            }
        } else {
            logger.warning("Gltf file has no scenes defined. Creating a default scene.");
            Scene defaultScene = new Scene();
            defaultScene.setName("Default Scene");

            List<Node> rootNodes = new ArrayList<>();
            for (Node node : allNodes) {
                if (node.getParent() == null) {
                    rootNodes.add(node);
                }
            }
            defaultScene.getRootNodes().addAll(rootNodes);
            List<Object3D> sceneObjects = collectMeshes(rootNodes);
            defaultScene.getObjects().addAll(sceneObjects);
            finalScenes.add(defaultScene);

            defaultScene.update();
        }
        return finalScenes;
    }

    private List<Object3D> collectMeshes(List<Node> nodes) {
        List<Object3D> collected = new ArrayList<>();
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
