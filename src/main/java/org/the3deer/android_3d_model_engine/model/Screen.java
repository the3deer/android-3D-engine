package org.the3deer.android_3d_model_engine.model;

public class Screen {

    public int width;
    public int height;

    public float ratio;
    private Dimensions dimensions;

    public Screen(int width, int height) {
        this.setSize(width, height);
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
        // derived
        this.ratio = (float) width / height;
        this.dimensions = new Dimensions(0, width, 0, height, 0, 0);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /*public Dimensions getDimensions(){
        return dimensions;
    }*/

    public float getRatio() {
        return ratio;
    }
}
