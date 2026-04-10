package org.the3deer.android.engine.model;

import org.the3deer.android.engine.animation.Animator;
import org.the3deer.android.util.Matrix;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class represents an entity in the world that can be animated. It
 * contains the model's VAO which contains the mesh data, the texture, and the
 * root joint of the joint hierarchy, or "skeleton". It also holds an int which
 * represents the number of joints that the model's skeleton contains, and has
 * its own {@link Animator} instance which can be used to apply animations to
 * this entity.
 *
 * @author andresoviedo
 */
public class AnimatedModel extends Object3D {

    private static final Logger logger = Logger.getLogger(AnimatedModel.class.getSimpleName());

    // skeleton
    private Skin skin;

    public AnimatedModel() {
        super();
    }

    public AnimatedModel(FloatBuffer vertexBuffer) {
        super(vertexBuffer);
    }

    public AnimatedModel(FloatBuffer vertexBuffer, Buffer drawOrderBuffer) {
        super(vertexBuffer, drawOrderBuffer);
    }

    public AnimatedModel(String id, FloatBuffer positions, FloatBuffer normals, FloatBuffer colors, FloatBuffer texCoords, Material material, Skin skin) {
        super(id, positions, normals, texCoords, colors, material);
        setSkin(skin);
    }

    public AnimatedModel setSkin(Skin skin) {
        this.skin = skin;
        return this;
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public AnimatedModel clone() {
        final AnimatedModel ret = new AnimatedModel();
        super.copy(ret);
        ret.setSkin(this.getSkin());
        return ret;
    }

    // FIXME:
    /**
     * This is the bind shape transform found in skin (ie. {@code <library_controllers><skin><bind_shape_matrix>}
     */
    public void setBindShapeMatrix(float[] matrix) {
        if (matrix == null) return;

        float[] vertex = new float[]{0, 0, 0, 1};
        float[] shaped = new float[]{0, 0, 0, 1};
        for (int i = 0; i < this.vertexArrayBuffer.capacity(); i += 3) {
            vertex[0] = this.vertexArrayBuffer.get(i);
            vertex[1] = this.vertexArrayBuffer.get(i + 1);
            vertex[2] = this.vertexArrayBuffer.get(i + 2);
            Matrix.multiplyMV(shaped, 0, matrix, 0, vertex, 0);
            this.vertexArrayBuffer.put(i, shaped[0]);
            this.vertexArrayBuffer.put(i + 1, shaped[1]);
            this.vertexArrayBuffer.put(i + 2, shaped[2]);
        }
        updateDimensions();
    }

    @Override
    public void debug() {
        try {
            // --- EXPANDED LOGGING ---
            logger.config("--- MODEL DATA --- " + getId());

            if (modelMatrix != null) {
                StringBuilder pos_sb = new StringBuilder("modelMatrix: ").append(Arrays.toString(modelMatrix));
                logger.config(pos_sb.toString());
            }

// Print first 30 floats (10 vertices)
            if (vertexArrayBuffer != null) {
                StringBuilder pos_sb = new StringBuilder("Positions: ").append("(").append(vertexArrayBuffer.capacity()).append(") ");
                for (int i = 0; i < 16 && i < vertexArrayBuffer.capacity(); i++) {
                    pos_sb.append(vertexArrayBuffer.get(i)).append(" ");
                }
                logger.config(pos_sb.toString());
            }

// Print first 30 floats (10 normals)
// IMPORTANT: Add a null check for finalNormals
            if (vertexNormalsArrayBuffer != null && vertexNormalsArrayBuffer.capacity() >= 30) {
                StringBuilder norm_sb = new StringBuilder("Normals:   ").append("(").append(vertexNormalsArrayBuffer.capacity()).append(") ");;
                for (int i = 0; i < 16 && i < vertexNormalsArrayBuffer.capacity(); i++) {
                    norm_sb.append(vertexNormalsArrayBuffer.get(i)).append(" ");
                }
                logger.config(norm_sb.toString());
            } else {
                logger.config("Normals: null or too short.");
            }

            // Print first 15 indices
            if (indexBuffer != null) {
                StringBuilder idx_sb = new StringBuilder("Indices:   ").append("(").append(indexBuffer.capacity()).append(") ");;
                idx_sb.append("(").append(indexBuffer.getClass().getSimpleName()).append(")");

                for (int i = 0; i < 16 && i < indexBuffer.capacity(); i++) {
                    idx_sb.append(IOUtils.getIntBufferValue(indexBuffer, i)).append(" ");
                }
                logger.config(idx_sb.toString());
            }

            if (textureCoordsArrayBuffer != null) {
                StringBuilder norm_sb = new StringBuilder("Textures:   ").append("(").append(textureCoordsArrayBuffer.capacity()).append(") ");;
                for (int i = 0; i < 16 && i < textureCoordsArrayBuffer.capacity(); i++) {
                    norm_sb.append(textureCoordsArrayBuffer.get(i)).append(" ");
                }
                logger.config(norm_sb.toString());
            } else {
                logger.config("Textures: null or too short.");
            }

            // --- END LOGGING ---
        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }
    }
}
