# Collision Controller

The collision controller is a class that encapsulates all the logic for the collision detection algorithm.
The algorithm is based on the AABB algorithm.

    1. The x,y 2D screen coordinates are converted into 3D space (gluUnProject)
    2. The object's current matrix is inverted to get it aligned (AABB)    
    3. The ray vector, is then converted to model's inverted matrix, to keep coherence
    4. The ray is casted into the BoundingBox of the object, that is the bbox of the raw model vertices with no transformation
    5. If hit:
        1. An Octree for the object's BoundingBox is created
        2. The ray is then casted against each object triangle and if hit, the final intersction point is calculated
        3. An Event is trigged with the collision point
        4. The current Scene implementation draws a point at the collision point




