package org.the3deer.android_3d_model_engine;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelViewModel extends ViewModel {

    private final MutableLiveData<Map<String,ModelEngine>> modelEngine = new MutableLiveData<>();
    private final MutableLiveData<String> recentId = new MutableLiveData<>();

    public void setModelEngine(String id, ModelEngine me) {
        final Map<String, ModelEngine> value;
        if (modelEngine.getValue() == null){
            value = new LinkedHashMap<>();
            modelEngine.setValue(value);
        } else {
            value = modelEngine.getValue();
        }
        value.put(id, me);
    }

    public ModelEngine getModelEngine(String id) {
        if (modelEngine.getValue() != null){
            return modelEngine.getValue().get(id);
        }
        return null;
    }

    public void setRecentId(String id) {
        recentId.setValue(id);
    }

    public LiveData<String> getRecentId() {
        return recentId;
    }

    public ModelEngine getModelEngine() {
        return getModelEngine(getRecentId().getValue());
    }
}