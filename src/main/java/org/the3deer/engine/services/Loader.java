package org.the3deer.engine.services;

import android.content.Context;

import java.net.URL;

public interface Loader {
    void load(Context context, URL url) throws Exception;
}
