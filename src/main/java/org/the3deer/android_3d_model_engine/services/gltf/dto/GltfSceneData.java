package org.the3deer.android_3d_model_engine.services.gltf.dto;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;

import java.util.List;

// Create a new file or as a public static inner class in GltfLoader
public class GltfSceneData {
    public final GltfDto dto;
    public final List<Node> nodes;
    public final List<Object3DData> meshes;
    public final List<Skin> skins;
    public final List<Animation> animations;
    public final List<Material> materials;

    public GltfSceneData(GltfDto dto, List<Node> nodes, List<Object3DData> meshes, List<Material> materials,
                         List<Skin> skins, List<Animation> animations) {
        this.dto = dto;
        this.nodes = nodes;
        this.meshes = meshes;
        this.materials = materials;
        this.skins = skins;
        this.animations = animations;
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
}
