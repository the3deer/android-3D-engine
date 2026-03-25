package org.the3deer.android.engine.services;

import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.the3deer.android.engine.model.Object3D;

import java.util.List;

/**
 * This component allows loading the model without blocking the UI.
 *
 * @author andresoviedo
 */
public abstract class LoaderTask extends AsyncTask<Void, String, List<Object3D>> {

	/**
	 * URL to the 3D model
	 */
	protected final Uri uri;
	/**
	 * Callback to notify of events
	 */
	protected final LoadListener callback;
	/**
	 * The dialog that will show the progress of the loading
	 */
	private final ProgressDialog dialog;

	/**
	 * Build a new progress dialog for loading the data model asynchronously
	 * @param uri        the URL pointing to the 3d model
	 *
	 */
	public LoaderTask(Activity parent, Uri uri, LoadListener callback) {
		this.uri = uri;
		this.dialog = new ProgressDialog(parent);
		this.callback = callback; }


	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		this.dialog.setMessage("Loading...");
		this.dialog.setCancelable(false);
		//this.dialog.getWindow().setGravity(Gravity.BOTTOM);
		this.dialog.show();
	}



	@Override
	protected List<Object3D> doInBackground(Void... params) {
		try {
			//ContentUtils.setThreadActivity(dialog.getContext());
			callback.onStart();
			List<Object3D> data = build();
			callback.onLoadComplete();
			//ContentUtils.setThreadActivity(null);
			return  data;
		} catch (Exception ex) {
			Log.e("LoaderTask",ex.getMessage(),ex);
			callback.onLoadError(ex);
			return null;
		} catch (OutOfMemoryError err){
			callback.onLoadError(new RuntimeException("Out Of Memory Error",err));
			return null;
		}
	}

	protected abstract List<Object3D> build() throws Exception;

	@Override
	protected void onProgressUpdate(String... values) {
		super.onProgressUpdate(values);
		this.dialog.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(List<Object3D> data) {
		super.onPostExecute(data);
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	protected void onProgress(String progress) {
		super.publishProgress(progress);
	}
}