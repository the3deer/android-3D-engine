package org.the3deer.android_3d_model_engine.services.fbx;

import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.services.LoadListener;
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
                
                // Vertex Buffer (float is 4 bytes)
                ByteBuffer vbb = (ByteBuffer)model.getMesh(i).getVerticesBuffer();
                vbb.order(ByteOrder.nativeOrder());
                final FloatBuffer vertexBuffer = vbb.asFloatBuffer();
                
                // Normal Buffer
                FloatBuffer normalsBuffer = null;
                ByteBuffer nbb = (ByteBuffer)model.getMesh(i).getNormalsBuffer();
                if (nbb != null) {
                    nbb.order(ByteOrder.nativeOrder());
                    normalsBuffer = nbb.asFloatBuffer();
                }

                // Color Buffer
                FloatBuffer colorsBuffer = null;
                ByteBuffer cbb = (ByteBuffer)model.getMesh(i).getColorsBuffer();
                if (cbb != null) {
                    cbb.order(ByteOrder.nativeOrder());
                    colorsBuffer = cbb.asFloatBuffer();
                }

                // IMPORTANT: We use the constructor WITHOUT indicesBuffer
                // because our vertices are already "unrolled" in triangle order by ufbx.
                // Using an index buffer from FBX with unrolled vertices causes the "explosion".
                final Object3DData mesh = new Object3DData(vertexBuffer);
                mesh.setNormalsBuffer(normalsBuffer);
                mesh.setColorsBuffer(colorsBuffer);
                mesh.setDrawMode(GLES20.GL_TRIANGLES);
                mesh.setIndexed(false);
                
                ret.add(mesh);
            }
        }
        return ret;
    }
}
