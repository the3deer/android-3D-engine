package org.the3deer.android.engine.services.gltf;

import org.the3deer.android.engine.services.gltf.dto.GltfAnimationDto;
import org.the3deer.android.engine.services.gltf.dto.GltfCameraDto;
import org.the3deer.android.engine.services.gltf.dto.GltfChannelDto;
import org.the3deer.android.engine.services.gltf.dto.GltfDto;
import org.the3deer.android.engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android.engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android.engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android.engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSamplerDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSceneDto;
import org.the3deer.android.engine.services.gltf.dto.GltfSkinDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.AnimationModel;
import de.javagl.jgltf.model.CameraModel;
import de.javagl.jgltf.model.CameraOrthographicModel;
import de.javagl.jgltf.model.CameraPerspectiveModel;
import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SceneModel;
import de.javagl.jgltf.model.SkinModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.v1.MaterialModelV1;
import de.javagl.jgltf.model.v2.MaterialModelV2;

public class GltfParser {

    private static final Logger logger = Logger.getLogger(GltfParser.class.getSimpleName());

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
        parseCameras();
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
                    primitiveDto.weights = GltfUtil.createNormalizedWeightsBuffer(weightsAccessor);
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

                try {
                    // Base color and texture
                    materialDto.baseColorFactor = materialModelV2.getBaseColorFactor();
                    if (materialModelV2.getBaseColorTexture() != null && materialModelV2.getBaseColorTexture().getImageModel() != null) {
                        materialDto.baseColorTexture = materialModelV2.getBaseColorTexture().getImageModel().getImageData();
                    }

                    // Alpha settings
                    materialDto.alphaCutoff = materialModelV2.getAlphaCutoff();
                    if (materialModelV2.getAlphaMode() != null) {
                        materialDto.alphaMode = materialModelV2.getAlphaMode().name();
                    }

                    // Normal map
                    if (materialModelV2.getNormalTexture() != null && materialModelV2.getNormalTexture().getImageModel() != null) {
                        materialDto.normalTexture = materialModelV2.getNormalTexture().getImageModel().getImageData();
                    }

                    // Emissive map and factor
                    materialDto.emissiveFactor = materialModelV2.getEmissiveFactor();
                    if (materialModelV2.getEmissiveTexture() != null && materialModelV2.getEmissiveTexture().getImageModel() != null) {
                        materialDto.emissiveTexture = materialModelV2.getEmissiveTexture().getImageModel().getImageData();
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to parse v2 material: " + e.getMessage(), e);
                }

                // KHR_materials_volume extension
                try {
                    final Map<String, Object> extensions = materialModelV2.getExtensions();
                    if (extensions != null) {
                        final Map<String, Object> volumeExtension = (Map<String, Object>) extensions.get("KHR_materials_volume");
                        if (volumeExtension != null) {
                            final Map<String, Object> thicknessTextureMap = (Map<String, Object>) volumeExtension.get("thicknessTexture");
                            if (thicknessTextureMap != null) {
                                final Integer texIndex = (Integer) thicknessTextureMap.get("index");
                                final TextureModel textureModel = gltfModel.getTextureModels().get(texIndex);
                                materialDto.thicknessTexture = textureModel.getImageModel().getImageData();
                            }

                            Double thicknessFactor = (Double) volumeExtension.get("thicknessFactor");
                            if (thicknessFactor != null) materialDto.thicknessFactor = thicknessFactor.floatValue();

                            Double attenuationDistance = (Double) volumeExtension.get("attenuationDistance");
                            if (attenuationDistance != null) materialDto.attenuationDistance = attenuationDistance.floatValue();

                            List<Double> attenuationColorList = (List<Double>) volumeExtension.get("attenuationColor");
                            if (attenuationColorList != null && attenuationColorList.size() >= 3) {
                                materialDto.attenuationColor = new float[]{
                                        attenuationColorList.get(0).floatValue(),
                                        attenuationColorList.get(1).floatValue(),
                                        attenuationColorList.get(2).floatValue()
                                };
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed to parse KHR_materials_volume extension: " + e.getMessage(), e);
                }

            } else if (materialModel instanceof MaterialModelV1){
                MaterialModelV1 materialModelV1 = (MaterialModelV1) materialModel;
                // Legacy v1 material handling would go here if needed
            }

            dto.materials.add(materialDto);
        }
    }

    private void parseCameras() {
        List<CameraModel> cameraModels = gltfModel.getCameraModels();
        if (cameraModels == null) return;
        
        dto.cameras = new ArrayList<>(cameraModels.size());
        for (CameraModel cameraModel : cameraModels) {
            GltfCameraDto cameraDto = new GltfCameraDto();
            cameraDto.name = cameraModel.getName();
            
            CameraPerspectiveModel perspective = cameraModel.getCameraPerspectiveModel();
            if (perspective != null) {
                cameraDto.type = "perspective";
                cameraDto.aspectRatio = perspective.getAspectRatio();
                cameraDto.yfov = perspective.getYfov();
                cameraDto.zfar = perspective.getZfar();
                cameraDto.znear = perspective.getZnear();
            }
            
            CameraOrthographicModel orthographic = cameraModel.getCameraOrthographicModel();
            if (orthographic != null) {
                cameraDto.type = "orthographic";
                cameraDto.xmag = orthographic.getXmag();
                cameraDto.ymag = orthographic.getYmag();
                cameraDto.zfar = orthographic.getZfar();
                cameraDto.znear = orthographic.getZnear();
            }
            
            dto.cameras.add(cameraDto);
        }
    }


    private void parseNodes() {
        List<NodeModel> nodeModels = gltfModel.getNodeModels();
        dto.nodes = new ArrayList<>(nodeModels.size());

        for (NodeModel nodeModel : nodeModels) {
            GltfNodeDto nodeDto = new GltfNodeDto();
            nodeDto.name = nodeModel.getName();

            // transform
            nodeDto.translation = nodeModel.getTranslation();
            nodeDto.rotation = nodeModel.getRotation();
            nodeDto.scale = nodeModel.getScale();
            nodeDto.matrix = nodeModel.getMatrix();

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
            dto.scenes = Collections.emptyList();
            return;
        }

        dto.scenes = new ArrayList<>(sceneModels.size());
        for (SceneModel sceneModel : sceneModels) {
            GltfSceneDto sceneDto = new GltfSceneDto();
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
            dto.animations = Collections.emptyList();
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
                channelDto.samplerIndex = samplerIndex;
                channelDto.targetNodeIndex = gltfModel.getNodeModels().indexOf(channel.getNodeModel());
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
            dto.skins = Collections.emptyList();
            return;
        }

        dto.skins = new ArrayList<>(skinModels.size());
        for (SkinModel skinModel : skinModels) {
            GltfSkinDto skinDto = new GltfSkinDto();
            skinDto.name = skinModel.getName();

            NodeModel skeletonNodeModel = skinModel.getSkeleton();
            if (skeletonNodeModel == null) {
                logger.finest("skin.skeleton not defined. Computing skeleton root node...");
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
