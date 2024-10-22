package org.the3deer.android_3d_model_engine.camera;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceScreen;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Projection;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.event.EventListener;

import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

public final class CameraController implements Camera.Controller, EventListener,
        PreferenceAdapter, MenuAdapter {

    interface Handler extends Camera.Controller {

        void enable();
    }

    private final static String TAG = CameraController.class.getSimpleName();

    // menu
    private final int MENU_GROUP_ID = Constants.MENU_GROUP_ID.getAndIncrement();
    private final Map<Integer,Projection> MENU_MAPPING = new HashMap<>();

    // dependencies
    @Inject
    private BeanFactory beanFactory;
    @Inject
    private Scene scene;
    @Inject
    private Screen screen;
    /*@Inject
    private List<Camera.Controller> controllers;*/

    // vars
    private Handler handlerDefault ;
    private Handler handlerIsometric;
    private Handler handlerOrtho;
    private Handler handlerPOV;

    // state
    private Handler handler;
    private Projection projection;

    public CameraController() {
    }

    /*public void setCamera(Camera camera) {
        this.camera = camera;
    }*/

    public void setUp(){
        /*this.handlerDefault = new DefaultHandler(camera);
        this.handlerIsometric = new IsometricHandler(camera);
        this.handlerOrtho = new OrthographicHandler(camera);
        this.handlerPOV = new POVHandler(camera);
        this.projection = Projection.PERSPECTIVE;
        this.handler = handlerDefault;*/

        this.scene.getCamera().setController(this);

        this.handlerDefault = beanFactory.addAndGet("camera.handlerDefault", DefaultHandler.class);
        this.handlerIsometric = beanFactory.addAndGet("camera.handlerIsometric", IsometricHandler.class);
        this.handlerOrtho = beanFactory.addAndGet("camera.handlerOrtho", OrthographicHandler.class);
        this.handlerPOV = beanFactory.addAndGet("camera.handlerPOV", POVHandler.class);
        this.handler = handlerDefault;
        this.projection = Projection.PERSPECTIVE;
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        Log.v("CameraController", "Restoring state...");

        if (state.containsKey("camera.pos") && state.containsKey("camera.view") && state.containsKey("camera.up")
                && state.containsKey("camera.projection")) {
            updateHandler(Projection.valueOf(state.getString("camera.projection")),
                    Objects.requireNonNull(state.getFloatArray("camera.pos")),
                    Objects.requireNonNull(state.getFloatArray("camera.up")),
                    Objects.requireNonNull(state.getFloatArray("camera.view")));
        } else {
            updateHandler(this.projection, null, null, null);
        }
        Log.v("CameraController","State restored");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v("CameraController","Saving state...");
        if (scene != null && scene.getCamera() != null && projection != null) {
            outState.putFloatArray("camera.pos", scene.getCamera().getPos());
            outState.putFloatArray("camera.view", scene.getCamera().getView());
            outState.putFloatArray("camera.up", scene.getCamera().getUp());
            outState.putString("camera.projection", projection.name());
        }
        Log.v("CameraController","State saved: "+outState);
    }

    @Override
    public void onRestorePreferences(@Nullable Map<String, ?> preferences) {
        PreferenceAdapter.super.onRestorePreferences(preferences);
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {
        PreferenceAdapter.super.onCreatePreferences(savedInstanceState, rootKey, context, screen);

        /*PreferenceCategory cameraCategory = new PreferenceCategory(context);
        cameraCategory.setKey("camera");
        cameraCategory.setTitle("Camera");
        screen.addPreference(cameraCategory);*/

        ListPreference projectionList = new ListPreference(context);
        projectionList.setIconSpaceReserved(screen.isIconSpaceReserved());
        projectionList.setKey("projection");
        projectionList.setTitle("Projection");
        projectionList.setEntries(new String[]{Projection.PERSPECTIVE.name(), Projection.ISOMETRIC.name(), Projection.ORTHOGRAPHIC.name(), Projection.FREE.name()});
        projectionList.setEntryValues(new String[]{Projection.PERSPECTIVE.name(), Projection.ISOMETRIC.name(), Projection.ORTHOGRAPHIC.name(), Projection.FREE.name()});
        projectionList.setOnPreferenceChangeListener((preference, newValue) -> {
            // perform
            Log.i(TAG,"New projection: "+newValue);
            updateHandler(Projection.valueOf((String) newValue), null, null, null);
            return true;
        });

        //screen.addPreference(projectionList);

        /*PreferenceCategory helpCategory = new PreferenceCategory(context);
        helpCategory.setKey("help");
        helpCategory.setTitle("Help");
        screen.addPreference(helpCategory);

        Preference feedbackPreference = new Preference(context);
        feedbackPreference.setKey("feedback");
        feedbackPreference.setTitle("Send feedback");
        feedbackPreference.setSummary("Report technical issues or suggest new features");
        helpCategory.addPreference(feedbackPreference);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final SubMenu subMenu = menu.addSubMenu(MENU_GROUP_ID, Constants.MENU_ITEM_ID.getAndIncrement(), Constants.MENU_ORDER_ID.getAndIncrement(), R.string.toggle_camera);
        for (Projection p : Projection.values()) {
            int mappingId = Constants.MENU_ITEM_ID.getAndIncrement();
            this.MENU_MAPPING.put(mappingId, p);
            final MenuItem item = subMenu.add(MENU_GROUP_ID, mappingId, p.ordinal(), p.name());
            item.setCheckable(true);
            item.setChecked(p == this.projection);
        }
        subMenu.setGroupCheckable(MENU_GROUP_ID, true, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // check
        if (item.getGroupId() != MENU_GROUP_ID) return false;
        if (!this.MENU_MAPPING.containsKey(item.getItemId())) return false;
        final Projection projection = this.MENU_MAPPING.get(item.getItemId());
        if (projection == null) return false;

        // perform
        updateHandler(projection, null, null, null);
        item.setChecked(true);
        return true;
    }

    private void updateHandler(Projection projection, float[] pos, float[] up, float[] view) {
        Log.i("CameraController", "Projection: "+projection);
        this.projection = projection;
        switch (projection) {
            case PERSPECTIVE:
                this.handler = handlerDefault;
                break;
            case ORTHOGRAPHIC:
                this.handler = handlerOrtho;
                break;
            case ISOMETRIC:
                this.handler = handlerIsometric;
                break;
            case FREE:
                this.handler = handlerPOV;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported projection: "+projection);
        }
        Log.i("CameraController", "Handler: "+handler.getClass().getSimpleName());
        this.scene.getCamera().setController(this.handler);
        if (pos != null && up != null && view != null){
            Log.v("CameraController", "Handler pos: "+
                    Arrays.toString(pos)+","+ Arrays.toString(view)+","+ Arrays.toString(up));
            this.scene.getCamera().set(pos, view, up);
        }
        this.handler.enable();
    }


    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof TouchEvent) {
            //Log.v("CameraController","event: "+event);
            TouchEvent touchEvent = (TouchEvent) event;
            switch (touchEvent.getAction()) {
                case MOVE:
                    float dx1 = touchEvent.getdX();
                    float dy1 = touchEvent.getdY();
                    float max = Math.max(screen.getWidth(), screen.getHeight());
                    dx1 = (float) (dx1 / max * Math.PI * 2);
                    dy1 = (float) (dy1 / max * Math.PI * 2);
                    handler.move(dx1, dy1);
                    break;
                case ROTATE:
                    float rotation = touchEvent.getAngle();
                    handler.rotate(rotation);
                    break;
                case PINCH:
                    final float zoomFactor = ((TouchEvent) event).getZoom();
                    handler.zoom((float) (-zoomFactor/2 * Constants.near * Math.log(scene.getCamera().getDistance())));
                    break;
                case SPREAD:
                    // TODO:
                case CLICK:
                    break;

            }
            return true;
        }
        return false;
    }




}
