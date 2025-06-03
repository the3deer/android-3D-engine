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
import org.the3deer.android_3d_model_engine.model.Object3DData;
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

public class SceneManager implements Scene, PreferenceAdapter {

    private final static String TAG = SceneManager.class.getSimpleName();

    @Inject
    private BeanFactory beanFactory;

    @Inject
    private Camera camera;
    @Inject
    private Light light;
    @Inject
    private Projection projection;
    @Inject
    private List<Scene> scenes;

    // variables
    private Scene delegate;

    @BeanInit
    public void setUp(){
        if (scenes == null || scenes.isEmpty()){
            throw new IllegalStateException("No scenes found");
        }
        if (scenes.size() == 1){
            throw new IllegalStateException("Need at least 1 Scene implementation");
        }

        // default scene
        delegate = scenes.get(1);

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

        Log.i(TAG, "SceneManager initialized. default scene: "+delegate);
    }



    @Override
    public void setName(String name) {
        throw new RuntimeException("this is a not to be called");
    }

    @NonNull
    @Override
    public String getName() {
        return "SceneManager: "+ System.identityHashCode(this);
    }

    @Override
    public Camera getCamera() {
        return delegate.getCamera();
    }

    public void addScene(Scene scene) {
        /*this.scenes.add(scene);
        scene.setCamera(camera);*/
        //scene.setProjection(projection);

        // FIXME:  beanFactory needs to refresh the BeanList
        beanFactory.addOrReplace("scene."+scene.getName(), scene);
        //beanFactory.init();

    }

    @Override
    public void addObject(Object3DData obj) {
        delegate.addObject(obj);
    }

    @Override
    public List<Object3DData> getObjects() {
        return delegate.getObjects();
    }

    @Override
    public Object3DData getSelectedObject() {
        return delegate.getSelectedObject();
    }

    @Override
    public void setCamera(Camera camera) {
        delegate.setCamera(camera);
    }

    @Override
    public void onLoadComplete() {
        delegate.onLoadComplete();
    }

    @Override
    public void reset() {
        if (scenes != null) {
            setUp();
            delegate.getObjects().clear();
        }
    }

    // ----------------------

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        if (scenes == null || scenes.size() < 2){
            return;
        }

        final List<Scene> scenes = new ArrayList<>(this.scenes);
        scenes.remove(0);

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
            if (i == 0) {
                sceneNamesA[i] = candidate.getName() + " (default)" ;
            } else {
                sceneNamesA[i] = candidate.getName();
            }
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


}
