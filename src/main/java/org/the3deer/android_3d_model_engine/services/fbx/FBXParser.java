package org.the3deer.android_3d_model_engine.services.fbx;

import android.util.Log;

import org.the3deer.android_3d_model_engine.services.fbx.dto.FBXMesh;
import org.the3deer.android_3d_model_engine.services.fbx.dto.FBXModel;

import java.io.InputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class FBXParser {

    private static final String TAG = FBXParser.class.getSimpleName();

    static {
        System.loadLibrary("the3deer_engine");
    }

    // FBX C Native Interface
    public native long fbxParseModel(String filePath);
    public native long fbxParseModelFromStream(InputStream is);
    public native int fbxGetVersion(long modelPtr);
    public native int fbxGetMeshCount(long modelPtr);
    public native Object fbxGetModelAttribute(long modelPtr, String name);
    public native Buffer fbxGetVertexBuffer(long modelPtr, int primitiveIndex);
    public native Buffer fbxGetNormalsBuffer(long modelPtr, int primitiveIndex);
    public native Buffer fbxGetColorsBuffer(long modelPtr, int primitiveIndex);
    public native Buffer fbxGetTexCoordsBuffer(long modelPtr, int primitiveIndex, boolean flipY);
    public native Buffer fbxGetTangentsBuffer(long modelPtr, int primitiveIndex);
    public native Buffer fbxGetIndexBuffer(long modelPtr, int primitiveIndex);
    public native String fbxGetTexturePath(long modelPtr, int primitiveIndex);
    public native byte[] fbxGetTextureEmbeddedData(long modelPtr, int primitiveIndex);
    public native float[] fbxGetMaterialColor(long modelPtr, int primitiveIndex);

    // FBX Application Interface
    public FBXModel parseModel(String filePath){
        long handler = fbxParseModel(filePath);
        return assembleModel(handler, filePath);
    }
    public FBXModel parseModel(InputStream is) {
        long handler = fbxParseModelFromStream(is);
        return assembleModel(handler, "StreamSource");
    }
    private FBXModel assembleModel(long handler, String name) {
        if (handler == 0) return null;

        final FBXModel model = new FBXModel(name);
        model.setNativeHandler(handler);

        final String creator = (String)fbxGetModelAttribute(handler, "metadata.creator");
        model.setCreator(creator);
        final int version = fbxGetVersion(handler);
        model.setVersion(version);

        final int meshCount = fbxGetMeshCount(handler);
        model.setMeshCount(meshCount);

        final List<FBXMesh> meshes = new ArrayList<>();
        for (int i = 0; i < meshCount; i++) {
            final FBXMesh mesh = new FBXMesh();
            
            mesh.setVerticesBuffer(fbxGetVertexBuffer(handler, i));
            mesh.setIndicesBuffer(fbxGetIndexBuffer(handler, i));
            mesh.setNormalsBuffer(fbxGetNormalsBuffer(handler, i));
            mesh.setColorsBuffer(fbxGetColorsBuffer(handler, i));
            // Default to flipY = true as it's the common case for your app
            mesh.setTexCoordsBuffer(fbxGetTexCoordsBuffer(handler, i, true));
            mesh.setTexturePath(fbxGetTexturePath(handler, i));
            mesh.setTextureEmbeddedData(fbxGetTextureEmbeddedData(handler, i));
            
            meshes.add(mesh);
        }
        model.setMeshes(meshes);

        return model;
    }
}
