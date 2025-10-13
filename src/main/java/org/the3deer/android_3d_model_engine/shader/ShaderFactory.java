package org.the3deer.android_3d_model_engine.shader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.io.IOUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

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
public class ShaderFactory {

    @Inject
    private Context context;
    /**
     * shader code loaded from raw resources
     * resources are cached on activity thread
     */
    private Map<Integer, String> shadersCode = new HashMap<>();
    /**
     * shader names loaded from raw resources
     */
    private Map<Integer, String> shadersNames = new HashMap<>();
    /**
     * list of cached shaders
     */
    private Map<String, Shader> shaders = new HashMap<>();

    /**
     * Read all shader data from /raw folder (context required for IO).
     * if there is any issue accessing the date a log is generated.
     */
    @BeanInit
    public void setUp() {
        if (context == null) {
            throw new IllegalStateException("Context is null");
        };

        Log.d("ShaderFactory", "Discovering shaders...");
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            String shaderId = field.getName();
            try {
                Log.v("ShaderFactory", "Loading shader... " + shaderId);
                int shaderResId = field.getInt(field);
                byte[] shaderBytes = IOUtils.read(context.getResources().openRawResource(shaderResId));
                String shaderCode = new String(shaderBytes);
                shadersCode.put(shaderResId, shaderCode);
                shadersNames.put(shaderResId, shaderId);
            } catch (Exception e) {
                Log.e("ShaderFactory", "Issue loading shader... " + shaderId);
            }
        }
        Log.d("ShaderFactory", "Shaders loaded: " + shadersCode.size());
    }

    public void reset(){
        for (Shader shader : shaders.values()){
            shader.reset();
        }
        shaders.clear();
    }

    /**
     * Load (requires having a GL Context) or get the shaders loaded in GPU.
     *
     * @param obj
     * @param usingSkyBox
     * @param usingTextures
     * @param usingLights
     * @param usingAnimation
     * @param isShadow
     * @param isUsingShadows
     * @return
     */
    public Shader getShader(Scene scene, Object3DData obj, boolean usingSkyBox,
                            boolean usingTextures, boolean usingLights,
                            boolean usingAnimation, boolean isShadow, boolean isUsingShadows) {

        // double check features
        final boolean animationOK = obj instanceof AnimatedModel
                && scene.getCurrentAnimation() != null
                && scene.getCurrentAnimation().isInitialized();
        final boolean isAnimated = usingAnimation && animationOK;
        final boolean isLighted = usingLights && obj != null && obj.getNormalsBuffer() != null;
        final boolean isTextured = usingTextures && obj != null && obj.getTextureBuffer() != null;

        // match shaders
        final ShaderResource shader;
        if (usingSkyBox){
            shader = ShaderResource.SKYBOX;
        } else {
            shader = getShader(isTextured, isLighted, isAnimated, isShadow, isUsingShadows);
        }

        // get cached shaders
        Shader renderer = shaders.get(shader.id);
        if (renderer != null) {
/*
            renderer.setTexturesEnabled(isTextured);
            renderer.setLightingEnabled(isLighted);
            renderer.setAnimationEnabled(isAnimated);
*/
            return renderer;
        }

        // load shader
        renderer = loadShader(shader.id, shader.vertexShaderResourceId, shader.fragmentShaderResourceId, isTextured, isLighted, isAnimated);

        // cache drawer
        shaders.put(shader.id, renderer);

        Log.d("ShaderFactory", "Loaded "+ shader.id+" size ("+shaders.size()+") this: "+this);

        // return drawer
        return renderer;
    }

    @NonNull
    private ShaderImpl loadShader(String shaderId, int vertexShaderResourceId, int fragmentShaderResourceId, boolean isTextured, boolean isLighted, boolean isAnimated) {
        ShaderImpl renderer;
        // build drawer
        String vertexShaderCode;

        // experimental: inject glPointSize
        vertexShaderCode = shadersCode.get(vertexShaderResourceId).replace("void main(){", "void main(){\n\tgl_PointSize = 5.0;");

        // use opengl constant to dynamically set up array size in shaders. That should be >=120
        vertexShaderCode = vertexShaderCode.replace("const int MAX_JOINTS = 60;", "const int MAX_JOINTS = gl_MaxVertexUniformVectors > 60 ? 60 : gl_MaxVertexUniformVectors;");

        // create drawer
        /*Log.v("RendererFactory", "\n---------- Vertex shader ----------\n");
        Log.v("RendererFactory", vertexShaderCode);
        Log.v("RendererFactory", "---------- Fragment shader ----------\n");
        Log.v("RendererFactory", fragmentShaderCode);
        Log.v("RendererFactory", "-------------------------------------\n");*/
        renderer = ShaderImpl.getInstance(shaderId, vertexShaderCode, shadersCode.get(fragmentShaderResourceId));
        /*renderer.setTexturesEnabled(isTextured);
        renderer.setLightingEnabled(isLighted);
        renderer.setAnimationEnabled(isAnimated);*/

        return renderer;
    }

    /**
     * Return the shader loaded in GPU.
     * @param resIdVertexShader the shader resource id
     * @return
     */
    public Shader getShader(int resIdVertexShader, int resIdFragmentShader){
        final String shaderName = shadersNames.get(resIdVertexShader);
        final Shader shader = shaders.get(shaderName);
        if (shader == null){
            Log.d("ShaderFactory", "Loading shader... "+ shaderName);
            final ShaderImpl impl = loadShader(shaderName, resIdVertexShader, resIdFragmentShader, true, true, true);
            shaders.put(shaderName, impl);
            Log.d("ShaderFactory", "Loaded "+ shaderName+" size ("+shaders.size()+") this: "+this);
            return impl;
        }
        return shader;
    }

    @NonNull
    private ShaderResource getShader(boolean isTextured, boolean isLighted, boolean isAnimated, boolean isShadow, boolean isUsingShadows) {

        final ShaderResource ret;
        if (isAnimated || isTextured || isLighted){
            ret = ShaderResource.ANIMATED;
        } else {
            ret = ShaderResource.BASIC;
        }
        return ret;
    }

    public Map<String, Shader> getShaders() {
        return shaders;
    }

    public void setContext(Context parent) {
        this.context = parent;
    }
}
