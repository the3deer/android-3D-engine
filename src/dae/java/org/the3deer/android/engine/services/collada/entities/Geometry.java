package org.the3deer.android.engine.services.collada.entities;

import org.the3deer.util.io.IOUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

// A simple container for the data parsed from a <geometry> tag in a DAE file.
public class Geometry {
    private final String id;
    private FloatBuffer positions;
    private FloatBuffer colors;
    private FloatBuffer normals;
    private FloatBuffer texCoords;
    private IntBuffer indices;
    private String materialId;

    // This is the list of meshes that will be merged into this geometry.
    // In some cases, a <geometry> can contain multiple <mesh> elements,
    // and we need to merge them together, because skinning is done at the geometry level, not the mesh level.
    private List<Mesh> meshes;

    // This maps an unrolled vertex back to its original vertex index for skinning
    private int[] indicesMap;

    public Geometry(String id) {
        this.id = id;
        this.meshes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public FloatBuffer getPositions() {
        return positions;
    }

    public void setPositions(FloatBuffer positions) {
        this.positions = positions;
    }

    public FloatBuffer getColors() {
        return colors;
    }

    public void setColors(FloatBuffer colors) {
        this.colors = colors;
    }

    public FloatBuffer getNormals() {
        return normals;
    }

    public void setNormals(FloatBuffer normals) {
        this.normals = normals;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }

    public FloatBuffer getTexCoords() {
        return texCoords;
    }

    public void setTextureCoords(FloatBuffer texCoords) {
        this.texCoords = texCoords;
    }

    public IntBuffer getIndices() {
        return indices;
    }

    public int[] getIndicesMap() {
        return indicesMap;
    }

    public void addMesh(Mesh mesh) {
        this.meshes.add(mesh);
    }

    public List<Mesh> getMeshes() {
        return meshes;
    }

    //
    public void assemble() {

        if (meshes.isEmpty()) return;

        // 1st scenario: default case
        if (meshes.size() == 1){
            final Mesh mesh1 = meshes.get(0);
            this.materialId = mesh1.getMaterialId();
            this.positions = IOUtils.createFloatBuffer(mesh1.getVertices());
            if(mesh1.getColors() != null) {
                this.colors = IOUtils.createFloatBuffer(mesh1.getColors());
            }
            if (mesh1.getTextureCoords() != null) {
                this.texCoords = IOUtils.createFloatBuffer(mesh1.getTextureCoords());
            }
            if (mesh1.getNormals() != null) {
                this.normals = IOUtils.createFloatBuffer(mesh1.getNormals());
            }
            if (mesh1.getIndices() != null) {
                this.indices = IOUtils.createIntBuffer(mesh1.getIndices());
                this.indicesMap = mesh1.getIndicesMap();
            }

            // Reset positions
            if (this.positions != null) this.positions.position(0);
            if (this.colors != null) this.colors.position(0);
            if (this.texCoords != null) this.texCoords.position(0);
            if (this.normals != null) this.normals.position(0);
            if (this.indices != null) this.indices.position(0);

            meshes.clear();
            return;
        }


        final Mesh mesh1 = meshes.get(0);
        this.materialId = mesh1.getMaterialId();

        // 2nd scenario: countdown.dae
        int totalVertices = 0;
        int totalIndices = 0;
        int totalColors = 0;
        int totalTexCoords = 0;
        for (Mesh mesh : meshes) {
            totalVertices += mesh.getVertices().length;
            totalIndices += mesh.getIndices().length;
            if (mesh.getColors() != null) totalColors += mesh.getColors().length;
            if (mesh.getTextureCoords() != null) totalTexCoords += mesh.getTextureCoords().length;
        }

        this.positions = IOUtils.createFloatBuffer(totalVertices);
        if (totalColors > 0) this.colors = IOUtils.createFloatBuffer(totalColors);
        if (totalTexCoords > 0) this.texCoords = IOUtils.createFloatBuffer(totalTexCoords);
        this.normals = IOUtils.createFloatBuffer(totalVertices);
        this.indices = IOUtils.createIntBuffer(totalIndices);
        this.indicesMap = new int[totalIndices];

        int offsetVertices = 0;
        int offsetIndices = 0;
        for (Mesh mesh : meshes){
            this.positions.put(mesh.getVertices());
            if (mesh.getColors() != null) {
                this.colors.put(mesh.getColors());
            }
            if (mesh.getTextureCoords() != null) {
                this.texCoords.put(mesh.getTextureCoords());
            }
            if (mesh.getNormals() != null) {
                this.normals.put(mesh.getNormals());
            }

            // update the indices Map - no need to offset it,
            // because the map is used to map unrolled vertices back to their original vertex index for skinning
            final int[] intIndicesMap = mesh.getIndicesMap();
            for (int i = 0; i < intIndicesMap.length; i++) {
                this.indicesMap[offsetIndices + i] = intIndicesMap[i];
            }

            // offset unrolled indices
            final int[] intIndexes = mesh.getIndices();
            for (int i = 0; i < intIndexes.length; i++) {
                intIndexes[i] += (offsetVertices / 3);
            }
            this.indices.put(intIndexes);

            // update offsets
            offsetVertices += mesh.getVertices().length;
            offsetIndices += mesh.getIndices().length;
        }

        // Finalize buffers
        this.positions.position(0);
        if (this.colors != null) this.colors.position(0);
        if (this.texCoords != null) this.texCoords.position(0);
        this.normals.position(0);
        this.indices.position(0);
    }

}