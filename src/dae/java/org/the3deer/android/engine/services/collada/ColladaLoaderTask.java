package org.the3deer.android.engine.services.collada;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.AnimatedModel;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.util.List;

public class ColladaLoaderTask extends LoaderTask {

    public ColladaLoaderTask(Model model, LoadListener callback) {
        super(model, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {

        callback.onLoadStart();

        // 1. Load the new model using your new parser
        final ColladaLoader loader = new ColladaLoader();
        final Scene scene = loader.load(uri);
        
        if (loader.getAuthor() != null && !loader.getAuthor().trim().isEmpty()) {
            if (model.getAuthor() == null || model.getAuthor().trim().isEmpty()) {
                model.setAuthor(loader.getAuthor().trim());
            }
        }
        if (loader.getCopyright() != null && !loader.getCopyright().trim().isEmpty()) {
            if (model.getLicense() == null || model.getLicense().trim().isEmpty()) {
                model.setLicense(loader.getCopyright().trim());
            }
        }
        if (loader.getComments() != null && !loader.getComments().trim().isEmpty()) {
            if (model.getDescription() == null || model.getDescription().trim().isEmpty()) {
                model.setDescription(loader.getComments().trim());
            }
        }

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

        callback.onLoadComplete();

        return loadNew;
    }

}
