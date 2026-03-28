package org.the3deer.android.engine.model;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * @author Andres Oviedo
 * @author Gemini AI
 */
public class Screen {

    public int width;
    public int height;

    public float ratio;

    // Window insets (safe area)
    public int top;
    public int bottom;
    public int left;
    public int right;

    // UI elements size
    public int toolbarHeight;
    public int bottomBarHeight;

    public Screen(int width, int height) {
        this.setSize(width, height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        // derived
        this.ratio = (float) width / height;

        Log.i("Screen", "Screen size is width: "+ width + ", height: " + height + ", ratio: "+ratio);
    }

    public void setInsets(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        Log.i("Screen", "Screen insets set: left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom);
    }

    public void setToolbarHeight(int toolbarHeight) {
        this.toolbarHeight = toolbarHeight;
        Log.i("Screen", "Toolbar height set: " + toolbarHeight);
    }

    public void setBottomBarHeight(int bottomBarHeight) {
        this.bottomBarHeight = bottomBarHeight;
        Log.i("Screen", "Bottom bar height set: " + bottomBarHeight);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public float getRatio() {
        return ratio;
    }

    public int getTop() {
        return top;
    }

    public int getBottom() {
        return bottom;
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int getToolbarHeight() {
        return toolbarHeight;
    }

    public int getBottomBarHeight() {
        return bottomBarHeight;
    }

    /**
     * Returns the height of the screen minus the top and bottom insets.
     * This represents the "safe" height for UI elements.
     */
    public int getSafeHeight() {
        return height - top - bottom;
    }

    /**
     * Returns the width of the screen minus the left and right insets.
     */
    public int getSafeWidth() {
        return width - left - right;
    }

    @NonNull
    @Override
    public String toString() {
        return "Screen{" +
                "width=" + width +
                ", height=" + height +
                ", ratio=" + ratio +
                ", insets={left=" + left + ", top=" + top + ", right=" + right + ", bottom=" + bottom + "}" +
                ", toolbarHeight=" + toolbarHeight +
                ", bottomBarHeight=" + bottomBarHeight +
                '}';
    }
}
