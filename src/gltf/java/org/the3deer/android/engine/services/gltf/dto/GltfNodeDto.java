package org.the3deer.android.engine.services.gltf.dto;

import java.util.List;

public class GltfNodeDto {
    public String name;
    public float[] matrix;
    public List<Integer> children; // List of child node indices
    public Integer meshIndex;
    public Integer skinIndex;
    public Integer cameraIndex;
    public float[] scale;
    public float[] rotation;
    public float[] translation;
}
