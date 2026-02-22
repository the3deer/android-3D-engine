package org.the3deer.android_3d_model_engine.services.gltf.dto;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class GltfSkinDto {
    public List<Integer> jointIndices;
    public Buffer inverseBindMatrices;
    public String name;
    public Integer skeletonRootNodeIndex;
    public ArrayList<Integer> jointNodeIndices;
}
