package org.the3deer.android_3d_model_engine.services;

import android.content.Context;

import java.net.URI;

public interface Loader {
    void load(Context context, URI uri) throws Exception;
}
