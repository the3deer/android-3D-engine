/**
 * Android-specific implementation of the 3D Engine.
 *
 * <p>This package contains classes that integrate the core engine with the Android framework,
 * specifically handling OpenGL ES lifecycle, surface management, and Android-specific resource loading.</p>
 *
 * <h2>Key Components</h2>
 * <ul>
 *     <li><b>SceneRenderer</b>: Implements <code>GLSurfaceView.Renderer</code> to orchestrate the rendering loop.</li>
 *     <li><b>CollisionController</b>: Provides ray-casting against the 3D scene, converting 2D screen coordinates to 3D space.</li>
 *     <li><b>Model Parsers</b>: Integration for GLTF (via JglTF) and FBX (via native <code>ufbx</code>).</li>
 * </ul>
 *
 * <h2>Rendering Pipeline</h2>
 * <p>The engine uses a recursive traversal of the Scene Graph. It supports:</p>
 * <ul>
 *     <li><b>Default Renderer</b>: Standard OpenGL ES rendering.</li>
 *     <li><b>Anaglyph Renderer</b>: Red/Cyan stereoscopic 3D.</li>
 *     <li><b>Stereo Renderer</b>: Side-by-side rendering for VR headsets.</li>
 * </ul>
 */
package org.the3deer.android.engine;