package org.the3deer.android.engine.objects;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.bean.Bean;

/**
 * Manager for 3D primitive objects.
 */
@Bean
public class PrimitiveManager {

    public Object3D build(String type) {
        if ("cube".equalsIgnoreCase(type)) {
            return Cube.build();
        } else if ("pyramid".equalsIgnoreCase(type)) {
            return Pyramid.build();
        } else if ("sphere".equalsIgnoreCase(type)) {
            return Sphere.build();
        } else if ("cylinder".equalsIgnoreCase(type)) {
            return Cylinder.build();
        } else if ("plane".equalsIgnoreCase(type)) {
            return Plane.build();
        }
        return null;
    }
}
