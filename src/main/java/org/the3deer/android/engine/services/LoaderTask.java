package org.the3deer.android.engine.services;

import org.the3deer.android.engine.model.Object3D;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This component allows loading the model without blocking the UI.
 *
 * @author andresoviedo
 */
public abstract class LoaderTask {

    protected static final Logger logger = Logger.getLogger(LoaderTask.class.getSimpleName());

    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    /**
     * URI to the 3D model
     */
    protected final URI uri;
    /**
     * Callback to notify of events
     */
    protected final LoadListener callback;

    /**
     * Build a new progress dialog for loading the data model asynchronously
     *
     * @param uri the URI pointing to the 3d model
     *
     */
    public LoaderTask(URI uri, LoadListener callback) {
        this.uri = uri;
        this.callback = callback;
    }


    public void execute() {
        this.execute(false);
    }

    public void execute(boolean async) {
        onPreExecute();
        if (async) {
            executor.execute(this::executeImpl);
        } else {
            executeImpl();
        }
    }

    private void executeImpl() {
        try {
            callback.onLoadStart();
            build();
            callback.onLoadComplete();
        } catch (final Throwable t) {
            logger.log(Level.SEVERE, "Error loading model: " + t.getMessage(), t);
            callback.onLoadError(t instanceof Exception ? (Exception) t : new RuntimeException(t));
            throw (t instanceof RuntimeException) ? (RuntimeException) t : new RuntimeException(t);
        }
    }

    protected void onPreExecute() {
    }

    protected abstract List<Object3D> build() throws Exception;

    protected void onProgressUpdate(String... values) {
        if (values.length > 0) {
            callback.onProgress(values[0]);
        }
    }

    protected void onPostExecute(List<Object3D> data) {
    }

    protected void onProgress(String progress) {
        publishProgress(progress);
    }

    protected final void publishProgress(String... values) {
        onProgressUpdate(values);
    }
}
