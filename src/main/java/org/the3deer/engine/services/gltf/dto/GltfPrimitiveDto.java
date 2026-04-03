package org.the3deer.engine.services.gltf.dto;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public class GltfPrimitiveDto {
    // Geometry Data
    public Buffer indices;
    public Buffer positions;
    public Buffer normals;
    public Buffer texCoords;

    // Skinning Data
    public Buffer jointIds;
    public FloatBuffer weights;
    public int jointIdsComponents; // The property we wanted to move!
    public int weightsComponents; // The property we wanted to move!

    // Material
    public Buffer colors;
    public Integer materialIndex;

    // Special effects
    public FloatBuffer tangents;
}
