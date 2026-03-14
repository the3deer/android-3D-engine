package org.the3deer.android_3d_model_engine.shader;


import org.the3deer.android_3d_model_engine.R;

public enum ShaderResource {

    SKYBOX("skybox", R.raw.shader_skybox_vert, R.raw.shader_skybox_frag),
    BASIC("basic", R.raw.shader_basic_vert_v2, R.raw.shader_basic_frag_v2),
    STATIC("static", R.raw.shader_static_vert_v2, R.raw.shader_static_frag_v2),
    ANIMATED("animated", R.raw.shader_animated_vert_v2, R.raw.shader_animated_frag_v2),
    SHADOW_MAP("shadow_map", R.raw.shader_v_depth_map_v2, R.raw.shader_f_depth_map_v2),
    SHADOW("shadow_map", R.raw.shader_v_with_shadow, R.raw.shader_f_with_simple_shadow);

    String id;
    int vertexShaderResourceId = -1;
    int fragmentShaderResourceId = -1;
    
    ShaderResource(String id, int vertexShaderCode, int fragmentShaderCode){
        this.id = id;
        this.vertexShaderResourceId = vertexShaderCode;
        this.fragmentShaderResourceId = fragmentShaderCode;
    }
}
