package org.the3deer.android_3d_model_engine.renderer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.PreferenceScreen;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

public class RendererController implements MenuAdapter, PreferenceAdapter {

    private final static String TAG = RendererController.class.getSimpleName();

    // menu
    private final int MENU_ORDER_ID = Constants.MENU_ORDER_ID.getAndIncrement();
    private final int MENU_ITEM_ID = Constants.MENU_ITEM_ID.getAndIncrement();
    private final int MENU_GROUP_ID = Constants.MENU_GROUP_ID.getAndIncrement();
    private final Map<Integer, Renderer> MENU_MAPPING = new HashMap<>();

    // menu
    private SubMenu subMenu;

    // drawers
    @Inject
    private List<Renderer> renderers;

    @Inject
    private Camera camera;

    @Override
    public void onRestorePreferences(@Nullable Map<String, ?> preferences) {
        PreferenceAdapter.super.onRestorePreferences(preferences);

        // get preferences
        Set<String> debug = (Set<String>) preferences.get("debug");
        if (debug == null) return;

        try {
            for (Renderer renderer : renderers) {
                renderer.setEnabled(debug.contains(renderer.getClass().getSimpleName()));
            }
        }catch(Exception ex){
            // ignore
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {
        PreferenceAdapter.super.onCreatePreferences(savedInstanceState, rootKey, context, screen);

        if (renderers == null || renderers.isEmpty()) return;

        final String[] entries = new String[renderers.size()];
        final String[] entriesValues = new String[renderers.size()];
        final HashSet<String> values = new HashSet<>();
        for (int i=0; i< renderers.size(); i++){
            final Renderer renderer = renderers.get(i);
            entries[i] = renderer.getClass().getSimpleName();
            entriesValues[i] = renderer.getClass().getSimpleName();
            if (renderer.isEnabled()){
                values.add(entriesValues[i]);
            }
        }

        MultiSelectListPreference list = new MultiSelectListPreference(context);
        list.setIconSpaceReserved(screen.isIconSpaceReserved());
        list.setKey("debug");
        list.setTitle("Debug");
        list.setEntries(entries);
        list.setEntryValues(entriesValues);
        list.setDefaultValue(values);
        list.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(context, newValue.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            if(newValue instanceof String[]) {
                final List<String> list1 = Arrays.asList((String[]) newValue);
                for (Renderer renderer : renderers) {
                    renderer.setEnabled(list1.contains(renderer.getClass().getSimpleName()));
                }
            } else if (newValue instanceof HashSet){
                for (Renderer renderer : renderers) {
                    renderer.setEnabled(((HashSet<?>) newValue).contains(renderer.getClass().getSimpleName()));
                }
            }
            return true;
        });
        screen.addPreference(list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        subMenu = menu.addSubMenu(MENU_GROUP_ID, MENU_ITEM_ID, MENU_ORDER_ID, R.string.toggle_view);
        refresh();
        return true;
    }

    public void setUp() {
        refresh();
    }

    private void refresh(){
        if (subMenu == null || renderers == null) return;

        for (Renderer renderer : renderers){
            int mappingId1 = Constants.MENU_ITEM_ID.getAndIncrement();
            MENU_MAPPING.put(mappingId1, renderer);

            //renderer.getObjects();

            final MenuItem item1 = subMenu.add(MENU_GROUP_ID, mappingId1, 0, renderer.getClass().getSimpleName());
            item1.setCheckable(true);
            item1.setChecked(renderer.isEnabled());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // check
        if (item.getGroupId() != MENU_GROUP_ID) return false;
        if (!MENU_MAPPING.containsKey(item.getItemId())) return false;
        final Renderer target = MENU_MAPPING.get(item.getItemId());
        if (target == null) return false;

        // perform
        Log.i("SkyBoxDrawer","View changed: "+target);
        target.setEnabled(!item.isChecked());

        // update
        item.setChecked(target.isEnabled());
        return true;
    }
}
