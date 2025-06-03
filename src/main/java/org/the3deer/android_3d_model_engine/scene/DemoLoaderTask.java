package org.the3deer.android_3d_model_engine.scene;

import android.app.Activity;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.objects.Cube;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.LoaderTask;
import org.the3deer.util.android.ContentUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * This class loads a 3D scene as an example of what can be done with the app
 *
 * @author andresoviedo
 */
public class DemoLoaderTask extends LoaderTask {

    /**
     * Build a new progress dialog for loading the data model asynchronously
     *
     * @param parent   parent activity
     * @param uri      the URL pointing to the 3d model
     * @param callback listener
     */
    public DemoLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
        ContentUtils.provideAssets(parent);
    }

    @Override
    protected List<Object3DData> build() throws Exception {

        // test cube made of arrays
        Object3DData obj10 = Cube.buildCubeV1();
        obj10.setColor(new float[]{0.5f, 0.75f, 0.5f, 0.75f});
        super.onLoad(obj10);

        return Collections.singletonList(obj10);
    }
}
