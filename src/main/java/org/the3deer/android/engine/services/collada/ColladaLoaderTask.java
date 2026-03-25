package org.the3deer.android.engine.services.collada;

import android.app.Activity;
import android.net.Uri;

import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ColladaLoaderTask extends LoaderTask {

    public ColladaLoaderTask(Activity parent, Uri uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {

        callback.onStart();

        // 1. Load the new model using your new parser
        final Scene scene = new ColladaLoader().load(uri);
        List<Object3D> loadNew = scene.getObjects();
        Map<String, Object3D> collectNew = loadNew.stream().collect(Collectors.toMap(Object3D::getId, Function.identity()));

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
        callback.onLoad(scene);
        callback.onLoadComplete(scene);

        callback.onLoadComplete();

        return loadNew;
    }

}