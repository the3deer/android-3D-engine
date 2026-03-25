package org.the3deer.android.engine.services.fbx;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

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

    public FbxLoaderTask(Activity parent, Uri uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3D> build() throws Exception {
        try {
            callback.onStart();

            final List<Object3D> load = loader.load(uri, callback);

            final Scene sceneDefault = new Scene();
            sceneDefault.getObjects().addAll(load);
            callback.onLoad(sceneDefault);
            callback.onLoadComplete(sceneDefault);
            callback.onLoadComplete();

            return load;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
