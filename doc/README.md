# Application

The Engine is an aggregate of different features.
The Engine uses a BeanFactory to register and initialize the different components


# Features

- Animator
- Camera
- Collision
- Decorators
- User Interface
- Preferences
- Parsers
- Renderer
- Shaders

# Components


* Application Model - MVC
*
*   ModelViewModel         (Model)
*   ModelController        (Controller)
*   ModelEngine            (Model)
*   GLSurfaceView          (View)
*   GLTouchController      (Controller)
*   TouchController        (Controller)
*   GLSurfaceView          (View)


## OpenGL customisation

3 buttons added: scene selection, camera selection, animation selection 

changes:
1.
setZOrderMediaOverlay(true): In HomeFragment.kt, I've added this call to the glSurfaceView. This explicitly tells Android to place the surface "behind" the window's normal View hierarchy.
2.
android:elevation="10dp": In fragment_home.xml, I've added elevation to the LinearLayout overlay. This provides a hint to the renderer to keep these views on top.

## CDI Manager

[CDI Manager](../src/main/java/org/the3deer/util/bean/README.md)

## Documentation

[Model](../src/main/java/org/the3deer/android/engine/model/README.md)
[Renderer](../src/main/java/org/the3deer/android/engine/renderer/README.md)
[Parsers](../src/main/java/org/the3deer/android/engine/services/gltf/README.md)
[Collision](../src/main/java/org/the3deer/android/engine/collision/README.md)

