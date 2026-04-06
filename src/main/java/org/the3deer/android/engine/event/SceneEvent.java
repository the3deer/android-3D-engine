package org.the3deer.android.engine.event;

import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

public class SceneEvent extends EventObject {

    public enum Code {
        SCENE_UPDATED,
        OBJECT_SELECTED,
        OBJECT_UNSELECTED,

        UNKNOWN
    }

    public final Code code;

    private Map<String, Object> data;

    public SceneEvent(Object source, Code code) {
        super(source);
        this.code = code;
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

    public SceneEvent setData(String id, Object value) {
        if (value == null) return this;
        if (data == null) {
            data = new HashMap<>();
        }
        data.put(id, value);
        return this;
    }

}
