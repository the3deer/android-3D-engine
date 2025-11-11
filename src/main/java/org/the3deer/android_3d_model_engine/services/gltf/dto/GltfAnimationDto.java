package org.the3deer.android_3d_model_engine.services.gltf.dto;

import java.util.List;

import de.javagl.jgltf.model.AccessorByteData;

public class GltfAnimationDto {
    public List<GltfChannelDto> channels;
    public List<GltfSamplerDto> samplers;
    public String name;
}
