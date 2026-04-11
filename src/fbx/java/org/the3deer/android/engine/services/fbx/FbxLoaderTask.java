package org.the3deer.android.engine.services.fbx;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.net.URI;
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

    public FbxLoaderTask(URI url, LoadListener callback) {
        super(url, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {
        try {
            final List<Object3D> load = loader.load(uri, callback);
            final Scene sceneDefault = new Scene();
            sceneDefault.getObjects().addAll(load);
            return load;
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
