Android 3D Engine
=================

This is a 3D OpenGL ES 2.0/3.0 engine

As this is my first android library and my first 3D engine and I'm still learning the OpenGL 2.0 language, it is highly probable that there are bugs;
however, I will try to continue improving the library and adding more features. 
So please send me your comments, suggestions or complains by opening an [issue](https://github.com/the3deer/android-3D-engine/issues).


Features
========

- [x] Supports >= Android 5.0 (LOLLIPOP) - Min API Level 21 -> Target API Level 34
- [x] OpenGL ES 2.0/3.0 API
- [x] 3D model parser:
    - [x] OBJ (wavefront)
    - [x] STL (STereoLithography)
    - [x] DAE (Collada-BETA)
    - [x] GLTF (GL Transmission Format)
- [x] Vertex Normals support
- [x] Transformation support: scaling, rotation, translation, orientation
- [x] Colors support
- [x] Textures support
- [x] Lighting support
- [x] Debuggers
    - [x] wireframe
    - [x] skeleton
- [x] skeletal animations (collada dae, gltf)
- [x] ray collision detection
- [x] other:
    - [x] Polygon with holes
    - [x] Bounding box
    - [x] Skybox
    - [x] Object picking
    - [x] file explorer
    - [x] repository explorer
    - [x] khronos explorer
    - [x] lightweight: only 8 Megabyte (3d models excluded)


Usage
=====

- Create your own Android application (eg. using Android Studio )
- Execute the following command to include this module/library

    $ cd your-3d-git-project
    $ git submodule add https://github.com/the3deer/android-3D-engine.git engine

- Add engine in settings.gradle

    include ':app'
    include ':engine'

- Add dependency in your app/build.gradle

    implementation project(':engine')


Dependants
==========

- android-3D-model-viewer: https://github.com/the3deer/android-3D-model-viewer
- android-3D-isogame: WIP
  

Documentation
=============

Not yet available.  You would need to check the android-3D-model-viewer application on how this engine is used. 


Licenses
========

The following copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.


    MIT License - Copyright (c) 2022 The 3Deer - https://github.com/the3deer
    GNU LGPL v2.1 Copyright (c) 2001, 2002 Dipl. Ing. P. Szawlowski - STL Parser
    MIT License - https://github.com/javagl/JglTF - GLTF Parser    
    ISC License - Earcut - https://github.com/the3deer/earcut

ChangeLog
=========

- 0.2.0 (22/10/2024)
  - Full Engine refactoring. Component based. MVC Architecture.
  - Full Android refactoring. Only 1 Activity and Fragments
  - BeanFactory: Bean Factory and Bean Manager utility
  - Updated GLTF parser with latest sources
  - Refactoring: Usage generic Buffer of dynamic type
  - Animation: Upgraded from vec3 to vec4 weights
  - Animator: Fixes for gltf
  - Shader: Normal mapping fixed
  - CameraController projections (DISABLED): rotations not working properly
  - Transformation rotation (DISABLED): Object rotation not working properly
  - Collada loader bug fixes
- 0.1.0 (18/10/2022)
  - Added support for ZIP files
- 0.0.1 (10/10/2022)
  - initial version imported from android-3D-model-viewer application

