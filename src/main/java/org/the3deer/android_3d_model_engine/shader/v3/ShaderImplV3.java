package org.the3deer.android_3d_model_engine.shader.v3;

import android.content.Context;
import android.opengl.GLES30;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.util.android.GLUtil;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenGL ES 3.x High-Performance Shader Implementation.
 * Uses VAOs and VBOs for maximum efficiency.
 * 
 * @author andresoviedo
 * @author Gemini AI
 */
public class ShaderImplV3 implements Shader, PreferenceAdapter {

    private static final String TAG = ShaderImplV3.class.getSimpleName();

    private final String id;
    private final int mProgram;
    private final Set<String> features;

    // Feature toggles
    private final boolean supportsMMatrix;
    private final boolean supportsLighting;
    private final boolean supportsAnimation;
    private final boolean supportsTextures;
    private final boolean supportsNormalTexture;
    private final boolean supportsEmissiveTexture;
    private final boolean supportsTransmissionTexture;
    private final boolean supportsTexturesTransformed;
    private final boolean supportBlending;
    private final boolean supportsColors;
    private final boolean supportsTextureCube;

    private boolean autoUseProgram = true;

    // variables
    private boolean lightingEnabled = true;
    private boolean texturesEnabled = true;

    private final SparseArray<String> jointCache = new SparseArray<>();
    private final List<Texture> textures = new ArrayList<>();

    // Internal GpuManager instance for asset retrieval
    private final GpuManager gpuManager = new GpuManager();

    public static ShaderImplV3 getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        return new ShaderImplV3(id, vertexShaderCode, fragmentShaderCode);
    }

    private ShaderImplV3(String id, String vertexShaderCode, String fragmentShaderCode) {
        this.id = id;
        this.features = new HashSet<>();

        this.supportsMMatrix = vertexShaderCode.contains("u_MMatrix");
        this.supportsLighting = fragmentShaderCode.contains("u_LightPos");
        this.supportsAnimation = vertexShaderCode.contains("in_jointIndices") && vertexShaderCode.contains("in_weights");
        this.supportsTextures = vertexShaderCode.contains("a_TexCoordinate");
        this.supportsNormalTexture = fragmentShaderCode.contains("u_NormalTexture") || vertexShaderCode.contains("a_Tangent") || fragmentShaderCode.contains("u_NormalTextured");
        this.supportsEmissiveTexture = fragmentShaderCode.contains("u_EmissiveTexture");
        this.supportsTransmissionTexture = fragmentShaderCode.contains("u_TransmissionTexture");
        this.supportsTexturesTransformed = fragmentShaderCode.contains("u_TextureTransformed");
        this.supportBlending = fragmentShaderCode.contains("u_AlphaCutoff") && fragmentShaderCode.contains("u_AlphaMode");
        this.supportsColors = vertexShaderCode.contains("a_Color");
        this.supportsTextureCube = fragmentShaderCode.contains("u_TextureCube");

        int vertexShader = GLUtil.loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, null);
    }

    @Override
    public void setLightingEnabled(boolean lightingEnabled) {
        this.lightingEnabled = lightingEnabled;
    }

    @Override
    public void setTexturesEnabled(boolean texturesEnabled) {
        this.texturesEnabled = texturesEnabled;
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPos, float[] colorMask, float[] cameraPos, int drawMode, int drawSize) {
        GpuAsset gpuAsset = gpuManager.getAsset(obj);
        if (gpuAsset == null) return;
        draw(gpuAsset, obj, pMatrix, vMatrix, lightPos, colorMask, cameraPos);
    }

    public void draw(GpuAsset asset, Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPos, float[] colorMask, float[] cameraPos) {
        if (this.autoUseProgram) {
            useProgram();
        }

        // 1. Set Global Matrices
        setUniformMatrix4(vMatrix, "u_VMatrix");
        setUniformMatrix4(pMatrix, "u_PMatrix");

        // 2. Set Object Matrix
        float[] modelMatrix = obj.getModelMatrix();
        if (obj instanceof AnimatedModel) {
            AnimatedModel animatedModel = (AnimatedModel) obj;
            if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                // If skinned, world transform is in the bones, so model matrix is identity
                modelMatrix = Math3DUtils.IDENTITY_MATRIX;
            }
        }
        setUniformMatrix4(modelMatrix, "u_MMatrix");

        // 3. Set Normal Matrix
        float[] normalMatrix = obj.getNormalMatrix();
        if (obj instanceof AnimatedModel) {
            AnimatedModel animatedModel = (AnimatedModel) obj;
            if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                normalMatrix = Math3DUtils.IDENTITY_MATRIX;
            }
        }
        setUniformMatrix4(normalMatrix, "u_NormalMatrix");

        // 4. Set Lighting & Camera
        if (supportsLighting) {
            setUniform3(lightPos, "u_LightPos");
            setUniform3(cameraPos, "u_cameraPos");
            setFeatureFlag("u_Lighted", lightingEnabled && lightPos != null);
        }

        // 5. Set Animation Data
        boolean animated = false;
        if (supportsAnimation && asset.hasSkin() && obj instanceof AnimatedModel) {
            AnimatedModel anim = (AnimatedModel) obj;
            Skin skin = anim.getSkin();
            if (skin != null && skin.getJointTransforms() != null && Constants.ANIMATIONS_ENABLED) {
                setUniformMatrix4(skin.getBindShapeMatrix(), "u_BindShapeMatrix");
                setJointTransforms(anim);
                animated = true;
            }
        }
        setFeatureFlag("u_Animated", animated);

        // 6. Set Per-Vertex Color Flag
        if (supportsColors) {
            setFeatureFlag("u_Coloured", asset.hasColors());
        }

        // 7. Set Global Color Mask (Stereoscopic) - Default to White to prevent blackout
        setUniform4(colorMask != null ? colorMask : Constants.COLOR_WHITE, "u_ColorMask");

        // 8. Bind VAO and Draw (Handle Multi-Elements)
        asset.bind();
        List<GpuAsset.GpuElement> gpuElements = asset.getGpuElements();
        if (gpuElements != null && !gpuElements.isEmpty()) {
            List<Element> elements = obj.getElements();
            for (int i = 0; i < gpuElements.size(); i++) {
                GpuAsset.GpuElement gpuElement = gpuElements.get(i);
                
                // Set Element-specific Color/Material
                Material material = (elements != null && i < elements.size()) ? elements.get(i).getMaterial() : null;
                if (material == null) {
                    material = obj.getMaterial();
                }
                
                bindMaterial(asset, material);

                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, gpuElement.getEboId());
                GLES30.glDrawElements(asset.getDrawMode(), gpuElement.getCount(), gpuElement.getType(), 0);
            }
        } else {
            bindMaterial(asset, obj.getMaterial());
            GLES30.glDrawArrays(asset.getDrawMode(), 0, asset.getVertexCount());
        }
        asset.unbind();
    }

    private void bindMaterial(GpuAsset asset, Material material) {
        // Color
        float[] color = (material != null && material.getColor() != null) ? material.getColor() : Constants.COLOR_WHITE;
        setUniform4(color, "u_Color");

        // Blending
        if (supportBlending) {
            if (material != null) {
                setUniformInt(material.getAlphaMode().ordinal(), "u_AlphaMode");
                setUniform1(material.getAlphaCutoff(), "u_AlphaCutoff");
            } else {
                setUniformInt(Material.AlphaMode.OPAQUE.ordinal(), "u_AlphaMode");
                setUniform1(0.5f, "u_AlphaCutoff");
            }
        }

        // Textures
        if (supportsTextures) {
            boolean hasTexture = material != null && material.getColorTexture() != null;
            boolean textured = asset.hasTexCoords() && hasTexture && texturesEnabled;

            setFeatureFlag("u_Textured", textured);
            if (textured) {
                loadTexture(material.getColorTexture());
                setTexture(material.getColorTexture(), "u_Texture", 0);
                
                // Texture Transform
                if (supportsTexturesTransformed) {
                    bindTextureTransform(material.getColorTexture());
                }
            }
        }

        // Normal Texture
        if (supportsNormalTexture) {
            boolean normalTextured = material != null && material.getNormalTexture() != null
                    && asset.hasTangents();
            setFeatureFlag("u_NormalTextured", normalTextured);
            if (normalTextured) {
                loadTexture(material.getNormalTexture());
                setTexture(material.getNormalTexture(), "u_NormalTexture", 1);
            }
        }

        // Emissive Texture
        if (supportsEmissiveTexture) {
            boolean emissiveTextured = material != null && material.getEmissiveTexture() != null;
            setFeatureFlag("u_EmissiveTextured", emissiveTextured);
            if (emissiveTextured) {
                loadTexture(material.getEmissiveTexture());
                setTexture(material.getEmissiveTexture(), "u_EmissiveTexture", 2);
                if (material.getEmissiveFactor() != null) {
                    setUniform3(material.getEmissiveFactor(), "u_EmissiveFactor");
                }
            }
        }

        // Transmission Texture
        if (supportsTransmissionTexture) {
            boolean transmissionTextured = material != null && material.getTransmissionTexture() != null;
            setFeatureFlag("u_TransmissionTextured", transmissionTextured);
            if (transmissionTextured) {
                loadTexture(material.getTransmissionTexture());
                setTexture(material.getTransmissionTexture(), "u_TransmissionTexture", 3);
                setUniform1(material.getThicknessFactor(), "u_TransmissionFactor");
            }
        }

        // Cube Texture
        if (supportsTextureCube && material != null && material.getColorTexture() != null) {
            setTextureCube(material.getColorTexture().getId(), "u_TextureCube", 4);
        }
    }

    private void bindTextureTransform(Texture texture) {
        try {
            if (texture.getExtensions() != null && texture.getExtensions().containsKey("KHR_texture_transform")) {
                Map<String, ?> extensions = (Map<String, ?>) texture.getExtensions().get("KHR_texture_transform");
                
                List<Double> offset = (List<Double>) extensions.get("offset");
                setUniform2(offset != null ? new float[]{offset.get(0).floatValue(), offset.get(1).floatValue()} : new float[]{0f, 0f}, "u_TextureOffset");
                
                List<Double> scale = (List<Double>) extensions.get("scale");
                setUniform2(scale != null ? new float[]{scale.get(0).floatValue(), scale.get(1).floatValue()} : new float[]{1f, 1f}, "u_TextureScale");
                
                Double rotation = (Double) extensions.get("rotation");
                setUniform1(rotation != null ? rotation.floatValue() : 0f, "u_TextureRotation");
                
                setFeatureFlag("u_TextureTransformed", true);
            } else {
                setFeatureFlag("u_TextureTransformed", false);
            }
        } catch (Exception e) {
            setFeatureFlag("u_TextureTransformed", false);
        }
    }

    private void loadTexture(Texture texture) {
        if (texture == null || texture.hasId()) return;

        final int textureId;
        if (texture.getBitmap() != null && !texture.getBitmap().isRecycled()) {
            textureId = GLUtil.loadTexture(texture.getBitmap());
            texture.setId(textureId);
        } else if (texture.getData() != null) {
            textureId = GLUtil.loadTexture(texture.getData());
            texture.setId(textureId);
        } else if (texture.getBuffer() != null) {
            textureId = GLUtil.loadTexture(texture.getBuffer());
            texture.setId(textureId);
        } else {
            return;
        }

        textures.add(texture);
    }

    private void setTexture(Texture texture, String variableName, int textureIndex) {
        if (texture == null || !texture.hasId()) return;

        int handle = GLES30.glGetUniformLocation(mProgram, variableName);
        if (handle != -1) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureIndex);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture.getId());
            GLES30.glUniform1i(handle, textureIndex);
        }
    }

    private void setTextureCube(int textureId, String variableName, int textureIndex) {
        int handle = GLES30.glGetUniformLocation(mProgram, variableName);
        if (handle != -1) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + textureIndex);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, textureId);
            GLES30.glUniform1i(handle, textureIndex);
        }
    }

    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] transforms = animatedModel.getSkin().getJointTransforms();
        for (int i = 0; i < transforms.length; i++) {
            String name = jointCache.get(i);
            if (name == null) {
                name = "jointTransforms[" + i + "]";
                jointCache.put(i, name);
            }
            setUniformMatrix4(transforms[i], name);
        }
    }

    private void setUniformMatrix4(float[] matrix, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && matrix != null) GLES30.glUniformMatrix4fv(handle, 1, false, matrix, 0);
    }

    private void setUniform3(float[] vec, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && vec != null) GLES30.glUniform3fv(handle, 1, vec, 0);
    }

    private void setUniform2(float[] vec, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && vec != null) GLES30.glUniform2fv(handle, 1, vec, 0);
    }

    private void setUniform4(float[] vec, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && vec != null) GLES30.glUniform4fv(handle, 1, vec, 0);
    }

    private void setUniform1(float value, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1f(handle, value);
    }

    private void setUniformInt(int value, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1i(handle, value);
    }

    private void setFeatureFlag(String name, boolean enabled) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1i(handle, enabled ? 1 : 0);
    }

    @Override public void useProgram() { GLES30.glUseProgram(mProgram); }
    @Override public int getProgram() { return mProgram; }
    @Override public int getId() { return mProgram; }
    @Override public String getName() { return id; }
    @Override public void setAutoUseProgram(boolean auto) { this.autoUseProgram = auto; }

    @Override 
    public void reset() {
        for (Texture texture : textures) {
            if (texture.getId() != -1) {
                GLES30.glDeleteTextures(1, new int[]{texture.getId()}, 0);
                texture.setId(-1);
            }
        }
        textures.clear();
        gpuManager.clear();
    }

    @Override public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {}
}
