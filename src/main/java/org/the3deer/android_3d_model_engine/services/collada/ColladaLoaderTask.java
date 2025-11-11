package org.the3deer.android_3d_model_engine.services.collada;

import android.app.Activity;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.android_3d_model_engine.services.ModelComparator;

import java.net.URI;
import java.util.List;

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

        for (Object3DData obj : loadNew) {
            if (obj.getParentNode() != null && obj.getParentNode().getSkin() != null) {
                scene.addSkeleton(obj.getParentNode().getSkin());
            }
        }

        // COMPARE
        /*List<Object3DData> load = new ColladaLoaderLegacy().load(uri, null);
        for (int i=0; i<load.size(); i++) {
            ModelComparator.compareModels(load.get(i), loadNew.get(i));
        }*/

        // --- testing...
        scene.onLoadComplete();
        callback.onLoad(scene);

        return loadNew;
    }

}