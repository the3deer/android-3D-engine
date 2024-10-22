package org.the3deer.android_3d_model_engine.toolbar;

import android.view.Menu;
import android.view.MenuItem;

/**
 * Called whenever there is a view (android activity) event
 */
public interface MenuAdapter {

    boolean onCreateOptionsMenu(Menu menu);

    boolean onOptionsItemSelected(MenuItem item);
}
