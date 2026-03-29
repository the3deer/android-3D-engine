package org.the3deer.android.engine.services;

import android.content.Context;

import java.net.URI;

public interface Loader {
    void load(Context context, URI uri) throws Exception;
}
