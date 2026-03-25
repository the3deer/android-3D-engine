package org.the3deer.android.engine.services.wavefront;

import android.app.Activity;
import android.net.Uri;
import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;

import java.net.URI;
import java.util.List;

/**
 * Wavefront loader implementation
 *
 * @author andresoviedo
 */

public class WavefrontLoaderTask extends LoaderTask {

    public WavefrontLoaderTask(final Activity parent, final Uri uri, final LoadListener callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3D> build() {

        final WavefrontLoader wfl = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, callback);

        super.publishProgress("Loading model...");

        final List<Object3D> load = wfl.load(URI.create(uri.toString()));

        return load;
    }

    @Override
    public void onProgress(String progress) {
        super.publishProgress(progress);
    }
}
