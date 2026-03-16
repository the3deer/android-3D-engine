package org.the3deer.android_3d_model_engine;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModelViewModel extends ViewModel {

    private final MutableLiveData<Map<String,ModelEngine>> modelEngine = new MutableLiveData<>();
    private final MutableLiveData<String> recentUri = new MutableLiveData<>();

    public void setModelEngine(String uri, ModelEngine me) {
        final Map<String, ModelEngine> value;
        if (modelEngine.getValue() == null){
            value = new LinkedHashMap<>();
            modelEngine.setValue(value);
        } else {
            value = modelEngine.getValue();
        }
        value.put(uri, me);
    }

    public ModelEngine getModelEngine(String uri) {
        if (modelEngine.getValue() != null){
            return modelEngine.getValue().get(uri);
        }
        return null;
    }

    public void setRecentUri(String uri) {
        recentUri.setValue(uri);
    }

    public LiveData<String> getRecentUri() {
        return recentUri;
    }

    public ModelEngine getModelEngine() {
        return getModelEngine(getRecentUri().getValue());
    }
}