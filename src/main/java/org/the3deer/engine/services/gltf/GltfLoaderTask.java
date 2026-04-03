package org.the3deer.engine.services.gltf;

import androidx.annotation.NonNull;

import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.services.LoadListener;
import org.the3deer.engine.services.LoaderTask;
import org.the3deer.engine.services.gltf.dto.GltfSceneData;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GltfLoaderTask extends LoaderTask {

    public GltfLoaderTask(URI url, LoadListener callback) {
        super(url, callback);
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
        GltfSceneData sceneData = loader.load(uri, callback);
        return loader.createScenes(sceneData);
    }
}
