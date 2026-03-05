package org.the3deer.android_3d_model_engine.model;

import androidx.annotation.NonNull;

public class Screen {

    public int width;
    public int height;

    public float ratio;

    public Screen(int width, int height) {
        this.setSize(width, height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        // derived
        this.ratio = (float) width / height;
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

    @NonNull
    @Override
    public String toString() {
        return "Screen{" +
                "width=" + width +
                ", height=" + height +
                ", ratio=" + ratio +
                '}';
    }
}
