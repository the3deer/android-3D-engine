package org.the3deer.android.engine.model;

import java.util.EventObject;

public class ModelEvent extends EventObject {

    public enum Code { LOADED }

    private final Code code;

    public ModelEvent(Object source, Code code) {
        super(source);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
