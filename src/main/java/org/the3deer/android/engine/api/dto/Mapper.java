package org.the3deer.android.engine.api.dto;

import org.the3deer.android.engine.model.Material;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility to map internal domain entities to public API DTOs.
 */
public class Mapper {

    public static Scene fromDomain(org.the3deer.android.engine.model.Scene domain) {
        if (domain == null) return null;
        
        Scene aiScene = new Scene();
        List<Object3D> aiObjects = new ArrayList<>();
        for (org.the3deer.android.engine.model.Object3D obj : domain.getObjects()) {
            if (obj.isDecorator()) continue;
            aiObjects.add(fromDomain(obj));
        }
        aiScene.setObjects(aiObjects);
        
        if (domain.getSelectedObject() != null) {
            aiScene.setSelectedObject(domain.getSelectedObject().getId());
        }
        
        return aiScene;
    }

    public static Object3D fromDomain(org.the3deer.android.engine.model.Object3D domain) {
        if (domain == null) return null;

        Object3D aiObj = new Object3D();
        aiObj.setId(domain.getId());
        aiObj.setName(domain.getName());
        aiObj.setPosition(domain.getPosition());
        aiObj.setRotation(domain.getRotation());
        aiObj.setScale(domain.getScale());
        aiObj.setVisible(domain.isVisible());

        Material material = domain.getMaterial();
        if (material != null && material.getColor() != null) {
            float[] c = material.getColor();
            aiObj.setColor(String.format("#%02x%02x%02x", 
                (int)(c[0] * 255), (int)(c[1] * 255), (int)(c[2] * 255)));
        }

        return aiObj;
    }
}
