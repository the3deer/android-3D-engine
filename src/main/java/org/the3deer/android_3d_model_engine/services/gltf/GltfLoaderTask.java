package org.the3deer.android_3d_model_engine.services.gltf;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.android_3d_model_engine.services.ModelComparator;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneData;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GltfLoaderTask extends LoaderTask {

    private static final String TAG = GltfLoaderTask.class.getSimpleName();

    public GltfLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws Exception {

        callback.onStart();

        if (Constants.STRATEGY_LOAD_NEW) {

            List<Scene> scenes = buildNew();
            List<Object3DData> loadNew = new ArrayList<>();
            for (Scene scene : scenes) {
                loadNew.addAll(scene.getObjects());
            }

            if (Constants.DEBUG_COMPARE) {
                // legacy
                List<Scene> scenesLegacy = new GltfLoaderLegacy().load(uri, callback);
                if (scenes.size() != scenesLegacy.size()) {
                    Log.e("DEBUG_COMPARE", "Returned different number of scenes, legacy " + scenesLegacy.size() + " vs new " + scenes.size());
                } else {
                    for (int i = 0; i < scenesLegacy.size(); i++) {
                        ModelComparator modelComparator = new ModelComparator();
                        modelComparator.compareScenes(scenesLegacy.get(i), scenes.get(i));
                    }
                }


                List<Object3DData> loadLegacy = new ArrayList<>();
                for (Scene scene : scenesLegacy) {
                    loadLegacy.addAll(scene.getObjects());
                }
                if (loadLegacy.size() != loadNew.size()){
                    Log.e("DEBUG_COMPARE", "Returned different number of models, legacy "+loadLegacy.size()+" vs new "+loadNew.size());
                }

                for (int i = 0; i < loadLegacy.size() && i < loadNew.size(); i++) {
                    ModelComparator.compareModels(loadLegacy.get(i), loadNew.get(i));
                    Log.v("DEBUG_COMPARE", "--legacy--");
                    loadLegacy.get(i).debug();
                    Log.v("DEBUG_COMPARE", "--new--");
                    loadNew.get(i).debug();
                }
            }

            for (Scene scene : scenes) {
                callback.onLoad(scene);
                callback.onLoadComplete(scene);
            }

            return loadNew;
        } else {

            // legacy
            List<Scene> load = buildLegacy();
            List<Object3DData> loadLegacy = new ArrayList<>();
            for (Scene scene : load) {
                loadLegacy.addAll(scene.getObjects());
            }

            if (!Constants.DEBUG_COMPARE) {
                return loadLegacy;
            }

            // new
            GltfLoader loader = new GltfLoader();
            GltfSceneData sceneData = loader.load(uri, callback);
            List<Scene> scenesNew = loader.createScenes(sceneData);
            List<Object3DData> loadNew = new ArrayList<>();
            for (Scene scene : scenesNew) {
                loadNew.addAll(scene.getObjects());
            }

            if (scenesNew.size() != load.size()) {
                Log.e("DEBUG_COMPARE", "Returned different number of scenes, legacy " + load.size() + " vs new " + scenesNew.size());
            } else {
                for (int i = 0; i < load.size(); i++) {
                    ModelComparator modelComparator = new ModelComparator();
                    modelComparator.compareScenes(load.get(i), scenesNew.get(i));
                }
            }

            if (loadLegacy.size() != loadNew.size()){
                Log.e("DEBUG_COMPARE", "Returned different number of models, legacy "+loadLegacy.size()+" vs new "+loadNew.size());
            }

            ModelComparator modelComparator = new ModelComparator();
            for (int i = 0; i < loadLegacy.size() && i < loadNew.size(); i++) {
                modelComparator.compareModels(loadLegacy.get(i), loadNew.get(i));
                Log.v("DEBUG_COMPARE", "--legacy--");
                loadLegacy.get(i).debug();
                Log.v("DEBUG_COMPARE", "--new--");
                loadNew.get(i).debug();
            }
            return loadLegacy;
        }
    }

    @NonNull
    protected List<Scene> buildLegacy() throws Exception {
        return new GltfLoaderLegacy().load(uri, callback);
    }

    @NonNull
    private List<Scene> buildNew() throws Exception {
        GltfLoader loader = new GltfLoader();
        GltfSceneData sceneData = loader.load(uri, callback);
        return loader.createScenes(sceneData);
    }
}