package org.the3deer.android.engine.services.collada;

import android.net.Uri;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.util.List;

public class ColladaLoaderTask extends LoaderTask {

    public ColladaLoaderTask(Uri uri, LoadListener callback) {
        super(uri, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {

        callback.onLoadStart();

        // 1. Load the new model using your new parser
        final Scene scene = new ColladaLoader().load(uri);
        List<Object3D> loadNew = scene.getObjects();
        //Map<String, Object3D> collectNew = loadNew.stream().collect(Collectors.toMap(Object3D::getId, Function.identity()));

        for (Object3D obj : loadNew) {
            /*if (obj.getParentNode() != null && obj.getParentNode().getSkin() != null) {
                scene.addSkeleton(obj.getParentNode().getSkin());
            }*/
            if (obj instanceof AnimatedModel) {
                if (((AnimatedModel) obj).getSkin() != null) {
                    scene.getSkins().add(((AnimatedModel) obj).getSkin());
                }
            }
        }

        // invoke the handler
        callback.onLoadScene(scene);

        return loadNew;
    }

}