package org.the3deer.android_3d_model_engine.drawer;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.objects.SkyBox;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.renderer.Renderer;
import org.the3deer.android_3d_model_engine.shader.Shader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;
import org.the3deer.android_3d_model_engine.util.Rescaler;
import org.the3deer.util.android.GLUtil;
import org.the3deer.util.bean.BeanPostConstruct;
import org.the3deer.util.math.Quaternion;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class SkyBoxRenderer implements Renderer, MenuAdapter, PreferenceAdapter {

    // menu
    private final int MENU_ORDER_ID = Constants.MENU_ORDER_ID.getAndIncrement();
    private final int MENU_ITEM_ID = Constants.MENU_ITEM_ID.getAndIncrement();
    private final int MENU_GROUP_ID = Constants.MENU_GROUP_ID.getAndIncrement();
    private final Map<Integer,Integer> MENU_MAPPING = new HashMap<>();

    // dependencies

    @Inject
    private Screen screen;

    @Inject
    private Camera camera;

    // enablement
    private boolean enabled = true;

    // data
    private int skyboxId = 0;
    private SkyBox[] skyBoxes = null;
    private Object3DData[] skyBoxes3D = null;

    public float[] viewMatrix = new float[16];
    public float[] projectionMatrix = new float[16];

    private Quaternion orientation = new Quaternion(0,0,0,1);

    // preferences
    private ListPreference skyboxList;
    private String[] skyBoxesNames = new String[]{"None", "Sea", "Sand"};;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.skyboxId == 0){
            this.skyboxId = 1;
        }
    }

    public void setSkyBox(int i) {
        skyboxId = i;
        enabled = i != 0;
    }

/*    @Override
    public List<? extends Object3DData> getObjects() {
        if (this.skyboxId < 0 || skyBoxes3D == null || this.skyboxId >= skyBoxes.length) {
            return Collections.emptyList();
        }
        return Collections.singletonList(skyBoxes3D[skyboxId]);
    }*/

    @BeanPostConstruct
    public void setUp(){
        skyBoxes = new SkyBox[]{null, SkyBox.getSkyBox1(), SkyBox.getSkyBox2()};
        skyBoxes3D = new Object3DData[skyBoxes.length];
        Matrix.frustumM(projectionMatrix, 0, -screen.getRatio(), screen.getRatio(),
                -1f, 1f, Constants.near, Constants.far);
    }

    @Override
    public void onRestorePreferences(@Nullable Map<String, ?> preferences) {
        PreferenceAdapter.super.onRestorePreferences(preferences);
        if (preferences.containsKey("skybox")){
            skyboxId = Integer.valueOf((String)preferences.get("skybox"));
            setSkyBox(skyboxId);
        }
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {
        PreferenceAdapter.super.onCreatePreferences(savedInstanceState, rootKey, context, screen);

        skyboxList = new ListPreference(context);
        skyboxList.setIconSpaceReserved(screen.isIconSpaceReserved());
        skyboxList.setKey("skybox");
        skyboxList.setTitle("Skybox");

        skyboxList.setEntries(this.skyBoxesNames);
        skyboxList.setEntryValues(new String[]{"0", "1", "2"});
        skyboxList.setDefaultValue(String.valueOf(skyboxId));

        skyboxList.setSummary(skyboxId >= 0 && skyboxId < skyBoxesNames.length? skyBoxesNames[skyboxId] : "Unknown");
        screen.addPreference(skyboxList);

        skyboxList.setSummaryProvider(new Preference.SummaryProvider(){
            @Nullable
            @Override
            public CharSequence provideSummary(@NonNull Preference preference) {
                if (skyBoxesNames != null && skyboxId >= 0 && skyboxId < skyBoxesNames.length) {
                    return skyBoxesNames[skyboxId];
                }
                return null;
            }
        });
        skyboxList.setOnPreferenceChangeListener((preference, newValue) -> {
            // perform
            Log.i("SkyBoxDrawer","New skybox: "+newValue);
            setSkyBox(Integer.parseInt((String)newValue));
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final SubMenu subMenu = menu.addSubMenu(MENU_GROUP_ID, MENU_ITEM_ID, MENU_ORDER_ID, R.string.toggle_skybox);

        int mappingId1 = Constants.MENU_ITEM_ID.getAndIncrement();
        MENU_MAPPING.put(mappingId1, -1);
        final MenuItem item1 = subMenu.add(MENU_GROUP_ID, mappingId1, 0, "No SkyBox");
        item1.setCheckable(true);
        item1.setChecked(skyboxId == -1);

        int mappingId2 = Constants.MENU_ITEM_ID.getAndIncrement();
        MENU_MAPPING.put(mappingId2, 0);
        final MenuItem item2 = subMenu.add(MENU_GROUP_ID, mappingId2, 1, "SkyBox 1");
        item2.setCheckable(true);
        item2.setChecked(skyboxId == 0);

        int mappingId3 = Constants.MENU_ITEM_ID.getAndIncrement();
        MENU_MAPPING.put(mappingId3, 1);
        final MenuItem item3 = subMenu.add(MENU_GROUP_ID, mappingId3, 2, "SkyBox 2");
        item3.setCheckable(true);
        item3.setChecked(skyboxId == 1);

        subMenu.setGroupCheckable(MENU_GROUP_ID, true, true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // check
        if (item.getGroupId() != MENU_GROUP_ID) return false;
        if (!MENU_MAPPING.containsKey(item.getItemId())) return false;
        final Integer skyBoxId = MENU_MAPPING.get(item.getItemId());
        if (skyBoxId == null) return false;

        // perform
        Log.i("SkyBoxDrawer","New skybox: "+skyBoxId);
        setSkyBox(skyBoxId);

        // update
        item.setChecked(true);
        return true;
    }

    public int toggle(){
        skyboxId++;
        if (skyboxId >= 2) {
            skyboxId = -1;
        }
        Log.i("SkyBoxDrawer", "Toggled skybox. Idx: " + skyboxId);
        return skyboxId;
    }

    @Override
    public void onDrawFrame() {
        draw(null, null, null, -1, null, null, null, -1, -1);
    }

    //@Override
    private void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawType, int drawSize) {

        // enabled?
        if (!enabled) return;

        // enabled?
        int skyBoxId = this.skyboxId;

        // assert
        if (skyBoxId < 1 || skyBoxId >= skyBoxes.length) return;

        try {
            // lazy building of the 3d object
            if (skyBoxes3D[skyBoxId] == null) {
                Log.i("SkyBoxDrawer", "Loading sky box textures to GPU... skybox: " + skyBoxId);
                int textureIdMap = GLUtil.loadCubeMap(skyBoxes[skyBoxId].getCubeMap());
                Log.d("SkyBoxDrawer", "Loaded textures to GPU... id: " + textureId);
                if (textureIdMap != -1) {
                    skyBoxes3D[skyBoxId] = SkyBox.build(skyBoxes[skyBoxId]);
                    Rescaler.rescale(skyBoxes3D[skyBoxId], 1f);
                    final float scale = Constants.SKYBOX_SIZE; //getFar()/skyBoxes3D[skyBoxId].getDimensions().getLargest()/20;
                    skyBoxes3D[skyBoxId].setScale(scale, scale, scale);
                    skyBoxes3D[skyBoxId].setColor(Constants.COLOR_BIT_TRANSPARENT);
                } else {
                    throw new IllegalArgumentException("Error loading sky box textures to GPU");
                }
            }

            // get drawer
            Shader basicDrawer = ShaderFactory.getInstance().getSkyBoxDrawer();

            // paint
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glClearColor(0, 0, 0, 1);
            GLES20.glDisable(GLES20.GL_CULL_FACE);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            basicDrawer.draw(skyBoxes3D[skyboxId], this.projectionMatrix, camera.getViewMatrix(),
                    null, null, camera.getPos(), skyBoxes3D[skyboxId].getDrawMode(), skyBoxes3D[skyboxId].getDrawSize());

            // sensor stuff
            /*this.orientation.toRotationMatrix(viewMatrixSkyBox);
            float[] rot = new float[16];
            Matrix.setRotateM(rot,0,90,1,0,0);
            float[] mat = new float[16];
            Matrix.multiplyMM(mat,0,viewMatrixSkyBox,0, rot,0);
            Renderer basicShader = drawer.getSkyBoxDrawer();
            basicShader.draw(skyBoxes3D[skyBoxId], projectionMatrixSkyBox, mat, skyBoxes3D[skyBoxId].getMaterial().getTextureId(), null, cameraPosInWorldSpace);*/
        } catch (Throwable ex) {
            Log.e("SkyBoxDrawer", "Error rendering sky box. " + ex.getMessage(), ex);
            enabled = false;
        }
    }

}
