package org.the3deer.android.util;

import org.the3deer.android.util.content.Handler;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class AndroidURLStreamHandlerFactory implements URLStreamHandlerFactory {

    @Override
    public URLStreamHandler createURLStreamHandler(String protocol) {
        if ("android".equals(protocol)) {
            return new org.the3deer.android.util.assets.Handler();
        } else if ("content".equals(protocol)){
            return new Handler();
        }
        return null;
    }
}
