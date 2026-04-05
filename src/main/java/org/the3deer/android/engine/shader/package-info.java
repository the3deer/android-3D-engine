/**
 * This package manages the shader resources available in the application
 * This manager mantains the shaders loaded in memory.
 * The shaders must be get only in the GLThread
 *
 * The model is the following
 * <pre>
 *  {
 *      ShaderManager {
 *          Shader {
 *              Program {
 *                  openGLVersion = 2
 *                  ShaderCode {
 *                      vertex shader code
 *                      fragment shader code
 *                  }
 *              }
 *          }
 *      }
 *  }
 * </pre>
 */
package org.the3deer.android.engine.shader;