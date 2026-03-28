package org.the3deer.android.engine.model;

import android.graphics.Bitmap;
import android.net.Uri;

import java.nio.ByteBuffer;
import java.util.Map;

public class Texture {

    private String name;

    private String file;
    private Uri uri;

    private int id = -1;

    private ByteBuffer buffer;

    private Bitmap bitmap;

    private byte[] data;

    private CubeMap cubeMap;

    private Map<String, Object> extensions;

    public Texture() {
    }

    public Texture(String file) {
        this.file = file;
    }

    public Texture(String s, Bitmap bitmap) {
        this.name = s;
        this.bitmap = bitmap;
    }

    public String getName() {
        return name;
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

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
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


    public Texture setBuffer(ByteBuffer imageData) {
        this.buffer = imageData;
        return this;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public CubeMap getCubeMap() {
        return cubeMap;
    }

    public Texture setCubeMap(CubeMap cubeMap) {
        this.cubeMap = cubeMap;
        return this;
    }

    public boolean isCubeMap() {
        return cubeMap != null;
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
                ", cubeMap=" + cubeMap +
                '}';
    }

}
