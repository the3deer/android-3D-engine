package org.the3deer.android.engine.services.fbx.dto;

import org.the3deer.android.engine.model.Scene;

import java.util.List;

public class FBXModel implements AutoCloseable {

    // attributes
    private final String name;
    private String creator;
    private String uri;
    private long nativeHandler;
    private int meshCount;
    private int version;

    // relations
    private List<Scene> scenes;
    private List<FBXMesh> meshes;

    public FBXModel(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public void setScenes(List<Scene> scenes) {
        this.scenes = scenes;
    }

    public String getCreator() {
        return this.creator;
    }

    public long getNativeHandler() {
        return nativeHandler;
    }

    public void setNativeHandler(long nativeHandler) {
        this.nativeHandler = nativeHandler;
    }

    public int getMeshCount() {
        return meshCount;
    }

    public void setMeshCount(int meshCount) {
        this.meshCount = meshCount;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public List<FBXMesh> getMeshes() {
        return meshes;
    }

    public void setMeshes(List<FBXMesh> meshes) {
        this.meshes = meshes;
    }

    @Override
    public void close() {
        if (nativeHandler != 0) {
            fbxFreeModel(nativeHandler);
            nativeHandler = 0;
        }
    }

    private native void fbxFreeModel(long modelPtr);

    public FBXMesh getMesh(int i) {
        return meshes.get(i);
    }
}
