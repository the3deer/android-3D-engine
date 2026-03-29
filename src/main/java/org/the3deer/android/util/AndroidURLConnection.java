package org.the3deer.android.util;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

public class AndroidURLConnection extends URLConnection {

    private InputStream stream;

    public AndroidURLConnection(URL url) {
        super(url);
    }

    @Override
    public void connect() throws IOException
    {
        if (stream == null) {
            try {
                stream = ContentUtils.getInputStream(Uri.parse(url.toURI().toString()));
            } catch (URISyntaxException e) {
                Log.e("Handler", e.getMessage(), e);
                throw new IOException("Error opening stream " + url + ". " + e.getMessage());
            }
        }
    }

    @Override
    public InputStream getInputStream() throws IOException {
        connect();
        return stream;
    }
}
