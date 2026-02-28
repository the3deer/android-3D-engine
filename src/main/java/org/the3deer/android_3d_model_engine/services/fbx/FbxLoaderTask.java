package org.the3deer.android_3d_model_engine.services.fbx;

import android.app.Activity;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * FBX loader supported by ufbx <a href="https://github.com/ufbx/ufbx">ufbx</a>
 *
 * @author andresoviedo
 */

public final class FbxLoaderTask extends LoaderTask {

    private final static String TAG = FbxLoaderTask.class.getSimpleName();

    private FbxLoader loader = new FbxLoader();

    public FbxLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws Exception {
        try {
            final List<Object3DData> load = loader.load(uri, callback);

            final Scene sceneDefault = new SceneImpl();
            sceneDefault.setObjects(load);

            callback.onLoad(sceneDefault);

            return load;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
