package org.the3deer.android.engine.services.fbx;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FBX loader supported by ufbx <a href="https://github.com/ufbx/ufbx">ufbx</a>
 *
 * @author andresoviedo
 */

public final class FbxLoaderTask extends LoaderTask {

    private static final Logger logger = Logger.getLogger(FbxLoaderTask.class.getSimpleName());

    private FbxLoader loader = new FbxLoader();

    public FbxLoaderTask(Model model, LoadListener callback) {
        super(model, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {
        try {
            final List<Object3D> load = loader.load(uri, callback);

            if (loader.getCreator() != null && !loader.getCreator().trim().isEmpty()) {
                if (model.getGltf() == null || model.getGltf().trim().isEmpty()) {
                    model.setGltf(loader.getCreator().trim());
                }
            }
            if (loader.getAuthor() != null && !loader.getAuthor().trim().isEmpty()) {
                if (model.getAuthor() == null || model.getAuthor().trim().isEmpty()) {
                    model.setAuthor(loader.getAuthor().trim());
                }
            }
            if (loader.getLicense() != null && !loader.getLicense().trim().isEmpty()) {
                if (model.getLicense() == null || model.getLicense().trim().isEmpty()) {
                    model.setLicense(loader.getLicense().trim());
                }
            }
            if (loader.getDescription() != null && !loader.getDescription().trim().isEmpty()) {
                if (model.getDescription() == null || model.getDescription().trim().isEmpty()) {
                    model.setDescription(loader.getDescription().trim());
                }
            }

            final Scene sceneDefault = new Scene();
            sceneDefault.getObjects().addAll(load);
            return load;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
