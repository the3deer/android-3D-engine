package org.the3deer.android.engine.event;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class Event extends EventObject {

    private Enum<?> code;

    private Map<String, Object> data;

    public Event(Object source) {
        this(source, null);
    }

    public Event(Object source, Enum<?> code) {
        super(source);
        this.code = code;
    }

    public Enum<?> getCode() {
        return code;
    }

    public Event setCode(Enum<?> code) {
        this.code = code;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public <T> T getData(String id, Class<T> clazz) {
        return getData(id, clazz, null);
    }

    public <T> T getData(String id, Class<T> clazz, T defaultValue) {
        if (getData() == null) return defaultValue;
        if (getData().get(id) == null) return defaultValue;
        return clazz.cast(getData().get(id));
    }

    public Event setData(String id, Object value) {
        if (value == null) return this;
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(id, value);
        return this;
    }
}