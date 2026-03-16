package org.the3deer.android_3d_model_engine.shader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.shader.v2.ShaderImplV2;
import org.the3deer.android_3d_model_engine.shader.v3.ShaderImplV3;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.io.IOUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

/**
 * The ShaderFactory Encapsulates version selection (V2 vs V3) and shader resource management.
 * Copyright 2013-2026 the3deer.org
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
 *
 * @author andresoviedo
 * @author Gemini AI
 */
public class ShaderFactory {

    private static final String TAG = "ShaderFactory";

    @Inject
    private Context context;

    private final Map<Integer, String> shadersCode = new HashMap<>();
    private final Map<String, Shader> shadersCache = new HashMap<>();

    @BeanInit
    public void setUp() {
        if (context == null) throw new IllegalStateException("Context is null");

        Log.d(TAG, "Discovering shaders...");
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                int resId = field.getInt(field);
                String code = new String(IOUtils.read(context.getResources().openRawResource(resId)));
                shadersCode.put(resId, code);
            } catch (Exception e) {
                Log.e(TAG, "Failed to load shader resource: " + field.getName());
            }
        }
    }

    /**
     * Main entry point for clients to get the appropriate shader.
     * Clients don't need to know which GLSL files are being used.
     */
    public Shader getShader(Object3DData obj) {
        // Determine the shader type based on model features
        boolean isAnimated = obj instanceof AnimatedModel && ((AnimatedModel) obj).getSkin() != null;
        boolean isPoints = obj.getDrawMode() == android.opengl.GLES20.GL_POINTS;

        // Map to the correct resource IDs based on version and type
        int vertResId, fragResId;
        String typeId;

        if (Constants.ANIMATIONS_ENABLED && isAnimated) {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_animated";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_animated_vert : R.raw.shader_v2_animated_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_animated_frag : R.raw.shader_v2_animated_frag;
        } else if (Constants.LIGHTING_ENABLED && !isPoints) {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_static";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_static_vert : R.raw.shader_v2_static_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_static_frag : R.raw.shader_v2_static_frag;
        } else {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_basic";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_basic_vert : R.raw.shader_v2_basic_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_basic_frag : R.raw.shader_v2_basic_frag;
        }

        // Check cache
        String cacheKey = typeId;
        Shader shader = shadersCache.get(cacheKey);
        if (shader != null) return shader;

        // Instantiate the right implementation
        Log.i(TAG, "Instantiating Shader: " + cacheKey);
        String vertCode = shadersCode.get(vertResId);
        String fragCode = shadersCode.get(fragResId);

        if (Constants.DEFAULT_SHADER_VERSION == 3) {
            shader = ShaderImplV3.getInstance(cacheKey, vertCode, fragCode);
        } else {
            shader = ShaderImplV2.getInstance(cacheKey, vertCode, fragCode);
        }

        shadersCache.put(cacheKey, shader);
        return shader;
    }

    public Shader getShader(ShaderResource shaderResource) {
        return getShader(shaderResource.id, shaderResource.vertexShaderResourceId, shaderResource.fragmentShaderResourceId);
    }

    /**
     * Return the shader loaded in GPU.
     * @param resIdVertexShader the shader resource id
     * @return
     */
    private Shader getShader(String id, int resIdVertexShader, int resIdFragmentShader){
        //final String shaderName = shadersCode.get(resIdVertexShader);
        final Shader shader = shadersCache.get(id);
        if (shader == null){
            Log.d("ShaderFactory", "Loading shader... "+ id);
            final Shader impl = loadShader(id, resIdVertexShader, resIdFragmentShader);
            shadersCache.put(id, impl);
            Log.d("ShaderFactory", "Loaded "+ id);
            return impl;
        }
        return shader;
    }

    @NonNull
    private Shader loadShader(String shaderId, int vertexShaderResourceId, int fragmentShaderResourceId) {
        if (Constants.DEFAULT_SHADER_VERSION == 3){
            return ShaderImplV3.getInstance(shaderId, shadersCode.get(vertexShaderResourceId), shadersCode.get(fragmentShaderResourceId));
        } else if (Constants.DEFAULT_SHADER_VERSION == 2) {
            return ShaderImplV2.getInstance(shaderId, shadersCode.get(vertexShaderResourceId), shadersCode.get(fragmentShaderResourceId));
        } else {
            throw new IllegalArgumentException("Unsupported shader version: "+Constants.DEFAULT_SHADER_VERSION);
        }
    }

    public void reset() {
        for (Shader shader : shadersCache.values()) shader.reset();
        shadersCache.clear();
    }

    @Deprecated
    public Map<String, Shader> getShaders() {
        return Collections.emptyMap();
    }
}
