package org.the3deer.android_3d_model_engine.scene;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanInit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

public class SceneManager implements PreferenceAdapter {

    private final static String TAG = SceneManager.class.getSimpleName();

    @Inject
    private BeanFactory beanFactory;

    @Inject
    private Camera camera;
    @Inject
    private Light light;
    @Inject
    private Projection projection;

    private List<Scene> scenes;

    // variables
    private Scene delegate;

    public SceneManager() {
        this.scenes = new ArrayList<>();
    }

    @BeanInit
    public void setUp(){
        if (scenes == null || scenes.isEmpty()){
            return;
        }

        // default scene
        delegate = scenes.get(0);
        delegate.setEnabled(true);

        /*// FIXME: only for testing
        final Object3DData plane = Plane2.build();
        plane.setColor(Constants.COLOR_GREEN);
        plane.setLocation(new float[]{0f, -Constants.DEFAULT_MODEL_SIZE/2, 0f});
        scene2.addObject(plane);
        // test cube made of arrays
        Object3DData obj10 = Cube.buildCubeV1();
        obj10.setColor(new float[] { 1f, 0f, 0f, 0.5f });
        obj10.setLocation(new float[] { -2f, 2f, 0f });
        obj10.setScale(0.5f, 0.5f, 0.5f);
        scene2.addObject(obj10);

        scene2.onLoadComplete();*/
    }

    public void addScene(Scene scene) {
        //String id = "20.scene." + scenes.size() + scene.getName();
        Log.d(TAG, "Adding scene to SceneManager: "+scene.getName());
        //beanFactory.addOrReplace(id, scene);

        // initialize default scene
        if (delegate == null){
            delegate = scene;
        };
    }

    // ----------------------

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        if (scenes == null || scenes.isEmpty()){
            return;
        }

        final List<Scene> scenes = new ArrayList<>(this.scenes);

        PreferenceGroup category = screen.findPreference(this.getClass().getName());
        if (category == null){
            category = new PreferenceCategory(context);
            category.setKey(getClass().getName());
            category.setTitle(getClass().getSimpleName());
            category.setLayoutResource(R.layout.preference_category);
            screen.addPreference(category);
        }

        final Set<String> scenesEnabled = new HashSet<>();
        final CharSequence[] sceneNamesA = new CharSequence[scenes.size()];
        final CharSequence[] sceneValuesA = new CharSequence[scenes.size()];
        for (int i = 0; i < scenes.size(); i++) {
            final Scene candidate = scenes.get(i);
            sceneNamesA[i] = candidate.getName();
            sceneValuesA[i] = String.valueOf(i);
            if(candidate.isEnabled()) {
                scenesEnabled.add(String.valueOf(i));
            }
        }

        MultiSelectListPreference sceneList = new MultiSelectListPreference(context);
        sceneList.setIconSpaceReserved(screen.isIconSpaceReserved());
        sceneList.setKey(getClass().getName()+".scenes");
        sceneList.setTitle("Scenes");
        sceneList.setEntries(sceneNamesA);
        sceneList.setEntryValues(sceneValuesA);
        sceneList.setDefaultValue(scenesEnabled);
        sceneList.setValues(scenesEnabled);
        sceneList.setSummary("Total: "+scenes.size());
        category.addPreference(sceneList);
        sceneList.setSummaryProvider(new Preference.SummaryProvider(){
            @NonNull
            @Override
            public CharSequence provideSummary(@NonNull Preference preference) {
                return "Total: "+scenes.size();
            }
        });
        sceneList.setOnPreferenceChangeListener((preference, newValue) -> {
            // perform
            Log.i(TAG,"Change scene: "+newValue);
            final Set<String> scenesSelected = (Set<String>)newValue;
            for (int i=0; i<scenes.size(); i++) {
                final Scene scene = scenes.get(i);
                scene.setEnabled(scenesSelected.contains(String.valueOf(i)));
                if (scene.isEnabled()){
                    delegate = scene;
                    Log.i(TAG,"Scene enabled: "+scene.getName());
                }
            }
            //sceneList.setValues(scenesSelected);
            return true;
        });
    }

    public Scene getCurrentScene() {
        return delegate;
    }

    public List<Scene> getScenes() {
        return scenes;
    }

    public void setCurrentScene(Scene scene) {
        this.delegate = scene;
    }
}
