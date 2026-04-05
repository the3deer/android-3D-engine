package org.the3deer.android.engine.services;

import android.content.Context;

import java.net.URL;

public interface Loader {
    void load(Context context, URL url) throws Exception;
}
