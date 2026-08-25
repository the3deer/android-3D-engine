package org.the3deer.android.engine.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Scene {
    private List<Object3D> objects;
    private String selectedObject;

    public List<Object3D> getObjects() { return objects; }
    public void setObjects(List<Object3D> objects) { this.objects = objects; }

    public String getSelectedObject() { return selectedObject; }
    public void setSelectedObject(String selectedObject) { this.selectedObject = selectedObject; }
}
