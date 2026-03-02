package org.the3deer.android_3d_model_engine.services.fbx;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.fbx.dto.FBXMesh;
import org.the3deer.android_3d_model_engine.services.fbx.dto.FBXModel;
import org.the3deer.util.android.ContentUtils;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class FbxLoader {

    private static final String TAG = FbxLoader.class.getSimpleName();

    final FBXParser parser;

    public FbxLoader() {
        this.parser = new FBXParser();
    }

    public List<Object3DData> load(URI uri, LoadListener callback) throws Exception {

        final List<Object3DData> ret = new ArrayList<>();

        try (InputStream stream = ContentUtils.getInputStream(uri)) {
            Log.i(TAG, "Parsing FBX model... " + uri);
            callback.onProgress("Parsing FBX model... " + uri);

            FBXModel model = parser.parseModel(stream);
            if (model == null) return ret;

            final int meshCount = model.getMeshCount();
            Log.i(TAG, "FBX Loaded. Mesh Count: " + meshCount);

            for (int i = 0; i < meshCount; i++) {
                final FBXMesh fbxMesh = model.getMesh(i);
                
                // Vertex Buffer (float is 4 bytes)
                ByteBuffer vbb = (ByteBuffer)fbxMesh.getVerticesBuffer();
                if (vbb == null) continue;

                vbb.order(ByteOrder.nativeOrder());
                final FloatBuffer vertexBuffer = vbb.asFloatBuffer();
                
                // Normal Buffer
                FloatBuffer normalsBuffer = null;
                ByteBuffer nbb = (ByteBuffer)fbxMesh.getNormalsBuffer();
                if (nbb != null) {
                    nbb.order(ByteOrder.nativeOrder());
                    normalsBuffer = nbb.asFloatBuffer();
                }

                // Color Buffer
                FloatBuffer colorsBuffer = null;
                ByteBuffer cbb = (ByteBuffer)fbxMesh.getColorsBuffer();
                if (cbb != null) {
                    cbb.order(ByteOrder.nativeOrder());
                    colorsBuffer = cbb.asFloatBuffer();
                }

                // Texture Buffer (UVs)
                FloatBuffer texCoordsBuffer = null;
                ByteBuffer tbb = (ByteBuffer)fbxMesh.getTexCoordsBuffer();
                if (tbb != null) {
                    tbb.order(ByteOrder.nativeOrder());
                    texCoordsBuffer = tbb.asFloatBuffer();
                }

                // Texture Path
                final String texturePath = fbxMesh.getTexturePath();
                Material material = null;
                if (texturePath != null && !texturePath.isEmpty()) {
                    Log.i(TAG, "Texture Path: " + texturePath);
                    material = new Material();
                    material.setColorTexture(new Texture().setFile(texturePath));
                }

                // IMPORTANT: We use the constructor WITHOUT indicesBuffer
                // because our vertices are already "unrolled" in triangle order by ufbx.
                // Using an index buffer from FBX with unrolled vertices causes the "explosion".
                final Object3DData mesh = new Object3DData(vertexBuffer);
                mesh.setId("fbx_mesh_" + i);
                mesh.setNormalsBuffer(normalsBuffer);
                mesh.setColorsBuffer(colorsBuffer);
                mesh.setTextureCoordsArrayBuffer(texCoordsBuffer);
                mesh.setDrawMode(GLES20.GL_TRIANGLES);
                mesh.setIndexed(false);

                // material
                mesh.setMaterial(material);
                
                ret.add(mesh);
            }
        }
        return ret;
    }
}
