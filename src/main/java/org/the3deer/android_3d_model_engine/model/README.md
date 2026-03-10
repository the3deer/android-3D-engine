# Application Model

    - Scenes
        |- Scene
            |- Object3DData                     <list>
                |- Parent Node                  <pointer>
                |- Skin                         <pointer>
                    |- Root Joint
                    |- JointsIds
                    |- Inverse Bind Matrices
                    |- Weights
                    |- Joints
            |- Root Joints                      <list>
            |- Cameras                          <list>
            |- Animations                       <list>
            |- Skins                            <list>


# Transforms

    - Scene
        |- worldMatrix             <-- rescaling to 