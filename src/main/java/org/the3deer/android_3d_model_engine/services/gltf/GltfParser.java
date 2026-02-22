package org.the3deer.android_3d_model_engine.services.gltf;

import android.util.Log;

import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSamplerDto;

import java.util.ArrayList;
import java.util.Collections;
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
import de.javagl.jgltf.model.SkinModel;
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

    public GltfDto parse(){
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

                primitiveDto.indices = GltfUtil.createIndicesBuffer(primitiveModel.getIndices());

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

                AccessorModel texCoordsAccessor = primitiveModel.getAttributes().get("TEXCOORD_0");
                if (texCoordsAccessor != null) {
                    primitiveDto.texCoords = GltfUtil.createFloatBuffer(texCoordsAccessor);
                }

                AccessorModel colorsAccessor = primitiveModel.getAttributes().get("COLOR_0");
                if (colorsAccessor != null){
                    primitiveDto.colors = GltfUtil.createColorsBuffer(colorsAccessor);
                }

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

            float[] transform = new float[16];
            nodeModel.computeLocalTransform(transform);
            nodeDto.matrix = transform;

            if (nodeModel.getChildren() != null) {
                nodeDto.children = new ArrayList<>();
                for (NodeModel childNode : nodeModel.getChildren()) {
                    nodeDto.children.add(gltfModel.getNodeModels().indexOf(childNode));
                }
            }

            if (nodeModel.getMeshModel() != null) {
                nodeDto.meshIndex = gltfModel.getMeshModels().indexOf(nodeModel.getMeshModel());
            }

            if (nodeModel.getSkinModel() != null) {
                nodeDto.skinIndex = gltfModel.getSkinModels().indexOf(nodeModel.getSkinModel());
            }

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

            Map<AnimationModel.Sampler, Integer> samplerMap = new HashMap<>();
            animDto.samplers = new ArrayList<>();
            animDto.channels = new ArrayList<>();

            for (AnimationModel.Channel channel : animModel.getChannels()) {

                AnimationModel.Sampler sampler = channel.getSampler();

                Integer samplerIndex = samplerMap.get(sampler);
                if (samplerIndex == null) {
                    GltfSamplerDto samplerDto = new GltfSamplerDto();
                    samplerDto.interpolation = sampler.getInterpolation();
                    samplerDto.input = GltfUtil.createFloatBuffer(sampler.getInput());
                    samplerDto.output = GltfUtil.createFloatBuffer(sampler.getOutput());

                    animDto.samplers.add(samplerDto);
                    samplerIndex = animDto.samplers.size() - 1;
                    samplerMap.put(sampler, samplerIndex);
                }

                GltfChannelDto channelDto = new GltfChannelDto();
                channelDto.sampler = samplerIndex;
                channelDto.targetNode = gltfModel.getNodeModels().indexOf(channel.getNodeModel());
                channelDto.targetPath = channel.getPath();
                animDto.channels.add(channelDto);
            }

            dto.animations.add(animDto);
        }
    }

    private NodeModel findLowestCommonAncestor(List<NodeModel> nodeModels) {
        if (nodeModels == null || nodeModels.isEmpty()) {
            return null;
        }
        if (nodeModels.size() == 1) {
            return nodeModels.get(0);
        }
        List<NodeModel> pathToRoot = getPathToRoot(nodeModels.get(0));
        if (pathToRoot == null) return null;

        int lowestAncestorIndex = pathToRoot.size() - 1;

        for (int i = 1; i < nodeModels.size(); i++) {
            List<NodeModel> otherPathToRoot = getPathToRoot(nodeModels.get(i));
            if (otherPathToRoot == null) continue;

            int searchIndex = lowestAncestorIndex;
            if (otherPathToRoot.size() - 1 < lowestAncestorIndex){
                searchIndex = otherPathToRoot.size() - 1;
            }

            while(searchIndex > 0 && pathToRoot.get(searchIndex) != otherPathToRoot.get(searchIndex)){
                searchIndex--;
            }
            lowestAncestorIndex = searchIndex;
        }
        return pathToRoot.get(lowestAncestorIndex);
    }

    private List<NodeModel> getPathToRoot(NodeModel node) {
        if (node == null) return null;
        List<NodeModel> path = new ArrayList<>();
        path.add(node);
        NodeModel parent = node.getParent();
        while (parent != null) {
            path.add(parent);
            parent = parent.getParent();
        }
        Collections.reverse(path);
        return path;
    }

    private void parseSkins() {
        List<SkinModel> skinModels = gltfModel.getSkinModels();
        if (skinModels == null || skinModels.isEmpty()) {
            dto.skins = java.util.Collections.emptyList();
            return;
        }

        dto.skins = new ArrayList<>(skinModels.size());
        for (SkinModel skinModel : skinModels) {
            org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSkinDto skinDto = new org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSkinDto();
            skinDto.name = skinModel.getName();

            NodeModel skeletonNodeModel = skinModel.getSkeleton();
            if (skeletonNodeModel == null) {
                Log.v("GltfParser", "skin.skeleton not defined. Computing skeleton root node...");
                skeletonNodeModel = findLowestCommonAncestor(skinModel.getJoints());
            }

            if (skeletonNodeModel != null) {
                skinDto.skeletonRootNodeIndex = gltfModel.getNodeModels().indexOf(skeletonNodeModel);
            }

            if (skinModel.getJoints() != null) {
                skinDto.jointNodeIndices = new ArrayList<>();
                for (NodeModel jointNode : skinModel.getJoints()) {
                    skinDto.jointNodeIndices.add(gltfModel.getNodeModels().indexOf(jointNode));
                }
            }

            if (skinModel.getInverseBindMatrices() != null) {
                skinDto.inverseBindMatrices = GltfUtil.createFloatBuffer(skinModel.getInverseBindMatrices());
            }

            dto.skins.add(skinDto);
        }
    }
}
