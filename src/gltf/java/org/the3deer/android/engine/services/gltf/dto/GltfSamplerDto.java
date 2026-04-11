package org.the3deer.android.engine.services.gltf.dto;

import java.nio.FloatBuffer;

import de.javagl.jgltf.model.AnimationModel;

public class GltfSamplerDto {
    public FloatBuffer input;
    public FloatBuffer output;
    public AnimationModel.Interpolation interpolation;
}
