package org.the3deer.android.engine.shader;

import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.bean.BeanProperty;
import org.the3deer.android.engine.shader.v2.ShaderImplV2;
import org.the3deer.android.engine.shader.v3.ShaderImplV3;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * This class manages the shaders of the application
 */
@Bean(category = "general")
public class ShaderManager {

    private static final Logger logger = Logger.getLogger(ShaderManager.class.getSimpleName());

    // dependencies
    @Inject
    private Map<String, Program> programsMap;

    // properties
    @BeanProperty(values = {"2", "3"})
    private String openGLVersion = "3";

    // variables
    private int openGLVersion_ = 3;

    /**
     * OpenGL program -> shader
     */
    private Map<Integer, Shader> activeShaders;

    @BeanInit
    public void setUp() {

        // check there is shaders available
        if (programsMap == null || programsMap.isEmpty())
            throw new IllegalStateException("There is no shaders configured: " + programsMap);

        // update state
        refresh();
    }

    @BeanProperty
    public void setOpenGLVersion(String openGLVersion) {

        // check arguments
        if (openGLVersion == null)
            throw new IllegalArgumentException("OpenGL version can't be null");

        // check argument value
        if (!openGLVersion.equals("2") && !openGLVersion.equals("3"))
            throw new IllegalArgumentException("Invalid OpenGL version: " + openGLVersion);

        // set property
        int openGLVersionRequested = Integer.parseInt(openGLVersion);

        // update model
        this.openGLVersion_ = openGLVersionRequested;

        // log event
        logger.info("- OpenGL version: "+openGLVersion_);

        // update
        //refresh();
    }

    private void refresh() {
        //refreshPrograms();

        // INFO: lazy loading
        //refreshShaders();
    }


    /*private void refreshPrograms() {

        // prepare cache
        final Map<String, Program> shadersFound = new HashMap<>();

        // filter shaders for the specific opengl version
        for (Map.Entry<String, Program> entry : programsMap.entrySet()) {

            // get version
            final int version = entry.getValue().openGLVersion;

            // check version
            if (version != openGLVersion_) continue;


            // register shader
            shadersFound.put(entry.getKey(), entry.getValue());
        }

        // check
        if (programsMap == null || programsMap.isEmpty())
            throw new IllegalStateException("There is no resources loaded");

        // build shader
        logger.info("Instantiating Shader... shader: " + this.openGLVersion_);

        // loop
        for (Map.Entry<String, Program> entry : activePrograms.entrySet()) {

            // load
            loadShader(entry.getValue());
        }

        // debug
        logger.info("Shader loaded successfully");
    }*/

    private Shader loadShader(int programId) {

        // find program
        Program program = null;
        for (Map.Entry<String, Program> entry : programsMap.entrySet()) {
            if (entry.getValue().openGLVersion == openGLVersion_ && entry.getValue().id == programId) {
                program = entry.getValue();
                break;
            }
        }

        // check arguments
        if (program == null) throw new IllegalArgumentException("Program not found");

        // log event
        logger.info("Loading program... program: " + program.id+", glVersion: "+openGLVersion_);

        // build shader
        final Shader shader;
        switch (openGLVersion_) {
            case 3:
                shader = ShaderImplV3.getInstance(program);
                break;
            case 2:
                shader = ShaderImplV2.getInstance(String.valueOf(program.id), program.vertexShaderCode, program.fragmentShaderCode);
                break;
            default:
                throw new IllegalArgumentException("Invalid OpenGL version: " + program.openGLVersion);
        }

        // get specific shaders
        if (activeShaders == null) {
            activeShaders = new HashMap<>();
        }

        // register shader
        activeShaders.put(program.id, shader);

        return shader;
    }

    /**
     * Helper method. Guesses the right shader.
     * Main entry point for clients to get the appropriate shader.
     * Clients don't need to know which shader is to be used
     */
    public Shader getShader(int programId) {

        // init
        if (activeShaders == null) activeShaders = new HashMap<>();

        // get versioned shaders
        Shader shader = activeShaders.get(programId);

        // check
        if (shader != null) return shader;

        // load new shader
        shader = loadShader(programId);

        return shader;
    }

    /**
     * Free the shaders. They will be recreated the next time they are queried
     */
    public void reset() {

        // check
        if (this.activeShaders == null || this.activeShaders.isEmpty()) return;

        // log event
        logger.info("Resetting shaders... size: " + this.activeShaders.size());

        // free shaders
        for (Shader shader : this.activeShaders.values()){
            shader.reset();
        }

        // free variables
        this.activeShaders.clear();
        this.activeShaders = null;
    }

}
