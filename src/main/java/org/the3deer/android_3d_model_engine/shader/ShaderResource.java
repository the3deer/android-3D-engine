package org.the3deer.android_3d_model_engine.shader;


import org.the3deer.android_3d_model_engine.R;

public enum ShaderResource {

    SKYBOX("skybox", R.raw.shader_v2_skybox_vert, R.raw.shader_v2_skybox_frag),
    BASIC("basic", R.raw.shader_v2_basic_vert, R.raw.shader_v2_basic_frag),
    STATIC("static", R.raw.shader_v2_static_vert, R.raw.shader_v2_static_frag),
    ANIMATED("animated", R.raw.shader_v2_animated_vert, R.raw.shader_v2_animated_frag),
    SHADOW_MAP("shadow_map", R.raw.shader_v2_shadow_depth_map_vert, R.raw.shader_v2_shadow_depth_map_frag),
    SHADOW("shadow", R.raw.shader_v2_shadow_vert, R.raw.shader_v2_shadow_frag);

    String id;
    int vertexShaderResourceId = -1;
    int fragmentShaderResourceId = -1;
    
    ShaderResource(String id, int vertexShaderCode, int fragmentShaderCode){
        this.id = id;
        this.vertexShaderResourceId = vertexShaderCode;
        this.fragmentShaderResourceId = fragmentShaderCode;
    }
}
