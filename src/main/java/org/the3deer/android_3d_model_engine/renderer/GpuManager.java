package org.the3deer.android_3d_model_engine.renderer;

import android.opengl.GLES30;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manager responsible for the lifecycle of GPU assets.
 * It caches GpuAssets to prevent redundant buffer uploads and VAO creation.
 * 
 * @author Gemini AI
 */
public class GpuManager {

    private static final String TAG = "GpuManager";

    // Attribute Locations (matching #version 300 es layouts)
    public static final int ATTR_POSITION = 0;
    public static final int ATTR_NORMAL = 1;
    public static final int ATTR_TEXCOORD = 2;
    public static final int ATTR_COLOR = 3;
    public static final int ATTR_JOINT_INDICES = 4;
    public static final int ATTR_JOINT_WEIGHTS = 5;

    // Cache to map Object3DData to its GPU representation
    private final Map<Object3DData, GpuAsset> assetMap = new HashMap<>();

    /**
     * Returns a GpuAsset for the given model. 
     * If the asset doesn't exist, it creates and uploads it.
     */
    public GpuAsset getAsset(Object3DData obj) {
        GpuAsset asset = assetMap.get(obj);
        if (asset == null) {
            asset = createAsset(obj);
            assetMap.put(obj, asset);
        }
        return asset;
    }

    private GpuAsset createAsset(Object3DData obj) {
        Log.d(TAG, "Creating GPU Asset for model: " + obj.getId());

        // 1. Create and bind a VAO
        int[] vaoIds = new int[1];
        GLES30.glGenVertexArrays(1, vaoIds, 0);
        GLES30.glBindVertexArray(vaoIds[0]);

        // 2. Create VBOs for attributes
        int[] vboIds = new int[6];
        GLES30.glGenBuffers(6, vboIds, 0);

        // Attribute 0: Position (mandatory)
        uploadAttribute(ATTR_POSITION, vboIds[ATTR_POSITION], obj.getVertexBuffer(), 3);

        // Attribute 1: Normal
        if (obj.getNormalsBuffer() != null) {
            uploadAttribute(ATTR_NORMAL, vboIds[ATTR_NORMAL], obj.getNormalsBuffer(), 3);
        }

        // Attribute 2: Texture Coordinates (UV)
        if (obj.getTextureCoordsArrayBuffer() != null) {
            uploadAttribute(ATTR_TEXCOORD, vboIds[ATTR_TEXCOORD], obj.getTextureCoordsArrayBuffer(), 2);
        }

        // Attribute 3: Colors
        if (obj.getColorsBuffer() != null) {
            uploadAttribute(ATTR_COLOR, vboIds[ATTR_COLOR], (FloatBuffer) obj.getColorsBuffer(), 4);
        }

        // Animation Attributes
        if (obj instanceof AnimatedModel) {
            Skin skin = ((AnimatedModel) obj).getSkin();
            if (skin != null) {
                if (skin.getJointsBuffer() != null) {
                    uploadIntAttribute(ATTR_JOINT_INDICES, vboIds[ATTR_JOINT_INDICES], skin.getJointsBuffer(), skin.getJointComponents());
                }
                if (skin.getWeightsBuffer() != null) {
                    uploadAttribute(ATTR_JOINT_WEIGHTS, vboIds[ATTR_JOINT_WEIGHTS], skin.getWeightsBuffer(), skin.getWeightsComponents());
                }
            }
        }

        // 3. Create GPU Elements (EBOs)
        List<GpuAsset.GpuElement> gpuElements = new ArrayList<>();
        if (obj.isIndexed()) {
            List<Element> elements = obj.getElements();
            if (elements != null && !elements.isEmpty()) {
                // Multi-element support
                for (Element element : elements) {
                    gpuElements.add(uploadIndexBuffer(element.getIndexBuffer()));
                }
            } else if (obj.getIndexBuffer() != null) {
                // Single index buffer fallback
                gpuElements.add(uploadIndexBuffer(obj.getIndexBuffer()));
            }
        }

        // Unbind VAO
        GLES30.glBindVertexArray(0);

        return new GpuAsset(vaoIds[0], vboIds, gpuElements, 
                obj.getVertexBuffer().capacity() / 3, 
                obj.getDrawMode());
    }

    private GpuAsset.GpuElement uploadIndexBuffer(Buffer indexBuffer) {
        if (indexBuffer == null) return null;
        
        int[] eboIds = new int[1];
        GLES30.glGenBuffers(1, eboIds, 0);
        int eboId = eboIds[0];
        
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, eboId);
        indexBuffer.position(0);
        int count = indexBuffer.capacity();
        int type;
        
        if (indexBuffer instanceof IntBuffer) {
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, count * 4, indexBuffer, GLES30.GL_STATIC_DRAW);
            type = GLES30.GL_UNSIGNED_INT;
        } else if (indexBuffer instanceof ShortBuffer) {
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, count * 2, indexBuffer, GLES30.GL_STATIC_DRAW);
            type = GLES30.GL_UNSIGNED_SHORT;
        } else {
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, count, indexBuffer, GLES30.GL_STATIC_DRAW);
            type = GLES30.GL_UNSIGNED_BYTE;
        }
        
        return new GpuAsset.GpuElement(eboId, count, type);
    }

    private void uploadAttribute(int location, int vboId, FloatBuffer buffer, int size) {
        if (buffer == null) return;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        buffer.position(0);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(location);
        GLES30.glVertexAttribPointer(location, size, GLES30.GL_FLOAT, false, 0, 0);
    }

    private void uploadIntAttribute(int location, int vboId, Buffer buffer, int size) {
        if (buffer == null) return;
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId);
        buffer.position(0);
        int elementSize = (buffer instanceof IntBuffer) ? 4 : (buffer instanceof ShortBuffer) ? 2 : 1;
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, buffer.capacity() * elementSize, buffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(location);
        
        int type = (buffer instanceof IntBuffer) ? GLES30.GL_UNSIGNED_INT : 
                   (buffer instanceof ShortBuffer) ? GLES30.GL_UNSIGNED_SHORT : GLES30.GL_UNSIGNED_BYTE;
        
        GLES30.glVertexAttribIPointer(location, size, type, 0, 0);
    }

    public void removeAsset(Object3DData obj) {
        GpuAsset asset = assetMap.remove(obj);
        if (asset != null) {
            asset.dispose();
        }
    }

    public void clear() {
        for (GpuAsset asset : assetMap.values()) {
            asset.dispose();
        }
        assetMap.clear();
    }
}
