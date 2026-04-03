package org.the3deer.engine.services.fbx;

import android.net.Uri;
import android.opengl.GLES20;

import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.model.Texture;
import org.the3deer.engine.services.LoadListener;
import org.the3deer.engine.services.fbx.dto.FBXMesh;
import org.the3deer.engine.services.fbx.dto.FBXModel;

import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FbxLoader {

    private static final Logger logger = Logger.getLogger(FbxLoader.class.getSimpleName());

    final FBXParser parser;

    public FbxLoader() {
        this.parser = new FBXParser();
    }

    public List<Object3D> load(Uri uri, LoadListener callback) throws Exception {

        final List<Object3D> ret = new ArrayList<>();

        try (InputStream stream = URI.create(uri.toString()).toURL().openStream()) {
            logger.info("Parsing FBX model... " + uri);
            callback.onProgress("Parsing FBX model... " + uri);

            FBXModel model = parser.parseModel(stream);
            if (model == null) return ret;

            final int meshCount = model.getMeshCount();
            logger.info("FBX Loaded. Mesh Count: " + meshCount);

            // build scene
            final Scene scene = new Scene("fbx_scene");

            // build objects
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

                // Texture Path & Embedded Data
                Material material = null;
                final String texturePath = fbxMesh.getTexturePath();
                final byte[] textureEmbeddedData = parser.fbxGetTextureEmbeddedData(model.getNativeHandler(), i);

                if (textureEmbeddedData != null) {
                    logger.info("Embedded Texture found for mesh: " + i);
                    material = new Material();
                    material.setColorTexture(new Texture().setData(textureEmbeddedData));
                } else if (texturePath != null && !texturePath.isEmpty()) {
                    logger.info("External Texture Path: " + texturePath);
                    material = new Material();
                    material.setColorTexture(new Texture().setFile(texturePath));
                }

                // Fallback to Material Color if no texture
                if (material == null) {
                    float[] diffuseColor = parser.fbxGetMaterialColor(model.getNativeHandler(), i);
                    if (diffuseColor != null) {
                        material = new Material();
                        material.setDiffuse(diffuseColor);
                        material.setAlpha(diffuseColor[3]);
                    }
                }

                // IMPORTANT: We use the constructor WITHOUT indicesBuffer
                // because our vertices are already "unrolled" in triangle order by ufbx.
                final Object3D mesh = new Object3D(vertexBuffer);
                mesh.setId("fbx_mesh_" + i);
                mesh.setNormalsBuffer(normalsBuffer);
                mesh.setColorsBuffer(colorsBuffer);
                mesh.setTextureCoordsArrayBuffer(texCoordsBuffer);
                mesh.setDrawMode(GLES20.GL_TRIANGLES);
                mesh.setIndexed(false);
                mesh.setMaterial(material);
                
                ret.add(mesh);

                callback.onLoadObject(scene, mesh);
            }

            // notify
            callback.onLoadScene(scene);

        }
        return ret;
    }
}
