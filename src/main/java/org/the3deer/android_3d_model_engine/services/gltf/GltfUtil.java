package org.the3deer.android_3d_model_engine.services.gltf;

import android.util.Log; // It's good practice to use Android's Log

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import de.javagl.jgltf.model.AccessorData;
import de.javagl.jgltf.model.AccessorModel;

public final class GltfUtil {

    /**
     * Creates a new NIO Buffer for vertex indices from the given glTF AccessorModel.
     * This method correctly handles byte, short, and int component types using GL constants.
     */
    public static Buffer createIndicesBuffer(AccessorModel indicesAccessor) {
        if (indicesAccessor == null) return null;

        AccessorData accessorData = indicesAccessor.getAccessorData();
        // Use the GL integer constant for robust checking
        final int glComponentType = accessorData.getGlComponentType();
        ByteBuffer byteBuffer = accessorData.createByteBuffer();

        switch (glComponentType) {
            case 5121: // GL_UNSIGNED_BYTE
                return byteBuffer;
            case 5123: // GL_UNSIGNED_SHORT
                return byteBuffer.asShortBuffer();
            case 5125: // GL_UNSIGNED_INT
                return byteBuffer.asIntBuffer();
            default:
                Log.e("GltfUtil", "Invalid component type for indices: " + glComponentType);
                throw new IllegalArgumentException("Invalid component type for indices: " + glComponentType);
        }
    }

    /**
     * Creates a FloatBuffer for vertex positions, normals, texture coordinates, or weights.
     */
    static FloatBuffer createFloatBuffer(AccessorModel accessor) {
        if (accessor == null) return null;
        // This is simple because we assume these are always floats
        return accessor.getAccessorData().createByteBuffer().asFloatBuffer();
    }

    public static FloatBuffer createVerticesBuffer(AccessorModel positionsAccessor) {
        return createFloatBuffer(positionsAccessor);
    }

    public static FloatBuffer createNormalsBuffer(AccessorModel normalsAccessor) {
        return createFloatBuffer(normalsAccessor);
    }

    public static FloatBuffer createTexCoordsBuffer(AccessorModel texCoordsAccessor) {
        return createFloatBuffer(texCoordsAccessor);
    }

    public static FloatBuffer createWeightsBuffer(AccessorModel weightsAccessor) {
        return createFloatBuffer(weightsAccessor);
    }

    /**
     * Creates a Buffer for joint IDs from the given glTF AccessorModel.
     * Joint IDs can be bytes or shorts.
     */
    public static Buffer createJointsBuffer(AccessorModel jointsAccessor) {
        if (jointsAccessor == null) return null;

        AccessorData accessorData = jointsAccessor.getAccessorData();
        final int glComponentType = accessorData.getGlComponentType();
        ByteBuffer byteBuffer = accessorData.createByteBuffer();

        switch (glComponentType) {
            case 5121: // GL_UNSIGNED_BYTE
                return byteBuffer;
            case 5123: // GL_UNSIGNED_SHORT
                return byteBuffer.asShortBuffer();
            default:
                Log.e("GltfUtil", "Invalid component type for joints: " + glComponentType);
                throw new IllegalArgumentException("Invalid component type for joints: " + glComponentType);
        }
    }

    /**
     * Creates a Buffer for vertex colors from the given glTF AccessorModel.
     */
    public static Buffer createColorsBuffer(AccessorModel colorsAccessor) {
        if (colorsAccessor == null) return null;

        AccessorData accessorData = colorsAccessor.getAccessorData();
        final int glComponentType = accessorData.getGlComponentType();
        ByteBuffer byteBuffer = accessorData.createByteBuffer();

        switch (glComponentType) {
            case 5121: // GL_UNSIGNED_BYTE
                return byteBuffer;
            case 5123: // GL_UNSIGNED_SHORT
                return byteBuffer.asShortBuffer();
            case 5126: // GL_FLOAT
                return byteBuffer.asFloatBuffer();
            default:
                Log.e("GltfUtil", "Invalid component type for colors: " + glComponentType);
                throw new IllegalArgumentException("Invalid component type for colors: " + glComponentType);
        }
    }
}
