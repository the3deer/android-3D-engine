package org.the3deer.android_3d_model_engine.services.gltf.dto;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;

import java.util.List;

public class GltfSceneData {
    public final GltfDto dto;
    public final List<Node> nodes;
    public final List<Object3DData> meshes;
    public final List<Skin> skins;
    public final List<Animation> animations;
    public final List<Material> materials;
    public final List<Camera> cameras;

    public GltfSceneData(GltfDto dto, List<Node> nodes, List<Object3DData> meshes, List<Material> materials,
                         List<Skin> skins, List<Animation> animations, List<Camera> cameras) {
        this.dto = dto;
        this.nodes = nodes;
        this.meshes = meshes;
        this.materials = materials;
        this.skins = skins;
        this.animations = animations;
        this.cameras = cameras;
    }

    public GltfDto getDto() {
        return dto;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Object3DData> getMeshes() {
        return meshes;
    }

    public List<Skin> getSkins() {
        return skins;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public List<Material> getMaterials() {
        return materials;
    }

    public List<Camera> getCameras() {
        return cameras;
    }
}
