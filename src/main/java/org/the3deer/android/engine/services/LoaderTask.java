package org.the3deer.android.engine.services;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.the3deer.android.engine.model.Object3D;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This component allows loading the model without blocking the UI.
 *
 * @author andresoviedo
 */
public abstract class LoaderTask {

	private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private final Handler handler = new Handler(Looper.getMainLooper());

	/**
	 * URL to the 3D model
	 */
	protected final Uri uri;
	/**
	 * Callback to notify of events
	 */
	protected final LoadListener callback;

	/**
	 * Build a new progress dialog for loading the data model asynchronously
	 * @param uri        the URL pointing to the 3d model
	 *
	 */
	public LoaderTask(Uri uri, LoadListener callback) {
		this.uri = uri;
		this.callback = callback; }


	public void execute() {
		onPreExecute();
		executor.execute(() -> {
			try {
				handler.post(callback::onLoadStart);
				final List<Object3D> data = build();
				handler.post(() -> {
					onPostExecute(data);
					callback.onLoadComplete();
				});
			} catch (final Exception ex) {
				Log.e("LoaderTask", ex.getMessage(), ex);
				handler.post(() -> callback.onLoadError(ex));
			} catch (final OutOfMemoryError err) {
				handler.post(() -> callback.onLoadError(new RuntimeException("Out Of Memory Error", err)));
			}
		});
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
		handler.post(() -> onProgressUpdate(values));
	}
}