# C interface (FBX)

    Model (.fbx)
        |- mesh
        |   |-  primitives
        |   |       |- vertices
        |   |       |- normals
        |   |       |- indices
        |   |       |- colors

    FBXModel                                                        <java Class>
        # String native getName()
        # String native getCreator()             
        # int native getVersion()
        # int native getMeshCount()
        # FBXMesh native getMesh(int index)
        # List<FBXMesh> native getMeshes()

    FBXMesh                                                         <java Class>
        # Buffer native getVertexBuffer(int handler, int mesh)
        # Buffer native getNormalsBuffer(int handler, int mesh)
        # Buffer native getIndexBuffer(int handler, int mesh)
        # Buffer native getColorsBuffer(int handler, int mesh)
        # Buffer native getTexCoordBuffer(int handler, int mesh)




