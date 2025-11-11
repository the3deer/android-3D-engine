package org.the3deer.android_3d_model_engine.services.gltf;

import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSamplerDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.v1.MaterialModelV1;
import de.javagl.jgltf.model.v2.MaterialModelV2;

public class GltfParser {

    private final GltfModel gltfModel;
    private final GltfAsset gltfAsset;
    private final GltfDto dto = new GltfDto();

    public GltfParser(GltfAsset gltfAsset, GltfModel gltfModel){
        this.gltfAsset = gltfAsset;
        this.gltfModel = gltfModel;
    }

    // Also, update the main parse() method to call the new method
    public GltfDto parse(){
        // The main orchestration method
        parseMeshes();
        parseMaterials();
        parseNodes();
        parseSkins();
        parseScenes();
        parseAnimations();

        return dto;
    }

    private void parseMeshes() {
        List<MeshModel> meshModels = gltfModel.getMeshModels();
        dto.meshes = new ArrayList<>(meshModels.size());

        for (MeshModel meshModel : meshModels) {
            GltfMeshDto meshDto = new GltfMeshDto();
            meshDto.name = meshModel.getName();
            meshDto.primitives = new ArrayList<>();

            for (MeshPrimitiveModel primitiveModel : meshModel.getMeshPrimitiveModels()) {
                GltfPrimitiveDto primitiveDto = new GltfPrimitiveDto();

                // --- Geometry Data ---
                primitiveDto.indices = GltfUtil.createIndicesBuffer(primitiveModel.getIndices());

                // Get attributes from the map
                AccessorModel posAccessor = primitiveModel.getAttributes().get("POSITION");
                if (posAccessor != null) {
                    primitiveDto.positions = GltfUtil.createFloatBuffer(posAccessor);
                }

                AccessorModel normalAccessor = primitiveModel.getAttributes().get("NORMAL");
                if (normalAccessor != null) {
                    primitiveDto.normals = GltfUtil.createFloatBuffer(normalAccessor);
                }

                AccessorModel tangentAccessor = primitiveModel.getAttributes().get("TANGENT");
                if (tangentAccessor != null) {
                    primitiveDto.tangents = GltfUtil.createFloatBuffer(tangentAccessor);
                }

                // --- Texture Coordinates (The Correct Way) ---
                AccessorModel texCoordsAccessor = primitiveModel.getAttributes().get("TEXCOORD_0");
                if (texCoordsAccessor != null) {
                    primitiveDto.texCoords = GltfUtil.createFloatBuffer(texCoordsAccessor);
                }

                // --- Vertex Colors ---
                AccessorModel colorsAccessor = primitiveModel.getAttributes().get("COLOR_0");
                if (colorsAccessor != null){
                    primitiveDto.colors = GltfUtil.createColorsBuffer(colorsAccessor);
                }

                // --- Skinning Data (The Correct Way) ---
                AccessorModel jointsAccessor = primitiveModel.getAttributes().get("JOINTS_0");
                if (jointsAccessor != null) {
                    primitiveDto.jointIds = GltfUtil.createJointsBuffer(jointsAccessor);
                    primitiveDto.jointIdsComponents = jointsAccessor.getElementType().getNumComponents();
                }

                AccessorModel weightsAccessor = primitiveModel.getAttributes().get("WEIGHTS_0");
                if (weightsAccessor != null) {
                    primitiveDto.weights = GltfUtil.createFloatBuffer(weightsAccessor);
                    primitiveDto.weightsComponents = weightsAccessor.getElementType().getNumComponents();
                }

                // --- Material ---
                if (primitiveModel.getMaterialModel() != null) {
                    primitiveDto.materialIndex = gltfModel.getMaterialModels().indexOf(primitiveModel.getMaterialModel());
                }

                meshDto.primitives.add(primitiveDto);
            }
            dto.meshes.add(meshDto);
        }
    }


    private void parseMaterials() {

        List<MaterialModel> materialModels = gltfModel.getMaterialModels();
        dto.materials = new ArrayList<>(materialModels.size());

        for (MaterialModel materialModel : materialModels) {

            GltfMaterialDto materialDto = new GltfMaterialDto();
            materialDto.name = materialModel.getName();
            if (materialModel instanceof MaterialModelV2) {

                MaterialModelV2 materialModelV2 = (MaterialModelV2) materialModel;
                materialDto.baseColorFactor = materialModelV2.getBaseColorFactor();

                if (materialModelV2.getBaseColorTexture() != null && materialModelV2.getBaseColorTexture().getImageModel() != null) {
                    materialDto.imageData = materialModelV2.getBaseColorTexture().getImageModel().getImageData();
                }
            } else if (materialModel instanceof MaterialModelV1){
                MaterialModelV1 materialModelV1 = (MaterialModelV1) materialModel;
                // TODO:
            }

            dto.materials.add(materialDto);
        }
    }


    private void parseNodes() {
        List<NodeModel> nodeModels = gltfModel.getNodeModels();
        dto.nodes = new ArrayList<>(nodeModels.size());

        for (NodeModel nodeModel : nodeModels) {
            GltfNodeDto nodeDto = new GltfNodeDto();
            nodeDto.name = nodeModel.getName();

            // Get the node's local transform matrix
            float[] transform = new float[16];
            nodeModel.computeLocalTransform(transform);
            nodeDto.matrix = transform;

            // Get indices of child nodes
            if (nodeModel.getChildren() != null) {
                nodeDto.children = new ArrayList<>();
                for (NodeModel childNode : nodeModel.getChildren()) {
                    nodeDto.children.add(gltfModel.getNodeModels().indexOf(childNode));
                }
            }

            // Get index of the mesh this node uses
            if (nodeModel.getMeshModel() != null) {
                nodeDto.meshIndex = gltfModel.getMeshModels().indexOf(nodeModel.getMeshModel());
            }

            // Get index of the skin this node uses
            if (nodeModel.getSkinModel() != null) {
                nodeDto.skinIndex = gltfModel.getSkinModels().indexOf(nodeModel.getSkinModel());
            }

            // Get index of the camera this node contains
            if (nodeModel.getCameraModel() != null) {
                nodeDto.cameraIndex = gltfModel.getCameraModels().indexOf(nodeModel.getCameraModel());
            }

            dto.nodes.add(nodeDto);
        }
    }

    private void parseScenes() {
        List<SceneModel> sceneModels = gltfModel.getSceneModels();
        if (sceneModels == null || sceneModels.isEmpty()) {
            dto.scenes = java.util.Collections.emptyList();
            return;
        }

        dto.scenes = new ArrayList<>(sceneModels.size());
        for (SceneModel sceneModel : sceneModels) {
            org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneDto sceneDto = new org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneDto();
            sceneDto.name = sceneModel.getName();

            // Get the indices of the root nodes for this scene
            List<NodeModel> rootNodeModels = sceneModel.getNodeModels();
            if (rootNodeModels != null) {
                sceneDto.nodes = new ArrayList<>();
                for (NodeModel nodeModel : rootNodeModels) {
                    sceneDto.nodes.add(gltfModel.getNodeModels().indexOf(nodeModel));
                }
            }
            dto.scenes.add(sceneDto);
        }
    }

    // Replace the entire parseAnimations method with this corrected version

    private void parseAnimations() {
        List<AnimationModel> animationModels = gltfModel.getAnimationModels();
        if (animationModels == null || animationModels.isEmpty()) {
            dto.animations = java.util.Collections.emptyList();
            return;
        }

        dto.animations = new ArrayList<>(animationModels.size());
        for (AnimationModel animModel : animationModels) {
            GltfAnimationDto animDto = new GltfAnimationDto();
            animDto.name = animModel.getName();

            // This map will help us avoid duplicating samplers
            // Key: The original Sampler object. Value: The index in our new DTO list.
            Map<AnimationModel.Sampler, Integer> samplerMap = new HashMap<>();
            animDto.samplers = new ArrayList<>();
            animDto.channels = new ArrayList<>();

            // 1. Iterate through CHANNELS (this is the correct approach)
            for (AnimationModel.Channel channel : animModel.getChannels()) {

                // 2. Get the sampler for THIS channel
                AnimationModel.Sampler sampler = channel.getSampler();

                // 3. Check if we have already parsed this sampler
                Integer samplerIndex = samplerMap.get(sampler);
                if (samplerIndex == null) {
                    // If not, parse it now and add it to our lists
                    GltfSamplerDto samplerDto = new GltfSamplerDto();
                    samplerDto.interpolation = sampler.getInterpolation();
                    samplerDto.input = GltfUtil.createFloatBuffer(sampler.getInput());
                    samplerDto.output = GltfUtil.createFloatBuffer(sampler.getOutput());

                    // Add to the DTO's sampler list and record its new index
                    animDto.samplers.add(samplerDto);
                    samplerIndex = animDto.samplers.size() - 1;
                    samplerMap.put(sampler, samplerIndex);
                }

                // 4. Now, parse the channel and point it to the correct sampler index
                GltfChannelDto channelDto = new GltfChannelDto();
                channelDto.sampler = samplerIndex;
                channelDto.targetNode = gltfModel.getNodeModels().indexOf(channel.getNodeModel());
                channelDto.targetPath = channel.getPath();
                animDto.channels.add(channelDto);
            }

            dto.animations.add(animDto);
        }
    }



    private void parseSkins(){
        // TODO: Loop through gltfModel.getSkinModels(), create GltfSkinDto,
        // and add them to dto.skins
    }
}
