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

# Model

/**
* Application Model - MVC
*
* <pre>
*    Activity
* </pre>
*
* <pre>
*   ModelViewModel         (Model)
*   ModelFragment          (View)                  --> GLSurface
*   PreferenceFragment     (View-Controller)
*   ModelController        (Controller)
* </pre>
*
* MModelViewModel:
*
* <pre>
*   ModelEngine            (Model)
*   GLSurfaceView          (View)
*   GLTouchController      (Controller)
*   TouchController        (Controller)
* </pre>
*
* Temporary Beans
*
* <pre>
*   GLSurfaceView          (View)
* </pre>
*
*/

## OpenGL customisation

3 buttons added: scene selection, camera selection, animation selection 

changes:
1.
setZOrderMediaOverlay(true): In HomeFragment.kt, I've added this call to the glSurfaceView. This explicitly tells Android to place the surface "behind" the window's normal View hierarchy.
2.
android:elevation="10dp": In fragment_home.xml, I've added elevation to the LinearLayout overlay. This provides a hint to the renderer to keep these views on top.


