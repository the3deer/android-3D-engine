# Application Model

    - Scenes
        |- Scene
            |- Object3DData                     <list>
                |- Parent Node                  <pointer>
                |   |- Material
                |- Skin                         <pointer>
                |   |- Root Joint
                |   |- JointsIds
                |   |- Inverse Bind Matrices
                |   |- Weights
                |   |- Joints
                |- Material
                |   |- Color
                |   |   |- diffuse
                |   |   |- alpha
                |   |- Texture
            |- Root Joints                      <list>
            |- Cameras                          <list>
            |- Animations                       <list>
            |- Skins                            <list>


# Transforms

    - Scene
        |- worldMatrix             <-- rescaling to 