package org.the3deer.android.engine.services.gltf;

import androidx.annotation.NonNull;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;
import org.the3deer.android.engine.services.gltf.dto.GltfSceneData;

import java.util.ArrayList;
import java.util.List;

public class GltfLoaderTask extends LoaderTask {

    public GltfLoaderTask(Model model, LoadListener callback) {
        super(model, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {

        callback.onLoadStart();

        List<Scene> scenes = buildNew();

        List<Object3D> loadNew = new ArrayList<>();
        for (Scene scene : scenes) {
            loadNew.addAll(scene.getObjects());
        }

        for (Scene scene : scenes) {
            callback.onLoadScene(scene);
        }

        //callback.onLoadComplete();

        return loadNew;
    }

    @NonNull
    private List<Scene> buildNew() throws Exception {
        GltfLoader loader = new GltfLoader();
        GltfSceneData sceneData = loader.load(model, callback);
        return loader.createScenes(sceneData);
    }
}
