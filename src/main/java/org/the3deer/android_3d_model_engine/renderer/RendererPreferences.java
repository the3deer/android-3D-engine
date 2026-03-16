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
import org.the3deer.android_3d_model_engine.view.GLRendererImpl;
import org.the3deer.android_3d_model_engine.view.Renderer;
import org.the3deer.util.bean.BeanInit;

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

    @Inject
    private GLRendererImpl glRenderer;

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
        if (renderersS != null) {
            for (Renderer renderer : renderers) {
                renderer.setEnabled(renderer.getClass().getName().equals(renderersS));
            }
        }

        // background color
        String backgroundColorS = (String) preferences.get(this.getClass().getName() + ".render.backgroundColor");
        if (backgroundColorS != null && glRenderer != null) {
            glRenderer.setBackgroundColor(getColor(backgroundColorS));
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
            entries[i] = renderer.getClass().getSimpleName().replace("Renderer", "");
            entriesValues[i] = renderer.getClass().getName();
            if (renderer.isEnabled() && value == null) {
                value = entriesValues[i];
            }
        }

        PreferenceCategory category = (PreferenceCategory) screen.findPreference(this.getClass().getName());
        if (category == null) {
            category = new PreferenceCategory(context);
            category.setKey(this.getClass().getName());
            category.setTitle("Renderer Settings");
            category.setLayoutResource(R.layout.preference_category);
            screen.addPreference(category);
        }

        ListPreference list = new ListPreference(context);
        list.setIconSpaceReserved(false);
        list.setKey(this.getClass().getName() + ".render.impl");
        list.setTitle("Render Mode");
        list.setSummary("%s");
        list.setEntries(entries);
        list.setEntryValues(entriesValues);
        list.setDefaultValue(value);
        list.setValue(value);
        list.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue instanceof String) {
                for (Renderer renderer : renderers) {
                    renderer.setEnabled(newValue.equals(renderer.getClass().getName()));
                }
                list.setValue((String) newValue);
            }
            return true;
        });
        category.addPreference(list);

        // background color
        ListPreference colorList = new ListPreference(context);
        colorList.setIconSpaceReserved(false);
        colorList.setKey(this.getClass().getName() + ".render.backgroundColor");
        colorList.setTitle("Background Color");
        colorList.setSummary("%s");
        colorList.setEntries(new String[]{context.getString(R.string.white), context.getString(R.string.gray), context.getString(R.string.black)});
        colorList.setEntryValues(new String[]{"white", "gray", "black"});
        colorList.setDefaultValue("gray");
        if (glRenderer != null) {
            colorList.setValue(getColorName(glRenderer.getBackgroundColor()));
        }
        colorList.setOnPreferenceChangeListener((preference, newValue) -> {
            if (glRenderer != null) {
                glRenderer.setBackgroundColor(getColor((String) newValue));
            }
            colorList.setValue((String) newValue);
            return true;
        });
        category.addPreference(colorList);
    }

    private void onCreateDrawerPrefs(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        if (drawers == null || drawers.isEmpty()) return;

        PreferenceCategory category = (PreferenceCategory) screen.findPreference("drawers_category");
        if (category == null) {
            category = new PreferenceCategory(context);
            category.setKey("drawers_category");
            category.setTitle("Scene Layers");
            category.setLayoutResource(R.layout.preference_category);
            screen.addPreference(category);
        }

        for (int i = 0; i < drawers.size(); i++) {
            final Drawer drawer = drawers.get(i);
            String label = drawer.getClass().getSimpleName().replace("Drawer", "");

            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(context);
            pref.setKey(drawer.getClass().getName() + ".enabled");
            pref.setTitle("Show " + label);
            pref.setDefaultValue(drawer.isEnabled());
            pref.setChecked(drawer.isEnabled());
            pref.setIconSpaceReserved(false);
            category.addPreference(pref);

            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                Boolean enabled = (Boolean) newValue;
                drawer.setEnabled(enabled);
                pref.setChecked(enabled);
                return true;
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        subMenu = menu.addSubMenu(MENU_GROUP_ID, MENU_ITEM_ID, MENU_ORDER_ID, R.string.toggle_view);
        refresh();
        return true;
    }

    @BeanInit
    public void setUp() {
        refresh();
    }

    private void refresh() {
        if (subMenu == null || renderers == null) return;

        for (Renderer renderer : renderers) {
            int mappingId1 = Constants.MENU_ITEM_ID.getAndIncrement();
            MENU_MAPPING.put(mappingId1, renderer);

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

    private float[] getColor(String value) {
        if ("white".equals(value)) {
            return Constants.COLOR_WHITE;
        } else if ("black".equals(value)) {
            return Constants.COLOR_BLACK;
        } else {
            return Constants.COLOR_GRAY;
        }
    }

    private String getColorName(float[] color) {
        if (color == Constants.COLOR_WHITE) {
            return "white";
        } else if (color == Constants.COLOR_BLACK) {
            return "black";
        } else {
            return "gray";
        }
    }
}
