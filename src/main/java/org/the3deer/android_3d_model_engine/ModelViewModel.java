package org.the3deer.android_3d_model_engine;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ModelViewModel extends ViewModel {

    private final MutableLiveData<ModelEngine> modelEngine = new MutableLiveData<>();

    public void setModelEngine(ModelEngine me) {
        modelEngine.setValue(me);
    }

    public LiveData<ModelEngine> getModelEngine() {
        return modelEngine;
    }
}