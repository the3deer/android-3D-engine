package org.the3deer.engine.android.shader.v2;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.SparseArray;

import org.the3deer.engine.android.shader.Shader;
import org.the3deer.engine.model.AnimatedModel;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Element;
import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Skin;
import org.the3deer.engine.model.Texture;
import org.the3deer.engine.util.GLUtil;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright 2013-2020 the3deer.org
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class ShaderImplV2 implements Shader {

    private static final Logger logger = Logger.getLogger(Shader.class.getSimpleName());

    // Preference Keys (define these as constants)
    public static final String KEY_SHADER_LIGHTING_TYPE = "shader_default_lighting_type";
    public static final String KEY_SHADER_ANIMATION_ENABLED = "shader_default_animation_enabled"; // Example for a boolean
    public static final String KEY_SHADER_COLORS_ENABLED = "shader_default_colors_enabled"; // Example for a boolean
    public static final String KEY_SHADER_TEXTURE_SELECTION = "shader_default_texture_enabled";

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXTURE_COORDS_PER_VERTEX = 2;
    private static final int COLOR_COORDS_PER_VERTEX = 4;

    private final static float[] DEFAULT_COLOR = Constants.COLOR_WHITE;
    private final static float[] NO_COLOR_MASK = Constants.COLOR_WHITE;

    // specification
    private final String id;
    private final String vertexShaderCode;
    private final String fragmentShaderCode;

    // features
    private final Set<String> features;
    private final boolean supportsColors;
    private final boolean supportsTextures;
    private final boolean supportsLighting;
    private final boolean supportsAnimation;
    private final boolean supportsNormalTexture;

    private final boolean supportsTextureCube;
    private final boolean supportBlending;
    private final boolean supportsTexturesTransformed;
    private final boolean supportsTransmissionTexture;
    private final boolean supportsMMatrix;
    private final boolean supportsNormals;

    // opengl program
    private final boolean checkGlError;
    private int mProgram;

    // animation data
    // put 0 to draw progressively, -1 to draw at once
    private long counter = -1;
    private double shift = -1d;

    // does the device support drawElements for GL_INT or not?
    private boolean drawUsingInt = true;
    private int textureCounter = 0;

    /**
     * Join transform names cache (optimization)
     */
    private final SparseArray<String> cache1 = new SparseArray<>();


    private boolean autoUseProgram = true;


    private boolean texturesEnabled = true;
    private boolean lightingEnabled = true;
    private boolean animationEnabled = true;

    // state
    private List<Texture> textures = new ArrayList<>();

    private final Set<String> logset = new HashSet<>();

    // handles for alpha uniforms
    private int uAlphaModeHandle;
    private int uAlphaCutoffHandle;

    @Override
    public int getId() {
        return mProgram;
    }

    @Override
    public String getName() {
        return id;
    }

    /**
     * Load the shaders into GPU. Requires having a GL Context.
     * This can be called in the onDrawFrame() Thread.
     *
     * @param id
     * @param vertexShaderCode
     * @param fragmentShaderCode
     * @return the compiled Shader
     */
    public static ShaderImplV2 getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        return new ShaderImplV2(id, vertexShaderCode, fragmentShaderCode);
    }

    private static boolean testShaderFeature(Set<String> outputFeatures, String shaderCode, String feature) {
        if (shaderCode.contains(feature)) {
            outputFeatures.add(feature);
            return true;
        }

        return false;
    }

    /**
     * Load the shaders into GPU. Requires having a GL Context.
     * This can be called in the onDrawFrame() Thread.
     *
     * @param id
     * @param vertexShaderCode
     * @param fragmentShaderCode
     */
    private ShaderImplV2(String id, String vertexShaderCode, String fragmentShaderCode) {

        this.id = id;
        this.vertexShaderCode = vertexShaderCode;
        this.fragmentShaderCode = fragmentShaderCode;

        logger.config("Checking features... " + id);
        final Set<String> shaderFeatures = new HashSet<>();
        final String shaderCode = vertexShaderCode + fragmentShaderCode;
        this.supportsMMatrix = testShaderFeature(shaderFeatures, shaderCode, "u_MMatrix");
        this.supportsNormals = testShaderFeature(shaderFeatures, shaderCode, "a_Normal")
                && testShaderFeature(shaderFeatures, shaderCode, "u_NormalMatrix");
        this.supportsColors = testShaderFeature(shaderFeatures, shaderCode, "a_Color");
        this.supportsNormalTexture = testShaderFeature(shaderFeatures, shaderCode, "a_Tangent");
        this.supportsTextures = testShaderFeature(shaderFeatures, shaderCode, "a_TexCoordinate");
        this.supportsLighting = testShaderFeature(shaderFeatures, shaderCode, "u_LightPos")
                && testShaderFeature(shaderFeatures, shaderCode, "u_cameraPos");
        this.supportsAnimation = testShaderFeature(shaderFeatures, shaderCode, "in_jointIndices")
                && testShaderFeature(shaderFeatures, shaderCode, "in_weights");
        this.supportsTextureCube = testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TextureCube");
        this.supportBlending = testShaderFeature(shaderFeatures, fragmentShaderCode, "u_AlphaCutoff") &&
                testShaderFeature(shaderFeatures, fragmentShaderCode, "u_AlphaMode");
        this.supportsTexturesTransformed = testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TextureTransformed");
        this.supportsTransmissionTexture = testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TransmissionTexture");
        this.features = shaderFeatures;
        this.checkGlError = false;

        this.init();
    }

    private void init() {
        logger.config("Loading Shader... " + id);

        // load shaders
        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // compile program
        logger.config("Compiled Shader " + id);
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, features.toArray(new String[0]));
        logger.config("Linked Shader " + id + " with program " + mProgram);

        // Get uniform handles after linking
        if (supportBlending) {
            uAlphaModeHandle = GLES20.glGetUniformLocation(mProgram, "u_AlphaMode");
            uAlphaCutoffHandle = GLES20.glGetUniformLocation(mProgram, "u_AlphaCutoff");
        }
    }

    @Override
    public void setAutoUseProgram(boolean autoUseProgram) {
        this.autoUseProgram = autoUseProgram;
    }

    @Override
    public void useProgram() {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        if (checkGlError && GLUtil.checkGlError("glUseProgram")) {
            throw new RuntimeException("glUseProgram failed");
        }
    }

    @Override
    public int getProgram() {
        return mProgram;
    }

    public void setTexturesEnabled(boolean texturesEnabled) {
        this.texturesEnabled = texturesEnabled;
    }

    public void setLightingEnabled(boolean lightingEnabled) {
        this.lightingEnabled = lightingEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    @Override
    public void reset() {

        // Re-initialize shader program as the context might have been lost
        this.init();

        // reset textures so they can be reloaded in opengl thread
        logger.info("Deleting textures... Total: " + textures.size());
        for (Texture texture : textures) {
            if (texture.getId() != -1) {
                GLES20.glDeleteTextures(1, new int[]{texture.getId()}, 0);
                texture.setId(-1);
            }
        }
        textures.clear();
        logger.config("Textures deleted");
    }

    @Override
    public void draw(Object3D obj, float[] pMatrix, float[] vMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawMode, int drawSize) {

        if (this.autoUseProgram) {
            useProgram();
        }


        setUniformMatrix4(vMatrix, "u_VMatrix");
        setUniformMatrix4(pMatrix, "u_PMatrix");

        // reset texture counter
        textureCounter = 0;

        // mvp matrix for position + lighting + animation
        if (supportsMMatrix) {

            // Refactoring: move the logic from AnimatedModel.getModelMatrix() into the shader itself.
            // This ensures that Object3D always returns its true logical model matrix,
            // while the shader decides when to ignore it (e.g., during skinning).
            float[] modelMatrix = obj.getModelMatrix();

            // Check if this is an animated model with a skin.
            if (obj instanceof AnimatedModel) {
                AnimatedModel animatedModel = (AnimatedModel) obj;
                if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                    // Skinned models have their world transformation baked into the joint matrices (u_jointMat).
                    // Therefore, the main model matrix must be identity to avoid double-transformation.
                    modelMatrix = Math3DUtils.IDENTITY_MATRIX;
                }
            }

            setUniformMatrix4(modelMatrix, "u_MMatrix");
        }

        // pass in vertex buffer
        int mPositionHandle = setVBO("a_Position", obj.getVertexBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);

        // pass in normals buffer for lighting
        int mNormalHandle = -1;
        if (supportsNormals && obj.getNormalsBuffer() != null) {
            mNormalHandle = setVBO("a_Normal", obj.getNormalsBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);
            
            // Recalculate normal matrix if the model matrix changed above
            float[] normalMatrix = obj.getNormalMatrix();
             if (obj instanceof AnimatedModel) {
                AnimatedModel animatedModel = (AnimatedModel) obj;
                if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                   normalMatrix = Math3DUtils.IDENTITY_MATRIX;
                }
            }
            setUniformMatrix4(normalMatrix, "u_NormalMatrix");
        }

        // pass in normals map for lighting
        int mNormalMapHandle = -1;
        if (supportsNormalTexture) {
            boolean toggle = obj.getVertexNormalsArrayBuffer() != null
                    && obj.getTangentBuffer() != null &&
                    obj.getMaterial() != null && obj.getMaterial().getNormalTexture() != null;

            setFeatureFlag("u_NormalTextured", toggle);
            if (toggle) {
                loadTexture(obj.getMaterial().getNormalTexture());
                setTexture(obj.getMaterial().getNormalTexture(), "u_NormalTexture", 1);
                mNormalMapHandle = setVBO("a_Tangent", obj.getTangentBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);
            }
        }

        // pass in color or colors array
        if (obj.getColor() != null) {
            setUniform4(obj.getColor(), "u_Color");
        } else {
            setUniform4(DEFAULT_COLOR, "u_Color");
        }

        // colors
        int mColorHandle = -1;
        if (supportsColors) {
            setFeatureFlag("u_Coloured", obj.getColorsBuffer() != null);
            if (obj.getColorsBuffer() != null) {
                mColorHandle = setVBO("a_Color", obj.getColorsBuffer(), COLOR_COORDS_PER_VERTEX, -1);
            }
        }

        // pass in color mask - i.e. stereoscopic
        setUniform4(colorMask != null ? colorMask : NO_COLOR_MASK, "u_ColorMask");

        // --- NEW: BLENDING LOGIC ---
        Material material = obj.getMaterial();
        if (supportBlending && material != null && material.getAlphaMode() != Material.AlphaMode.OPAQUE) {
            //GLES20.glEnable(GLES20.GL_BLEND);
            //GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            GLES20.glUniform1i(uAlphaModeHandle, material.getAlphaMode().ordinal());
            GLES20.glUniform1f(uAlphaCutoffHandle, material.getAlphaCutoff());
        } else {
            //GLES20.glDisable(GLES20.GL_BLEND);
            if (supportBlending) {
                GLES20.glUniform1i(uAlphaModeHandle, Material.AlphaMode.OPAQUE.ordinal());
                GLES20.glUniform1f(uAlphaCutoffHandle, 0.5f); // Default cutoff
            }
        }
        // -------------------------

        // pass in texture UV buffer
        int mTextureHandle = -1;
        if (supportsTextures) {
            setFeatureFlag("u_Textured", false);

            if (obj.getMaterial() != null && obj.getTextureCoordsArrayBuffer() != null) {
                mTextureHandle = setVBO("a_TexCoordinate", obj.getTextureCoordsArrayBuffer(), TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT);

                if (obj.getMaterial().getColorTexture() != null) {
                    loadTexture(obj.getMaterial().getColorTexture());
                    setTexture(obj.getMaterial().getColorTexture(), "u_Texture", 0);
                    setFeatureFlag("u_Textured", texturesEnabled);
                }


                boolean enableEmissive = obj.getMaterial().getEmissiveTexture() != null && obj.getMaterial().getEmissiveFactor() != null;
                setFeatureFlag("u_EmissiveTextured", enableEmissive);
                if (enableEmissive) {
                    loadTexture(obj.getMaterial().getEmissiveTexture());
                    setTexture(obj.getMaterial().getEmissiveTexture(), "u_EmissiveTexture", 2);
                    setUniform3(obj.getMaterial().getEmissiveFactor(), "u_EmissiveFactor");
                }
            }
        }

        // pass in the Skybox texture
        if (obj.getMaterial() != null && obj.getMaterial().getColorTexture() != null && supportsTextureCube) {
            loadTexture(obj.getMaterial().getColorTexture());
            setTextureCube(obj.getMaterial().getColorTexture().getId(), 3);
        }

        // pass in light position for lighting
        if (supportsLighting && lightPosInWorldSpace != null && cameraPos != null) {
            boolean toggle = lightingEnabled && obj.getVertexNormalsArrayBuffer() != null;
            setFeatureFlag("u_Lighted", toggle);
            setUniform3(lightPosInWorldSpace, "u_LightPos");
            setUniform3(cameraPos, "u_cameraPos");
        }

        // pass in joint transformation for animated model
        int in_weightsHandle = -1;
        int in_jointIndicesHandle = -1;


        if (supportsAnimation) {
            boolean toggle = false;
            if (obj instanceof AnimatedModel && this.animationEnabled && Constants.ANIMATIONS_ENABLED) {
                final Skin skin = ((AnimatedModel) obj).getSkin();
                toggle = ((AnimatedModel) obj).getSkin() != null
                        && ((AnimatedModel) obj).getSkin().getWeightsBuffer() != null
                        && ((AnimatedModel) obj).getSkin().getJointsBuffer() != null
                        && ((AnimatedModel) obj).getSkin().getJointTransforms() != null;
                if (toggle) {
                    in_weightsHandle = setVBO("in_weights", skin.getWeightsBuffer(), skin.getWeightsComponents(), -1);
                    in_jointIndicesHandle = setVBO("in_jointIndices", skin.getJointsBuffer(), skin.getJointComponents(), -1);
                    setUniformMatrix4(((AnimatedModel) obj).getSkin().getBindShapeMatrix(), "u_BindShapeMatrix");
                    setJointTransforms((AnimatedModel) obj);
                }
            }

            //logger.finest("u_Animated: " + toggle + " ("+obj.getId()+")");
            setFeatureFlag("u_Animated", toggle);

            // debug
            if (Constants.DEBUG) {
                if (!logset.contains(obj.getId())) {
                    logger.finest("id: " + obj.getId() + ", u_Animated " + toggle);
                }
            }
        }

        if (Constants.DEBUG) {
            if (!logset.contains(obj.getId())) {
                logger.finest("id: " + obj.getId() + ", modelMatrix = " + Arrays.toString(obj.getModelMatrix()));
                logset.add(obj.getId());
            }
        }

        // FIXME:
        if (obj.getElements() != null && obj.getElements().size() > 1) {
            for (int i = 0; i < obj.getElements().size(); i++) {
                if (obj.getElements().get(i).getMaterial() == null) {
                    // draw mesh
                    drawElement(obj, obj.getElements().get(i), drawMode, drawSize);

                }
            }
            for (int i = 0; i < obj.getElements().size(); i++) {
                // draw mesh
                if (obj.getElements().get(i).getMaterial() != null && obj.getElements().get(i).getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE)
                    drawElement(obj, obj.getElements().get(i), drawMode, drawSize);
            }
            for (int i = 0; i < obj.getElements().size(); i++) {
                if (obj.getElements().get(i).getMaterial() != null && obj.getElements().get(i).getMaterial().getAlphaMode() != Material.AlphaMode.OPAQUE) {
                    // draw mesh
                    drawElement(obj, obj.getElements().get(i), drawMode, drawSize);

                }
            }

        } else {
            // draw mesh
            drawElement(obj, null, drawMode, drawSize);

        }

        // Disable vertex handlers
        disableVBO(mPositionHandle);
        disableVBO(mColorHandle);
        disableVBO(mNormalHandle);
        disableVBO(mNormalMapHandle);
        disableVBO(mTextureHandle);
        disableVBO(in_weightsHandle);
        disableVBO(in_jointIndicesHandle);
    }

    private void loadTexture(Texture texture) {

        // check
        if (texture == null || texture.hasId()) return;

        // load
        final int textureId;
        if (texture.isCubeMap()) {
            textureId = GLUtil.loadCubeMap(texture.getCubeMap());
            texture.setId(textureId);
        } else if (texture.getBitmap() != null && !texture.getBitmap().isRecycled()) {
            textureId = GLUtil.loadTexture(texture.getBitmap());
            texture.setId(textureId);
        } else if (texture.getData() != null) {
            textureId = GLUtil.loadTexture(texture.getData());
            texture.setId(textureId);
        } else if (texture.getBuffer() != null) {
            textureId = GLUtil.loadTexture(texture.getBuffer());
            texture.setId(textureId);
        } else {
            logger.log(Level.SEVERE, "No texture data for " + id);
            return;
        }

        textures.add(texture);
       logger.finest("Loaded texture " + textureId + " for " + id);
    }

    private int setVBO(final String shaderVariableName, final Buffer buffer, int componentsPerVertex, int glType) {

        // check
        if (buffer == null) return -1;

        int handler = GLES20.glGetAttribLocation(mProgram, shaderVariableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetAttribLocation");
        }

        GLES20.glEnableVertexAttribArray(handler);
        if (checkGlError) {
            GLUtil.checkGlError("glEnableVertexAttribArray");
        }

        // FIXME: type should be a preset
        if (glType == -1) {
            if (buffer instanceof FloatBuffer) {
                glType = GLES20.GL_FLOAT;
            } else if (buffer instanceof IntBuffer) {
                glType = GLES20.GL_UNSIGNED_INT;
            } else if (buffer instanceof ShortBuffer) {
                glType = GLES20.GL_UNSIGNED_SHORT;
            } else if (buffer instanceof ByteBuffer) {
                glType = GLES20.GL_UNSIGNED_BYTE;
            } else {
                throw new IllegalArgumentException("Unsupported buffer type: " + buffer.getClass());
            }
        }

        buffer.position(0);
        GLES20.glVertexAttribPointer(handler, componentsPerVertex, glType, false, 0, buffer);
        if (checkGlError) {
            GLUtil.checkGlError("glVertexAttribPointer");
        }

        return handler;
    }

    private void setUniformInt(int value, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform1i(handle, value);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform1f");
        }
    }

    private void setUniform1(float value, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform1f(handle, value);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform1f");
        }
    }

    private void setUniform2(float[] uniform2f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform2fv(handle, 1, uniform2f, 0);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform2fv");
        }
    }

    private void setUniform3(float[] uniform3f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform3fv(handle, 1, uniform3f, 0);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform3fv");
        }
    }

    private void setUniform4(float[] uniform4f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform4fv(handle, 1, uniform4f, 0);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform4fv");
        }
    }

    private void setUniformMatrix4(float[] matrix, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        // Pass in the light position in eye space.
        GLES20.glUniformMatrix4fv(handle, 1, false, matrix, 0);
        if (checkGlError) {
            GLUtil.checkGlError("glUniformMatrix4fv");
        }
    }

    private void disableVBO(int handle) {
        if (handle != -1) {
            GLES20.glDisableVertexAttribArray(handle);
            if (checkGlError) {
                GLUtil.checkGlError("glDisableVertexAttribArray");
            }
        }
    }

    private boolean supportsMMatrix() {
        return supportsMMatrix;
    }

    private boolean supportsTextureCube() {
        return supportsTextureCube;
    }

    private boolean supportsColors() {
        return supportsColors;
    }

    private boolean supportsNormals() {
        return supportsNormals;
    }

    private boolean supportsTangent() {
        return supportsNormalTexture;
    }

    private boolean supportsLighting() {
        return supportsLighting;
    }

    private boolean supportsTextures() {
        return supportsTextures;
    }

    private boolean supportsTransmissionTexture() {
        return supportsTransmissionTexture;
    }

    private boolean supportsTextureTransformed() {
        return supportsTexturesTransformed;
    }

    private boolean supportsBlending() {
        return supportBlending;
    }

    private void setFeatureFlag(String variableName, boolean enabled) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }
        GLES20.glUniform1i(handle, enabled ? 1 : 0);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform1i");
        }
    }

    private void setTexture(Texture texture, String variableName, int textureIndex) {

        // check
        if (texture == null || !texture.hasId()) return;

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, variableName);
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIndex);
        if (checkGlError) {
            GLUtil.checkGlError("glActiveTexture");
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getId());
        if (checkGlError) {
            GLUtil.checkGlError("glBindTexture");
        }

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, textureIndex);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform1i");
        }
    }

    private void setTextureCube(int textureId, int textureIndex) {

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureCube");
        if (checkGlError) {
            GLUtil.checkGlError("glGetUniformLocation");
        }

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIndex);
        if (checkGlError) {
            GLUtil.checkGlError("glActiveTexture");
        }

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId);
        if (checkGlError) {
            GLUtil.checkGlError("glBindTexture");
        }

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, textureIndex);
        if (checkGlError) {
            GLUtil.checkGlError("glUniform1i");
        }

    }

    private boolean supportsJoints() {
        return supportsAnimation;
    }

    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] jointTransformsArray = animatedModel.getSkin().getJointTransforms();

        // TODO: optimize this (memory allocation)
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransform = jointTransformsArray[i];
            String jointTransformHandleName = cache1.get(i);
            if (jointTransformHandleName == null) {
                jointTransformHandleName = "jointTransforms[" + i + "]";
                cache1.put(i, jointTransformHandleName);
            }
            setUniformMatrix4(jointTransform, jointTransformHandleName);

            // debug
            if (Constants.DEBUG) {
                if (!logset.contains(animatedModel.getId())) {
                    logger.finest( "id: " + animatedModel.getId() + ", jointTransform[" + i + "] = " + Arrays.toString(jointTransform));
                }
            }
        }
    }

    private void drawElement(Object3D obj, Element element, int drawMode, int drawSize) {


        int drawBufferType = -1;
        Buffer drawOrderBuffer;
        final FloatBuffer vertexBuffer;
        if (!obj.isIndexed()) {
            drawOrderBuffer = null;
            vertexBuffer = obj.getVertexBuffer();
        } else {
            vertexBuffer = obj.getVertexBuffer();
            drawOrderBuffer = obj.getIndexBuffer();

            if (!drawUsingInt && drawOrderBuffer instanceof IntBuffer) {
                ShortBuffer indexShortBuffer;
                drawOrderBuffer.position(0);
                indexShortBuffer = IOUtils.createShortBuffer(drawOrderBuffer.capacity());
                for (int j = 0; j < indexShortBuffer.capacity(); j++) {
                    indexShortBuffer.put((short) ((IntBuffer) drawOrderBuffer).get(j));
                }
                drawOrderBuffer = indexShortBuffer;
                obj.setIndexBuffer(drawOrderBuffer);
            }

            if (drawOrderBuffer instanceof IntBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_INT;
            } else if (drawOrderBuffer instanceof ShortBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_SHORT;
            } else if (drawOrderBuffer instanceof ByteBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_BYTE;
            }
        }
        vertexBuffer.position(0);

        List<int[]> drawModeList = obj.getDrawModeList();
        if (drawModeList != null) {
            if (!obj.isIndexed()) {
                drawPolygonsUsingArrays(drawMode, drawModeList);
            } else {
                drawPolygonsUsingIndex(drawOrderBuffer, drawBufferType, drawModeList);
            }
        } else {
            if (!obj.isIndexed()) {
                drawTrianglesUsingArrays(drawMode, drawSize, vertexBuffer.capacity() / COORDS_PER_VERTEX);
            } else {
                drawTrianglesUsingIndex(obj, element, drawMode, drawSize, drawOrderBuffer, drawBufferType);
            }
        }
    }

    private void drawTrianglesUsingArrays(int drawMode, int drawSize, int drawCount) {
        if (drawSize <= 0) {
            // if we want to animate, initialize counter=0 at variable declaration
            if (this.shift >= 0) {
                double rotation = ((SystemClock.uptimeMillis() % 10000) / 10000f) * (Math.PI * 2);

                if (this.shift == 0d) {
                    this.shift = rotation;
                }
                drawCount = (int) ((Math.sin(rotation - this.shift + Math.PI / 2 * 3) + 1) / 2f * drawCount);
            }
            GLES20.glDrawArrays(drawMode, 0, drawCount);
            if (checkGlError) {
                GLUtil.checkGlError("glDrawArrays");
            }
        } else {
            for (int i = 0; i < drawCount; i += drawSize) {
                GLES20.glDrawArrays(drawMode, i, drawSize);
                if (checkGlError) {
                    GLUtil.checkGlError("glDrawArrays");
                }
            }
        }
    }

    private void drawTrianglesUsingIndex(Object3D obj, Element el, int drawMode, int drawSize, Buffer drawOrderBuffer, int drawBufferType) {


        if (drawSize <= 0 && obj.getElements() != null) {

            if (el != null) {
                drawObjectElement(obj, el, drawMode, drawBufferType);
            } else {

                int size = obj.getElements().size();
                // draw rest
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);

                    if (element.getMaterial() == null) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw opaque elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);
                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw opaque elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);
                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.MASK) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw translucent elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);

                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.BLEND) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }
            }

        } else {
            for (int i = 0; i < drawOrderBuffer.capacity(); i += drawSize) {
                drawOrderBuffer.position(i);
                GLES20.glDrawElements(drawMode, drawSize, drawBufferType, drawOrderBuffer);
                boolean error = GLUtil.checkGlError("glDrawElements");
                if (drawUsingInt && error) {
                    logger.log(Level.SEVERE,  "Exception drawing elements. Switching to ShortBuffer");
                    drawUsingInt = false;
                }
            }
        }
    }

    private void drawObjectElement(Object3D obj, Element element, int drawMode, int drawBufferType) {

        Buffer drawOrderBuffer = element.getIndexBuffer();

        if (!drawUsingInt && drawOrderBuffer instanceof IntBuffer) {
            ShortBuffer indexShortBuffer;
            drawOrderBuffer.position(0);
            indexShortBuffer = IOUtils.createShortBuffer(drawOrderBuffer.capacity());
            for (int j = 0; j < indexShortBuffer.capacity(); j++) {
                indexShortBuffer.put((short) ((IntBuffer) drawOrderBuffer).get(j));
            }
            drawOrderBuffer = indexShortBuffer;
            element.setIndexBuffer(drawOrderBuffer);
        }

        if (drawOrderBuffer instanceof IntBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_INT;
        } else if (drawOrderBuffer instanceof ShortBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_SHORT;
        } else if (drawOrderBuffer instanceof ByteBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_BYTE;
        }

        // element material
        Material material = element.getMaterial();

        // override if null
        if (material == null) {
            material = obj.getMaterial();
        }

        if (material != null && material.getColor() != null) {
            setUniform4(material.getColor(), "u_Color");
        } else {
            //setUniform4(DEFAULT_COLOR, "u_Color");
        }

        // default is no textured
        if (supportsTextures) {
            setFeatureFlag("u_Textured", obj.getTextureCoordsArrayBuffer() != null
                    && material != null && material.getColorTexture() != null
                    && material.getColorTexture().hasId()
                    && texturesEnabled);
        }

        // texture transform (Khronos)
        if (supportsTexturesTransformed) {
            setFeatureFlag("u_TextureTransformed", false);

            if (material != null) {
                try {
                    if (material.getColorTexture() != null &&
                            material.getColorTexture().getExtensions() != null &&
                            material.getColorTexture().getExtensions().containsKey("KHR_texture_transform")) {
                        Map<String, ?> extensions = (Map<String, ?>) material.getColorTexture().getExtensions().get("KHR_texture_transform");
                        List<Double> offset = (List<Double>) extensions.get("offset");
                        if (offset != null) {
                            setUniform2(new float[]{offset.get(0).floatValue(), offset.get(1).floatValue()}, "u_TextureOffset");
                        } else {
                            setUniform2(new float[]{0f, 0f}, "u_TextureOffset");
                        }
                        List<Double> scale = (List<Double>) extensions.get("scale");
                        if (scale != null) {
                            setUniform2(new float[]{scale.get(0).floatValue(), scale.get(1).floatValue()}, "u_TextureScale");
                        } else {
                            setUniform2(new float[]{1f, 1f}, "u_TextureScale");
                        }
                        Double rotation = (Double) extensions.get("rotation");
                        if (rotation != null) {
                            setUniform1(rotation.floatValue(), "u_TextureRotation");
                        } else {
                            setUniform1(0, "u_TextureRotation");
                        }
                        setFeatureFlag("u_TextureTransformed", true);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        textureCounter = 4;
        if (material != null) {

            // set alpha cutoff
            if (supportBlending) {
                setUniform1(material.getAlphaCutoff(), "u_AlphaCutoff");
                setUniformInt(material.getAlphaMode().ordinal(), "u_AlphaMode");
            }

            if (supportsTextures && obj.getTextureCoordsArrayBuffer() != null
                    && material.getColorTexture() != null
                    && texturesEnabled) {
                loadTexture(material.getColorTexture());
                setTexture(material.getColorTexture(), "u_Texture", textureCounter++);
                setFeatureFlag("u_Textured", true);
            }

            // transmission map
            if (supportsTransmissionTexture) {
                boolean toggle = material.getTransmissionTexture() != null;
                loadTexture(material.getTransmissionTexture());
                setTexture(material.getTransmissionTexture(), "u_TransmissionTexture", textureCounter++);
                setUniform1(material.getThicknessFactor(), "u_TransmissionFactor");
                setFeatureFlag("u_TransmissionTextured", toggle);
            }

            if (material.getNormalTexture() != null) {
                loadTexture(material.getNormalTexture());
                setTexture(material.getNormalTexture(), "u_NormalTexture", textureCounter++);
                setFeatureFlag("u_NormalTextured", true);
            }

            boolean enableEmissive = material.getEmissiveTexture() != null && material.getEmissiveFactor() != null;
            setFeatureFlag("u_EmissiveTextured", enableEmissive);
            if (enableEmissive) {
                loadTexture(material.getEmissiveTexture());
                setTexture(material.getEmissiveTexture(), "u_EmissiveTexture", textureCounter++);
                setUniform3(material.getEmissiveFactor(), "u_EmissiveFactor");
            }
        }

        // draw element
        drawOrderBuffer.position(0);
        GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                drawOrderBuffer);

    }

    private void drawPolygonsUsingIndex(Buffer drawOrderBuffer, int drawBufferType, List<int[]> polygonsList) {
        for (int i = 0; i < polygonsList.size(); i++) {
            int[] drawPart = polygonsList.get(i);
            int drawModePolygon = drawPart[0];
            int vertexPos = drawPart[1];
            int drawSizePolygon = drawPart[2];
            drawOrderBuffer.position(vertexPos);
            GLES20.glDrawElements(drawModePolygon, drawSizePolygon, drawBufferType, drawOrderBuffer);
            boolean error = GLUtil.checkGlError("glDrawElements");
            if (drawUsingInt && error) {
                logger.log(Level.SEVERE,  "Exception drawing elements. Switching to ShortBuffer");
                drawUsingInt = false;
            }
        }
    }

    private void drawPolygonsUsingArrays(int drawMode, List<int[]> polygonsList) {
        for (int j = 0; j < polygonsList.size(); j++) {
            int[] polygon = polygonsList.get(j);
            int drawModePolygon = polygon[0];
            int vertexPos = polygon[1];
            int drawSizePolygon = polygon[2];
            if (drawMode == GLES20.GL_LINE_LOOP && polygon[2] > 3) {
                for (int i = 0; i < polygon[2] - 2; i++) {
                    GLES20.glDrawArrays(drawMode, polygon[1] + i, 3);
                    if (checkGlError) {
                        GLUtil.checkGlError("glDrawArrays");
                    }
                }
            } else {
                GLES20.glDrawArrays(drawMode, polygon[1], polygon[2]);
                if (checkGlError) {
                    GLUtil.checkGlError("glDrawArrays");
                }
            }
        }
    }

    @Override
    public String toString() {
        return "GLES20Renderer{" +
                "id='" + id + '\'' +
                ", features=" + features +
                '}';
    }
}