package org.the3deer.android.engine.util;

import org.the3deer.android.engine.util.content.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class AndroidURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("android".equals(protocol)) {
            return new org.the3deer.android.engine.util.assets.Handler();
        } else if ("content".equals(protocol)){
            return new Handler();
        }
        return null;
    }
}
