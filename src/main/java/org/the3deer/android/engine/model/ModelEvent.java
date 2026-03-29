package org.the3deer.android.engine.model;

import java.util.EventObject;
import java.util.Map;

public class ModelEvent extends EventObject {

    public enum Code {
        LOADING,
        LOADED,
        PROGRESS,
        LOAD_ERROR,
        SCREEN_CHANGED
    }

    private final Code code;

    private final Map<String, Object> data;

    public ModelEvent(Object source, Code code) {
        this(source, code, null);
    }
    public ModelEvent(Object source, Code code, Map<String, Object> data) {
        super(source);
        this.code = code;
        this.data = data;
    }

    public Code getCode() {
        return code;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
