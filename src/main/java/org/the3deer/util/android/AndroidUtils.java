package org.the3deer.util.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.the3deer.util.event.EventListener;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.EventObject;
import java.util.List;
import java.util.Set;

import de.javagl.jgltf.model.io.Buffers;


public class AndroidUtils {

    public static Bitmap decodeBitmap(InputStream is) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // By default, Android applies pre-scaling to bitmaps depending on the resolution of your device and which
        // resource folder you placed the image in. We don’t want Android to scale our bitmap at all, so to be sure,
        // we set inScaled to false.
        options.inScaled = false;

        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
        if (bitmap == null) {
            throw new RuntimeException("couldn't load bitmap");
        }
        return bitmap;
    }

    public static String decodeMimeType(ByteBuffer imageData) {
        String mimeType = null;
        try (InputStream imageIS = Buffers.createByteBufferInputStream(imageData)){
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imageIS, null, opt);
            mimeType = opt.outMimeType;
        } catch (Exception e) {
            // ignore
        }
        return mimeType;
    }

    @FunctionalInterface
    public interface Callback {
        void onClick(File file);
    }

    public static URL createURL(String url) {
        try {
            return new URL(url);
        } catch(MalformedURLException e){
            throw new RuntimeException(e);
        }
    }

    public static void logd(String sb){
        sb = sb.replaceAll("android_asset", System.getProperty("user.local.dir")+"/app/src/main/assets");
        if (sb.length() > 4000) {
            int chunkCount = sb.length() / 4000;     // integer division
            for (int i = 0; i <= chunkCount; i++) {
                int max = 4000 * (i + 1);
                if (max >= sb.length()) {
                    Log.d("AndroidUtils", sb.substring(4000 * i));
                } else {
                    Log.d("AndroidUtils", sb.substring(4000 * i, max));
                }
            }
        } else {
            Log.d("AndroidUtils", sb);
        }
    }

    /**
     * Fires the event to all listeners
     * @param listeners the listener list
     * @param eventObject the event
     * @return true if the event was handled, false otherwise
     */
    public static boolean fireEvent(Set<EventListener> listeners, EventObject eventObject){
        for (EventListener listener : listeners) {
            if(listener.onEvent(eventObject)){
                return true;
            }
        }
        return false;
    }

    public static boolean fireEvent(List<EventListener> listeners, EventObject eventObject){
        if (listeners != null) {
            for (EventListener listener : listeners) {
                //Log.v("AndroidUtils", "onEvent -> listener: " + listener+", event: "+eventObject);
                if (listener.onEvent(eventObject)) {
                    //Log.v("AndroidUtils", "onEvent -> event: "+eventObject +", listener: " + listener.getClass());
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean supportsMultiTouch(PackageManager packageManager) {
        boolean ret = false;
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)) {
            ret = true;
            Log.i("utils", "System supports multitouch (2 fingers)");
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)) {
            Log.i("utils", "System supports advanced multitouch (multiple fingers)");
            ret = true;
        }
        return ret;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static boolean checkPermission(Activity context, String permission, int callback) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            return true;
        }
        if (checkPermission(context, permission)) {
            return true;
        }
        requestPermission(context, permission, callback);
        return false;
    }

    public static boolean checkPermission(Activity context, String permission) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, permission);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermission(Activity context, String permission, int callback) {
        ActivityCompat.requestPermissions(context, new String[]{permission}, callback);
    }

    public static void openUrl(Activity activity, String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        activity.startActivity(i);
    }

    // url = file path or whatever suitable URL you want.
    public static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}