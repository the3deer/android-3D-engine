# Android 3D Engine Documentation

The Engine is a modular OpenGL ES 2.0/3.0 library designed for Android. It uses a component-based architecture managed by a `BeanFactory`.

## Core Architecture

### BeanFactory
The Engine uses a `BeanFactory` to register, initialize, and manage the lifecycle of different components. This allows for a decoupled system where features can be enabled or disabled dynamically.

### Features
The engine is composed of several core feature modules:
- **Animator**: Handles skeletal and property animations.
- **Camera**: Manages view and projection matrices.
- **Collision**: Implements ray-casting and AABB collision detection.
- **Decorators**: Visual helpers like wireframes, skeletons, and bounding boxes.
- **Parsers**: Loaders for various 3D formats (OBJ, STL, DAE, GLTF, FBX).
- **Renderer**: Orchestrates the OpenGL drawing calls.
- **Shaders**: Manages GLSL programs and uniform injections.

## Scene Graph Model

The engine represents 3D data through a hierarchical Scene Graph:

- **Model**: The top-level container for a 3D asset.
- **Scene**: A specific 3D environment containing a hierarchy of Nodes.
- **Node**: The fundamental building block.
    - Each node has a `localMatrix` (translation, rotation, scale).
    - Nodes compute a `worldMatrix` recursively based on their parent.
    - Nodes can host a **Mesh** (`Object3D`) or a **Camera**.
- **Mesh (Object3D)**: Contains geometry data (Vertices, Normals, Colors, TexCoords, Indices).
- **Skin**: Holds joint hierarchies and inverse bind matrices for skeletal animation.

## Rendering Pipeline

### SceneRenderer
The `SceneRenderer` implements `GLSurfaceView.Renderer`. It performs a recursive traversal of the Scene Graph starting from the active Scene's root nodes.

### Default Renderer
Uses a collection of `Drawer` objects to render geometry. It handles:
- Animation updates.
- World matrix calculations.
- Shader selection and property binding.

### Stereoscopic Rendering
- **Anaglyph Renderer**: Specialized for red/cyan 3D glasses.
- **Stereo Renderer**: Side-by-side rendering for VR headsets.

## Model Parsers

### GLTF Integration
Maps GLTF Primitives to `Object3D` instances.
- **Attributes**: POSITION, NORMAL, TANGENT, TEXCOORD, COLOR.
- **Skinning**: Maps GLTF Skins to Engine Skins, unrolling weights and joints for shader compatibility.

### FBX Integration (C/Native Interface)
Uses `ufbx` via JNI. The Java layer communicates with the native C interface to retrieve:
- Mesh counts and metadata.
- Vertex, Normal, Index, and Color buffers directly from memory.

## Collision Detection

The `CollisionController` provides ray-casting against the 3D scene:
1. Converts 2D screen coordinates to 3D space (unprojecting).
2. Performs AABB (Axis-Aligned Bounding Box) checks for quick filtering.
3. For hits, performs precise triangle-level intersection tests using an Octree-optimized search.
4. Triggers events with the exact collision coordinates.
