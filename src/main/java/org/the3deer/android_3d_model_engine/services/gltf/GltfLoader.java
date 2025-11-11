package org.the3deer.android_3d_model_engine.services.gltf;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.model.Transform;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMaterialDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfMeshDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfNodeDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfPrimitiveDto;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneData;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSkinDto;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.math.Quaternion;

import java.io.InputStream;
import java.net.URI;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.GltfModels;
import de.javagl.jgltf.model.io.Buffers;
import de.javagl.jgltf.model.io.GltfAsset;
import de.javagl.jgltf.model.io.GltfAssetReader;
import de.javagl.jgltf.model.io.GltfReferenceResolver;
import de.javagl.jgltf.model.io.IO;

public class GltfLoader {

    private static final String TAG = GltfLoader.class.getSimpleName();

    public GltfLoader() {
    }

    // In GltfLoader.java - replace the whole load() method

    public GltfSceneData load(URI uri, LoadListener callback) throws Exception {

        try (InputStream stream = ContentUtils.getInputStream(uri)) {
            Log.i(TAG, "Loading and parsing model file... " + uri);
            callback.onProgress("Parsing " + uri);

            // --- Stage 1: Parse to DTO ---
            GltfAssetReader gltfAssetReader = new GltfAssetReader();
            GltfAsset gltfAsset = gltfAssetReader.readWithoutReferences(stream);
            URI baseUri = IO.getParent(uri);
            GltfReferenceResolver.resolveAll(gltfAsset.getReferences(), baseUri);
            GltfModel gltfModel = GltfModels.create(gltfAsset);
            GltfParser parser = new GltfParser(gltfAsset, gltfModel);
            GltfDto dto = parser.parse();

            // --- Stage 2: Build Engine Objects from DTO ---
            Log.i(TAG, "Building engine objects from DTO...");
            callback.onProgress("Building objects");

            // A. Find which node uses which skin (for dependency lookup)
            Map<Integer, GltfNodeDto> skinToNodeMap = new HashMap<>();
            for (GltfNodeDto nodeDto : dto.nodes) {
                if (nodeDto.skinIndex != null) {
                    skinToNodeMap.put(nodeDto.skinIndex, nodeDto);
                }
            }

            // B. Load Meshes and create a map for the skin builder
            List<Material> materials = buildMaterialsFromDto(dto);
            Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap = new HashMap<>();
            List<Object3DData> meshes = buildMeshesFromDto(dto, materials, meshToPrimitiveMap);

            // C. Load Skins, which depend on the mesh primitive data
            List<Skin> skins = buildSkinsFromDto(dto, skinToNodeMap, meshToPrimitiveMap);

            // D. Load Nodes, which links everything together
            List<Node> nodes = buildNodesFromDto(dto, meshes, skins);

            // E. Load animations
            GltfAnimationLoader animationLoader = new GltfAnimationLoader(dto, nodes);
            List<Animation> animations = animationLoader.load();

            // --- Stage 3: Return all the built parts ---
            return new GltfSceneData(dto, nodes, meshes, materials, skins, animations);
        }
    }

    private List<Material> buildMaterialsFromDto(GltfDto dto) {
        if (dto.materials == null || dto.materials.isEmpty()) {
            return Collections.emptyList();
        }

        List<Material> materials = new ArrayList<>(dto.materials.size());
        for (int i = 0; i < dto.materials.size(); i++) {
            GltfMaterialDto materialDto = dto.materials.get(i);
            Material material = new Material();
            material.setName(materialDto.name);
            material.setDiffuse(materialDto.baseColorFactor);

            // diffuse texture
            if (materialDto.imageData != null) {
                material.setColorTexture(new Texture().setBuffer(materialDto.imageData));
            }

            materials.add(material);
        }

        return materials;
    }

    // Modify buildNodesFromDto to accept the skins list and link them
    private List<Node> buildNodesFromDto(GltfDto dto, List<Object3DData> meshes, List<Skin> skins) {
        if (dto.nodes == null || dto.nodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. First, create all Node objects
        List<Node> nodes = new ArrayList<>(dto.nodes.size());
        for (int i = 0; i < dto.nodes.size(); i++) {
            Node node = new Node(i);
            nodes.add(node);
        }

        // 2. Now, iterate again to set properties and build the hierarchy.
        for (int i = 0; i < dto.nodes.size(); i++) {
            GltfNodeDto nodeDto = dto.nodes.get(i);
            Node node = nodes.get(i);

            node.setName(nodeDto.name);

            // Set local transform... (this part is correct)
            if (nodeDto.matrix != null) {
                node.setMatrix(nodeDto.matrix);
            } else {
                node.setLocalTransform(new Transform(floatArrayToFloatWrapperArray(nodeDto.scale),
                        nodeDto.rotation != null ? new Quaternion(nodeDto.rotation) : null,
                        floatArrayToFloatWrapperArray(nodeDto.translation)
                ));
            }

            // Link children... (this part is correct)
            if (nodeDto.children != null) {
                for (Integer childIndex : nodeDto.children) {
                    Node childNode = nodes.get(childIndex);
                    node.addChild(childNode);
                    childNode.setParent(node);
                }
            }

            // Link mesh and the fully populated skin
            if (nodeDto.meshIndex != null) {
                Object3DData mesh = meshes.get(nodeDto.meshIndex);
                node.setMesh(mesh);
                mesh.setParentNode(node);

                // --- THIS IS THE FINAL LINK ---
                // If this node uses a skin, link the fully populated skin object to the mesh
                if (nodeDto.skinIndex != null && mesh instanceof AnimatedModel) {
                    ((AnimatedModel) mesh).setSkin(skins.get(nodeDto.skinIndex));
                }
            }
        }
        return nodes;
    }

    // You will also need the helper method in Node.java or in a utility class
    private static Float[] floatArrayToFloatWrapperArray(float[] primitiveArray) {
        if (primitiveArray == null) return null;
        Float[] wrapperArray = new Float[primitiveArray.length];
        for (int i = 0; i < primitiveArray.length; i++) {
            wrapperArray[i] = primitiveArray[i];
        }
        return wrapperArray;
    }

    // In GltfLoader.java
// Replace your existing buildMeshesFromDto with this one.

    private List<Object3DData> buildMeshesFromDto(GltfDto dto, List<Material> materials,
                                                  Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap) {
        if (dto.meshes == null || dto.meshes.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object3DData> allPrimitives = new ArrayList<>();
        for (int i = 0; i < dto.meshes.size(); i++) {
            GltfMeshDto meshDto = dto.meshes.get(i);
            if (meshDto.primitives == null) continue;

            // NOTE: For now, we assume one primitive per mesh for simplicity.
            // GLTF supports multiple primitives, but our engine structure links one mesh to one node.
            if (!meshDto.primitives.isEmpty()){
                GltfPrimitiveDto primitiveDto = meshDto.primitives.get(0);

                // Each primitive becomes one drawable Object3DData.
                AnimatedModel model = new AnimatedModel();
                model.setId(meshDto.name != null ? meshDto.name + "_" + i : "mesh_" + i);
                model.setVertexArrayBuffer((FloatBuffer) primitiveDto.positions);
                model.setVertexNormalsArrayBuffer((FloatBuffer) primitiveDto.normals);
                model.setTangentBuffer(primitiveDto.tangents);
                model.setTextureCoordsArrayBuffer((FloatBuffer) primitiveDto.texCoords);
                model.setVertexColorsArrayBuffer(primitiveDto.colors);
                model.setIndexBuffer(primitiveDto.indices);
                model.setDrawUsingArrays(primitiveDto.indices == null);
                model.setDrawMode(GLES20.GL_TRIANGLES);

                if (primitiveDto.materialIndex != null) {
                    model.setMaterial(materials.get(primitiveDto.materialIndex));
                }

                allPrimitives.add(model);

                // Crucially, map the mesh index to its primitive DTO for the skin builder to use.
                meshToPrimitiveMap.put(i, primitiveDto);
            }
        }
        return allPrimitives;
    }



    // In GltfLoader.java
// Replace your existing buildSkinsFromDto with this one.

    private List<Skin> buildSkinsFromDto(GltfDto dto, Map<Integer, GltfNodeDto> skinToNodeMap,
                                         Map<Integer, GltfPrimitiveDto> meshToPrimitiveMap) {
        if (dto.skins == null || dto.skins.isEmpty()) {
            return Collections.emptyList();
        }

        List<Skin> skins = new ArrayList<>();
        for (int i = 0; i < dto.skins.size(); i++) {
            GltfSkinDto skinDto = dto.skins.get(i);
            GltfNodeDto skinnedNodeDto = skinToNodeMap.get(i);

            if (skinnedNodeDto == null || skinnedNodeDto.meshIndex == null) {
                Log.w(TAG, "Skin " + i + " is not linked to a node with a mesh. Skipping.");
                skins.add(new Skin()); // Add a placeholder to keep indices correct
                continue;
            }

            // Find the primitive that contains the geometry for this skin
            GltfPrimitiveDto primitiveDto = meshToPrimitiveMap.get(skinnedNodeDto.meshIndex);
            if (primitiveDto == null) {
                Log.w(TAG, "Could not find mesh primitive for skin " + i + ". Skipping.");
                skins.add(new Skin()); // Add a placeholder
                continue;
            }

            // Convert the inverse bind matrix buffer to a flat, transposed array
            float[] ibmArray = new float[skinDto.inverseBindMatrices.remaining()];
            ((FloatBuffer) skinDto.inverseBindMatrices).get(ibmArray);
            float[] ibmArrayTransposed = new float[ibmArray.length];
            int matrixCount = ibmArray.length / 16;
            for (int m = 0; m < matrixCount; m++) {
                Matrix.transposeM(ibmArrayTransposed, m * 16, ibmArray, m * 16);
            }

            // Now, construct the Skin object with ALL its data
            Skin skin = new Skin(
                    null, // bindShapeMatrix is not typically used in GLTF this way
                    primitiveDto.jointIds,
                    primitiveDto.weights,
                    ibmArrayTransposed,
                    null // jointNames will be populated later if needed
            );
            skin.setJointComponents(primitiveDto.jointIdsComponents);
            skin.setWeightsComponents(primitiveDto.weightsComponents);
            skins.add(skin);
        }
        return skins;
    }

}
