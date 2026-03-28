package org.the3deer.android.engine.services.gltf;

import android.net.Uri;

import androidx.annotation.NonNull;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;
import org.the3deer.android.engine.services.gltf.dto.GltfSceneData;

import java.util.ArrayList;
import java.util.List;

public class GltfLoaderTask extends LoaderTask {

    private static final String TAG = GltfLoaderTask.class.getSimpleName();

    public GltfLoaderTask(Uri uri, LoadListener callback) {
        super(uri, callback);
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