package org.the3deer.android_3d_model_engine.model;

import android.graphics.Bitmap;

import java.util.Map;

public class Texture {

    private String file;

    private int id = -1;

    private Bitmap bitmap;

    private byte[] data;

    private Map<String, Object> extensions;

    public Texture() {
    }

    public int getId() {
        return id;
    }

    public boolean hasId(){
        return id != -1;
    }

    public Texture setId(int id) {
        this.id = id;
        return this;
    }

    public String getFile() {
        return file;
    }

    public Texture setFile(String file) {
        this.file = file;
        return this;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Texture setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        return this;
    }

    public byte[] getData() {
        return data;
    }

    public Texture setData(byte[] data) {
        this.data = data;
        return this;
    }
    public Texture setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    @Override
    public String toString() {
        return "Texture{" +
                "file='" + file + '\'' +
                ", glTextureId=" + id +
                ", data=" + data +
                ", bitmap=" + bitmap +
                '}';
    }
}
