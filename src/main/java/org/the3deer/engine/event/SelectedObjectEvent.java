package org.the3deer.engine.event;

import org.the3deer.engine.model.Object3D;

import java.util.EventObject;

public class SelectedObjectEvent extends EventObject {

    private final Object3D selected;

    public SelectedObjectEvent(Object source, Object3D selected) {
        super(source);
        this.selected = selected;
    }

    public Object3D getSelected() {
        return selected;
    }
}
