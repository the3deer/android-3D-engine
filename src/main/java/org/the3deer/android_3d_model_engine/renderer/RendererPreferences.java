package org.the3deer.android_3d_model_engine.renderer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;
import org.the3deer.android_3d_model_engine.view.Renderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class RendererPreferences implements MenuAdapter, PreferenceAdapter {

    private final static String TAG = RendererPreferences.class.getSimpleName();

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
    private List<Drawer> drawers;

    @Inject
    private Camera camera;

    @Override
    public void onRestorePreferences(@Nullable Map<String, ?> preferences) {
        PreferenceAdapter.super.onRestorePreferences(preferences);
        onRestoreRendererPrefs(preferences);
        onRestoreDrawerPrefs(preferences);
    }

    private void onRestoreDrawerPrefs(@Nullable Map<String, ?> preferences) {
        for (Drawer drawer : drawers) {
            Object o = preferences.get(drawer.getClass().getName() + ".enabled");
            if (o != null) {
                drawer.setEnabled((Boolean) o);
            }
        }
    }

    private void onRestoreRendererPrefs(@Nullable Map<String, ?> preferences) {

        // get preferences
        String renderersS = (String) preferences.get(this.getClass().getName() + ".render.impl");
        if (renderersS == null) return;

        for (Renderer renderer : renderers) {
            renderer.setEnabled(renderer.getClass().getName().equals(renderersS));
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {
        PreferenceAdapter.super.onCreatePreferences(savedInstanceState, rootKey, context, screen);
        onCreateRendererPrefs(savedInstanceState, rootKey, context, screen);
        onCreateDrawerPrefs(savedInstanceState, rootKey, context, screen);
    }

    private void onCreateRendererPrefs(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        if (renderers == null || renderers.isEmpty()) return;

        final String[] entries = new String[renderers.size()];
        final String[] entriesValues = new String[renderers.size()];
        String value = null;
        for (int i = 0; i < renderers.size(); i++) {
            final Renderer renderer = renderers.get(i);
            entries[i] = renderer.getClass().getSimpleName();
            entriesValues[i] = renderer.getClass().getName();
            if (renderer.isEnabled() && value == null) {
                value = entriesValues[i];
            }
        }

        Preference category = screen.findPreference(this.getClass().getName());
        if (category == null) {
            category = new PreferenceCategory(context);
            category.setKey(this.getClass().getName());
            category.setTitle(this.getClass().getSimpleName());
            category.setLayoutResource(R.layout.preference_category);
            screen.addPreference(category);
        }

        ListPreference list = new ListPreference(context);
        list.setIconSpaceReserved(screen.isIconSpaceReserved());
        list.setKey(this.getClass().getName() + ".render.impl");
        list.setTitle("Render");
        list.setEntries(entries);
        list.setEntryValues(entriesValues);
        list.setDefaultValue(value);
        list.setValue(value);
        list.setOnPreferenceChangeListener((preference, newValue) -> {
            Toast.makeText(context, String.valueOf(newValue), Toast.LENGTH_LONG).show();
            if (newValue instanceof String) {
                for (Renderer renderer : renderers) {
                    renderer.setEnabled(newValue.equals(renderer.getClass().getName()));
                }
                list.setValue((String) newValue);
            }
            return true;
        });
        ((PreferenceGroup) category).addPreference(list);

    }

    private void onCreateDrawerPrefs(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        if (drawers == null || drawers.isEmpty()) return;

        for (int i = 0; i < drawers.size(); i++) {
            final Drawer drawer = drawers.get(i);

            Preference category2 = screen.findPreference(drawer.getClass().getName());
            if (category2 == null) {
                category2 = new PreferenceCategory(context);
                category2.setKey(drawer.getClass().getName());
                category2.setTitle(drawer.getClass().getSimpleName());
                category2.setLayoutResource(R.layout.preference_category);
                screen.addPreference(category2);
            }

            // Example: SwitchPreference for Animation (if you had a boolean)
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey(drawer.getClass().getName() + ".enabled");
            pref.setTitle("Enable");
            pref.setDefaultValue(drawer.isEnabled());
            pref.setChecked(drawer.isEnabled());
            pref.setIconSpaceReserved(false);
            ((PreferenceGroup) category2).addPreference(pref);

            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                Boolean newLightingValue = (Boolean) newValue;
                drawer.setEnabled(newLightingValue);
                // Update summary or other UI elements if needed
                pref.setChecked(newLightingValue);
                preference.setSummary("Current: " + newLightingValue);
                return true; // True to update the preference's state
            });
        }
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

    private void refresh() {
        if (subMenu == null || renderers == null) return;

        for (Renderer renderer : renderers) {
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
        Log.i(TAG, "View changed: " + target);
        target.setEnabled(!item.isChecked());

        // update
        item.setChecked(target.isEnabled());
        return true;
    }
}
