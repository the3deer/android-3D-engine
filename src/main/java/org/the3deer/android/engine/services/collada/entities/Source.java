package org.the3deer.android.engine.services.collada.entities;// You can create this as a private static inner class inside ColladaParser

public class Source {
    private final String id;
    public final float[] floatData;
    private final String[] stringData; // NEW
    public final int stride;

    public Source(String id, float[] floatData, String[] stringData, int stride) {
        this.id = id;
        this.floatData = floatData;
        this.stringData = stringData; // NEW
        this.stride = stride;
    }

    public String getId() { return id; }
    public float[] getFloatData() { return floatData; }
    public String[] getStringData() { return stringData; } // NEW
    public int getStride() { return stride; }
}

