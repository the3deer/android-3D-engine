/**
 * Core classes for the Android 3D Engine.
 *
 * <p>A lightweight, modular OpenGL ES 2.0/3.0 engine for Android. This library provides a component-based architecture
 * to load and render 3D models with support for animations, lighting, and various file formats.</p>
 *
 * <h2>Core Capabilities</h2>
 * <ul>
 *     <li>Skeletal and Property Animations</li>
 *     <li>Directional and Point Lighting</li>
 *     <li>Normal Mapping</li>
 *     <li>Skybox support</li>
 *     <li>Collision Detection (Ray-casting &amp; Octree optimized)</li>
 *     <li>Stereoscopic/VR Rendering (Anaglyph &amp; Side-by-Side)</li>
 * </ul>
 *
 * <h2>Supported Formats</h2>
 * <ul>
 *     <li><b>OBJ</b> (Wavefront)</li>
 *     <li><b>STL</b> (Stereolithography)</li>
 *     <li><b>DAE</b> (Collada) - Full support for skinning and animations</li>
 *     <li><b>GLTF</b> (GL Transmission Format) - PBR-ready structures, skinning, and animations</li>
 *     <li><b>FBX</b> (Filmbox) - High-performance native parsing via <code>ufbx</code></li>
 * </ul>
 *
 *  * <p>This package and its subpackages bridge the core engine with the Android platform,
 *  * providing implementations for OpenGL ES rendering, Android touch event handling,
 *  * and resource management using Android Assets and Resources.</p>
 *  *
 *  * <h2>Key Components</h2>
 *  * <ul>
 *  *     <li><b>SceneRenderer</b>: Implements <code>GLSurfaceView.Renderer</code> to orchestrate the rendering loop.</li>
 *  *     <li><b>CollisionController</b>: Provides ray-casting against the 3D scene, converting 2D screen coordinates to 3D space.</li>
 *  *     <li><b>Model Parsers</b>: Integration for GLTF (via JglTF) and FBX (via native <code>ufbx</code>).</li>
 *  * </ul>
 *  *
 *  * <h2>Rendering Pipeline</h2>
 *  * <p>The engine uses a recursive traversal of the Scene Graph. It supports:</p>
 *  * <ul>
 *  *     <li><b>Default Renderer</b>: Standard OpenGL ES rendering.</li>
 *  *     <li><b>Anaglyph Renderer</b>: Red/Cyan stereoscopic 3D.</li>
 *  *     <li><b>Stereo Renderer</b>: Side-by-side rendering for VR headsets.</li>
 *  * </ul>
 *
 * @see <a href="https://github.com/the3deer/android-3D-engine">GitHub Repository</a>
 */
package org.the3deer.android.engine;