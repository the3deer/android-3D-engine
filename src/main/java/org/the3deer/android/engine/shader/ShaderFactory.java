package org.the3deer.android.engine.shader;

import android.content.Context;

import org.the3deer.bean.BeanFactory;
import org.the3deer.android.engine.R;
import org.the3deer.util.io.IOUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

public class ShaderFactory {

    private static final Logger logger = Logger.getLogger(ShaderFactory.class.getSimpleName());

    private enum AndroidShaderResource {
        SKYBOX_v2("shader.v2.skybox", Program.SKYBOX, 2, R.raw.shader_v2_skybox_vert, R.raw.shader_v2_skybox_frag),
        SKYBOX_v3("shader.v3.skybox", Program.SKYBOX, 3, R.raw.shader_v3_skybox_vert, R.raw.shader_v3_skybox_frag),
        BASIC_v2("shader.v2.basic", Program.BASIC, 2, R.raw.shader_v2_basic_vert, R.raw.shader_v2_basic_frag),
        BASIC_v3("shader.v3.basic", Program.BASIC, 3, R.raw.shader_v3_basic_vert, R.raw.shader_v3_basic_frag),
        SUN_v3("shader.v3.sun", Program.SUN, 3, R.raw.shader_v3_sun_vert, R.raw.shader_v3_sun_frag),
        STATIC_v2("shader.v2.static", Program.STATIC, 2, R.raw.shader_v2_static_vert, R.raw.shader_v2_static_frag),
        STATIC_v3("shader.v3.static", Program.STATIC, 3, R.raw.shader_v3_static_vert, R.raw.shader_v3_static_frag),
        ANIMATED_v2("shader.v2.animated", Program.ANIMATED, 2, R.raw.shader_v2_animated_vert, R.raw.shader_v2_animated_frag),
        ANIMATED_v3("shader.v3.animated", Program.ANIMATED, 3, R.raw.shader_v3_animated_vert, R.raw.shader_v3_animated_frag),
        SHADOW_MAP_v2("shader.v2.shadow_map", Program.SHADOW_MAP, 2, R.raw.shader_v2_shadow_depth_map_vert, R.raw.shader_v2_shadow_depth_map_frag),
        // FIXME: upgrade to OpenGL 3
        SHADOW_MAP_v3("shader.v3.shadow_map", Program.SHADOW_MAP, 3, R.raw.shader_v2_shadow_depth_map_vert, R.raw.shader_v3_shadow_depth_map_frag),
        SHADOW_v2("shader.v2.shadow",  Program.SHADOW, 2, R.raw.shader_v2_shadow_vert, R.raw.shader_v2_shadow_frag),
        // FIXME: upgrade to OpenGL 3
        SHADOW_v3("shader.v3.shadow",  Program.SHADOW, 3, R.raw.shader_v2_shadow_vert, R.raw.shader_v2_shadow_frag);

        final String beanId;
        final int programId;
        final int openGLVersion;
        final int vertexShaderResourceId;
        final int fragmentShaderResourceId;

        AndroidShaderResource(String beanId, int programId, int openGLVersion, int vertexShaderResourceId, int fragmentShaderResourceId) {
            this.beanId = beanId;
            this.programId = programId;
            this.openGLVersion = openGLVersion;
            this.vertexShaderResourceId = vertexShaderResourceId;
            this.fragmentShaderResourceId = fragmentShaderResourceId;
        }
    }

    @Inject
    private Context context;

    @BeanFactory
    private Map<String, Program> buildPrograms() {

        // check
        if (context == null) throw new IllegalStateException("Context is null");

        // log event
        logger.info("Loading " + AndroidShaderResource.values().length + " shaders...");

        // prepare
        final Map<String, Program> programs = new HashMap<>();

        // load data
        for (AndroidShaderResource ar : AndroidShaderResource.values()) {
            try {

                final String vertexShaderCode = new String(IOUtils.read(context.getResources().openRawResource(ar.vertexShaderResourceId)));
                final String fragmentShaderCode = new String(IOUtils.read(context.getResources().openRawResource(ar.fragmentShaderResourceId)));

                final Program program = new Program(ar.programId, ar.openGLVersion, vertexShaderCode, fragmentShaderCode);
                programs.put(ar.beanId, program);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load shader resource. bean: " + ar.beanId+", program: "+ar.programId, e);
            }
        }

        return programs;
    }
}
