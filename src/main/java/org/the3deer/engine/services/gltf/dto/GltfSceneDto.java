// File: src/main/java/org/the3deer/android_3d_model_engine/services/gltf/dto/GltfSceneDto.java
package org.the3deer.engine.services.gltf.dto;

import java.util.List;

public class GltfSceneDto {
    public String name;
    public List<Integer> nodes; // List of root node indices for this scene
}
