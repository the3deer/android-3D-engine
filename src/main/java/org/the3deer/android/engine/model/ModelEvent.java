package org.the3deer.android.engine.model;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class ModelEvent extends EventObject {

    public enum Code {
        STATUS_CHANGED,
        SCREEN_CHANGED
    }

    private final Code code;

    private Map<String, Object> data;

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

    public boolean hasData(String id) {
        if (data == null) return false;
        return data.containsKey(id);
    }

    public Map<String, Object> getData() {
        return data;
    }

    public <T> T getData(String id, Class<T> clazz) {
        return getData(id,clazz, null);
    }
    public <T> T getData(String id, Class<T> clazz, T defaultValue) {
        if (getData() == null) return defaultValue;
        if (getData().get(id) == null) return defaultValue;
        return clazz.cast(getData().get(id));
    }

    public ModelEvent setData(String id, Object value) {
        if (value == null) return this;
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(id, value);
        return this;
    }

}
