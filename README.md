# Android 3D Engine

A lightweight, modular OpenGL ES 2.0/3.0 engine for Android. This library provides a component-based architecture to load and render 3D models with support for animations, lighting, and various file formats.

## Features

- **Compatibility**: Android 7.0+ (API 24+), targeting API 35.
- **Graphics API**: OpenGL ES 2.0 and 3.0.
- **Modular Architecture**: The engine is decoupled from specific file formats. You only compile and include the parsers you actually use.
- **Supported Formats (Optional Plugins)**:
    - **OBJ** (Wavefront)
    - **STL** (Stereolithography)
    - **DAE** (Collada) - Support for skinning and animations.
    - **GLTF** (GL Transmission Format) - PBR-ready structures, skinning, and animations.
    - **FBX** (Filmbox) - High-performance native parsing via `ufbx`.
- **Core Capabilities**:
    - Skeletal and Property Animations.
    - Directional and Point Lighting.
    - Normal Mapping.
    - Skybox support.
    - Collision Detection (Ray-casting & Octree optimized).
    - Stereoscopic/VR Rendering (Anaglyph & Side-by-Side).

## Modular Configuration

To keep your APK small and build times fast, you can enable only the loaders you need in your `gradle.properties` file:

```properties
# Enable specific 3D format plugins
includeObj=true
includeStl=true
includeGltf=true
includeDae=true
includeFbx=false  # Set to false to exclude FBX and its native C++ code
```

## Documentation

Detailed technical documentation regarding the engine's architecture, Scene Graph, and API can be found here:
**[doc/README.md](./doc/README.md)**

## Usage

### 1. Add as a Submodule
```bash
git submodule add https://github.com/the3deer/android-3D-engine.git engine
```

### 2. Configure Gradle
Add the module to your `settings.gradle`:
```gradle
include ':engine'
```

And add the dependency to your `app/build.gradle`:
```gradle
dependencies {
    implementation project(':engine')
}
```

### 2. Register Loaders
In your application initialization (e.g., `MainActivity` or `Application` class), register the plugins you want to use:

```kotlin
// Register only the formats your game uses
LoaderRegistry.register("obj") { uri, listener -> WavefrontLoaderTask(uri, listener) }
LoaderRegistry.register("gltf") { uri, listener -> GltfLoaderTask(uri, listener) }
LoaderRegistry.register("glb") { uri, listener -> GltfLoaderTask(uri, listener) }
```

## Documentation

Detailed technical documentation regarding the engine's architecture, Scene Graph, and API can be found here:
**[doc/README.md](./doc/README.md)**

## Credits & License

- **The3Deer**: MIT License
- **STL Parser**: GNU LGPL v2.1 (Dipl. Ing. P. Szawlowski)
- **GLTF Parser**: MIT License (javagl/JglTF)
- **EarCut**: ISC License (Mapbox)

---
*Developed with assistance from Gemini AI.*
