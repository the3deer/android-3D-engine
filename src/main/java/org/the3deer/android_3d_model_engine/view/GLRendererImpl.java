package org.the3deer.android_3d_model_engine.view;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.toolbar.MenuAdapter;
import org.the3deer.util.event.EventManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GL renderer
 * It calls all the @{@link Renderer#onDrawFrame()} in the list
 */
public class GLRendererImpl implements GLSurfaceView.Renderer, MenuAdapter {

    private final static String TAG = GLRendererImpl.class.getSimpleName();

    @Inject
    private Screen screen;

    @Inject
    private EventManager eventManager;
    @Inject
    private List<RenderListener> listeners;
    @Inject
    private List<Renderer> renderers;

    /**
     * Background GL clear color. Default is light gray
     */
    private float[] backgroundColor = Constants.COLOR_GRAY;
    // width of the screen
    private int width;
    // height of the screen
    private int height;
    private float ratio;

    // frames per second
    private long framesPerSecondTime = -1;
    private int framesPerSecond = 0;
    private int framesPerSecondCounter = 0;

    // sub-menu
    private final int MENU_ORDER_ID = Constants.MENU_ORDER_ID.getAndIncrement();
    private final int MENU_ITEM_ID = Constants.MENU_ITEM_ID.getAndIncrement();
    private final int MENU_GROUP_ID = Constants.MENU_GROUP_ID.getAndIncrement();
    private final Map<Integer, float[]> MENU_MAPPING = new HashMap<>();
    private SubMenu subMenu;

    /**
     * Construct a new renderer for the specified surface view
     */
    public GLRendererImpl() {
    }

    public void setBackgroundColor(float[] backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setShaders(List<Renderer> renderers) {
        this.renderers = renderers;
    }

    public float getNear() {
        return Constants.near;
    }

    public float getFar() {
        return Constants.far;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        subMenu = menu.addSubMenu(MENU_GROUP_ID, MENU_ITEM_ID, MENU_ORDER_ID, R.string.toggle_renderer);
        initSubMenu();
        subMenu.setGroupCheckable(MENU_GROUP_ID, true, true);
        return true;
    }

    private void initSubMenu() {
        addItem(Constants.COLOR_WHITE, R.string.white);
        addItem(Constants.COLOR_GRAY, R.string.gray);
        addItem(Constants.COLOR_BLACK, R.string.black);
    }

    private void addItem(float[] color, int label) {
        int mappingId1 = Constants.MENU_ITEM_ID.getAndIncrement();
        MENU_MAPPING.put(mappingId1, color);
        final MenuItem item1 = subMenu.add(MENU_GROUP_ID, mappingId1, 0, label);
        item1.setCheckable(true);
        item1.setChecked(this.backgroundColor == color);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // check
        if (item.getGroupId() != MENU_GROUP_ID) return false;
        if (!MENU_MAPPING.containsKey(item.getItemId())) return false;
        final float[] color = MENU_MAPPING.get(item.getItemId());
        if (color == null) return false;

        // perform
        Log.i(TAG, "New color: " + color);
        setBackgroundColor(color);

        // update
        item.setChecked(true);
        return true;
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        // log event
        Log.d(TAG, "onSurfaceCreated. config: " + config);

        // Set the background frame color
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        // Use culling to remove back faces.
        // Don't remove back faces so we can see them
        // GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing for hidden-surface elimination.
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Enable not drawing out of view port
        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CREATED));
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        Log.i(TAG, "onSurfaceChanged. with: " + width + ", height: " + height);

        this.width = width;
        this.height = height;
        this.ratio = (float) width / height;

        // Adjust the viewport based on geometry changes, such as screen rotation
        //GLES20.glViewport(0, 0, width, height);

        // call renderers
        for (int i = 0; i < renderers.size(); i++) {
            try {
                renderers.get(i).onSurfaceChanged(width, height);
            } catch (Exception ex) {
                Log.e(TAG, "Exception on delegate: " + renderers.get(i), ex);
            }
        }

        // update model
        screen.setSize(width, height);

        // fire event
        eventManager.propagate(new GLEvent(this, GLEvent.Code.SURFACE_CHANGED,
                this.width, this.height));
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (renderers == null || renderers.isEmpty()) {
            // scene not ready
            return;
        }

        // Default viewport
        GLES20.glViewport(0, 0, width, height);
        GLES20.glScissor(0, 0, width, height);

        // Default color
        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glColorMask(true, true, true, true);
        GLES20.glLineWidth((float) Math.PI);

        // Default blending
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // enable depth testing
        GLES20.glDepthMask(true);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // prepare listeners
        for (int i = 0; i < listeners.size(); i++) {
            try {
                //Log.e(TAG, "onPrepareFrame ("+i+"): "+listeners.get(i));
                listeners.get(i).onPrepareFrame();
            } catch (Exception ex) {
                Log.e(TAG, "Exception on delegate: " + renderers.get(i), ex);
                renderers.get(i).setEnabled(false);
            }
        }

        // call all delegates
        for (int i = 0; i < renderers.size(); i++) {
            if (!renderers.get(i).isEnabled()) {
                continue;
            }
            try {
                renderers.get(i).onDrawFrame();
            } catch (Exception ex) {
                Log.e(TAG, "Exception on delegate: " + renderers.get(i), ex);
                renderers.get(i).setEnabled(false);
            }
        }

        if (eventManager != null) {
            try {
                if (framesPerSecondTime == -1) {
                    framesPerSecondTime = SystemClock.elapsedRealtime();
                    framesPerSecondCounter++;
                } else if (SystemClock.elapsedRealtime() > framesPerSecondTime + 1000) {
                    framesPerSecond = framesPerSecondCounter;
                    framesPerSecondCounter = 1;
                    framesPerSecondTime = SystemClock.elapsedRealtime();
                    eventManager.propagate(new FPSEvent(this, framesPerSecond));
                    //Log.v(TAG, "FPS: " + framesPerSecond);
                } else {
                    framesPerSecondCounter++;
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception on fps: "+e.getMessage(), e);
            }
        }
    }

}