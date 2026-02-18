package org.the3deer.android_3d_model_engine.services.collada.entities;

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
    private int[] vertexJointIndices; // This maps an unrolled vertex back to its original vertex index for skinning
    private FloatBuffer weights;

    private List<Mesh> meshes;

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
            }
            meshes.clear();
            return;
        }


        final Mesh mesh1 = meshes.get(0);
        this.materialId = mesh1.getMaterialId();

        // 2nd scenario: countdown.dae
        int totalVertices = 0;
        int totalIndices = 0;
        for (Mesh mesh : meshes) {
            totalVertices += mesh.getVertices().length;
            totalIndices += mesh.getIndices().length;
        }

        this.positions = IOUtils.createFloatBuffer(totalVertices * 3);
        //this.colors = IOUtils.createFloatBuffer(totalVertices * 4);
        //this.texCoords = IOUtils.createFloatBuffer(totalVertices * 2);
        this.normals = IOUtils.createFloatBuffer(totalVertices * 3);
        this.indices = IOUtils.createIntBuffer(totalIndices);

        int offset = 0;
        for (Mesh mesh : meshes){
            this.positions.put(mesh.getVertices());
            /*if (mesh.getColors() != null) {
                this.colors.put(mesh.getColors());
            }
            if (mesh.getTextureCoords() != null) {
                this.texCoords.put(mesh.getTextureCoords());
            }*/
            if (mesh.getNormals() != null) {
                this.normals.put(mesh.getNormals());
            }

            // update indices
            final int[] intIndexes = mesh.getIndices();
            for (int i = 0; i < intIndexes.length; i++) {
                intIndexes[i] += offset;
            }

            // update offset
            offset += intIndexes.length;
        }
    }
}