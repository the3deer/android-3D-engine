package org.the3deer.android_3d_model_engine.services.collada;

import android.app.Activity;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.android_3d_model_engine.services.ModelComparator;

import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ColladaLoaderTask extends LoaderTask {

    public ColladaLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws Exception {

        if (Constants.STRATEGY_LOAD_NEW) {
            return buildNew();
        } else {
            return buildLegacy();
        }
    }

    private List<Object3DData> buildLegacy() {
        return new ColladaLoaderLegacy().load(uri, callback);
    }

    private List<Object3DData> buildNew() throws Exception {
        callback.onStart();
        // 1. Load the new model using your new parser
        final Scene scene = new ColladaLoader().load(uri);
        List<Object3DData> loadNew = scene.getObjects();
        Map<String, Object3DData> collectNew = loadNew.stream().collect(Collectors.toMap(Object3DData::getId, Function.identity()));

        for (Object3DData obj : loadNew) {
            /*if (obj.getParentNode() != null && obj.getParentNode().getSkin() != null) {
                scene.addSkeleton(obj.getParentNode().getSkin());
            }*/
            if (obj instanceof AnimatedModel) {
                if (((AnimatedModel) obj).getSkin() != null) {
                    scene.addSkeleton(((AnimatedModel) obj).getSkin());
                }
            }
        }

        if (Constants.DEBUG) {
            try {
                // COMPARE
                List<Object3DData> load = new ColladaLoaderLegacy().load(uri, null);

                Log.e("MODEL_COMPARE", "Comparing...");
                if (loadNew.size() == 1) {
                    ModelComparator.compareModels(load.get(0), loadNew.get(0));
                } else {

                    Map<String, Object3DData> collectLegacy = load.stream().collect(Collectors.toMap(Object3DData::getName, Function.identity()));

                    Collections.sort(loadNew, Comparator.comparing(Object3DData::getId));
                    Collections.sort(load, Comparator.comparing(Object3DData::getId));

                    for (Map.Entry<String, Object3DData> entry : collectNew.entrySet()) {
                        String key = entry.getKey();
                        Object3DData value = entry.getValue();
                        Object3DData legacyValue = collectLegacy.get(key);

                        Log.e("MODEL_COMPARE", "Comparing " + key + "...");
                        ModelComparator.compareModels(legacyValue, value);
                    }
                }
            } catch (Exception e) {
                Log.e("MODEL_COMPARE", "Comparison failed " + e.getMessage(), e);
            }
        }

        // invoke the handler
        callback.onLoad(scene);
        callback.onLoadComplete(scene);

        return loadNew;
    }

}