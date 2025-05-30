package org.the3deer.android_3d_model_engine.shader;


import org.the3deer.android_3d_model_engine.R;

public enum ShaderResource {

    SKYBOX("skybox", R.raw.shader_skybox_vert, R.raw.shader_skybox_frag),
    BASIC("basic", R.raw.shader_basic_vert, R.raw.shader_basic_frag),
    ANIMATED("animated", R.raw.shader_animated_vert, R.raw.shader_animated_frag);

    String id;
    int vertexShaderResourceId = -1;
    int fragmentShaderResourceId = -1;
    
    ShaderResource(String id, int vertexShaderCode, int fragmentShaderCode){
        this.id = id;
        this.vertexShaderResourceId = vertexShaderCode;
        this.fragmentShaderResourceId = fragmentShaderCode;
    }
}
