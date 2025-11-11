package org.the3deer.android_3d_model_engine.services.gltf;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.android_3d_model_engine.services.ModelComparator;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneData;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GltfLoaderTask extends LoaderTask {

    private static final String TAG = GltfLoaderTask.class.getSimpleName();

    public GltfLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws Exception {

        callback.onStart();

        if (Constants.STRATEGY_LOAD_NEW) {
            return buildNew();
        } else {
            return buildLegacy();
        }
    }

    @NonNull
    private List<Object3DData> buildNew() throws Exception {
        GltfLoader loader = new GltfLoader();
        GltfSceneData sceneData = loader.load(uri, callback);

        GltfSceneFactory factory = new GltfSceneFactory();
        List<Scene> scenes = factory.createScenes(sceneData);

        List<Object3DData> loadNew = new ArrayList<>();
        for (Scene scene : scenes) {
            loadNew.addAll(scene.getObjects());
            callback.onLoad(scene);
        }

        return loadNew;
    }

    protected List<Object3DData> buildLegacy() throws Exception {
        List<Object3DData> loadLegacy = new GltfLoaderLegacy().load(uri, callback);

        /*GltfLoader loader = new GltfLoader();
        GltfSceneData sceneData = loader.load(uri, callback);

        GltfSceneFactory factory = new GltfSceneFactory();
        List<Scene> scenes = factory.createScenes(sceneData);

        List<Object3DData> loadNew = new ArrayList<>();
        for (Scene scene : scenes) {
            loadNew.addAll(scene.getObjects());
        }

        if (loadLegacy.size() != loadNew.size()){
            Log.e("MODEL_COMPARE", "Returned different number of models, legacy "+loadLegacy.size()+" vs new "+loadNew.size());
        }
        ModelComparator modelComparator = new ModelComparator();
        for (int i = 0; i < loadLegacy.size() && i < loadNew.size(); i++) {
            modelComparator.compareModels(loadLegacy.get(i), loadNew.get(i));
            Log.v("MODEL_COMPARE", "--legacy--");
            loadLegacy.get(i).debug();
            Log.v("MODEL_COMPARE", "--new--");
            loadNew.get(i).debug();
        }*/

        return loadLegacy;
    }
}