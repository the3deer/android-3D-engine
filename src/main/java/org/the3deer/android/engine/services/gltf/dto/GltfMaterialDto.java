package org.the3deer.android.engine.services.gltf.dto;

import java.nio.ByteBuffer;

public class GltfMaterialDto {
    public String name;
    public float[] baseColorFactor;
    public ByteBuffer imageData;
    public ByteBuffer baseColorTexture;
    public float alphaCutoff;
    public String alphaMode;
    public ByteBuffer normalTexture;
    public float[] emissiveFactor;
    public ByteBuffer emissiveTexture;
    public ByteBuffer thicknessTexture;
    public float thicknessFactor;
    public float attenuationDistance;
    public float[] attenuationColor;
}
