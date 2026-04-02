package org.the3deer.android.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dalvik.system.ZipPathValidator;

public class ContentUtils {

    private static final String TAG = ContentUtils.class.getSimpleName();

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ZipPathValidator.clearCallback();
        }
    }

    public interface ContentResolver {
        Uri resolveUri(Uri uri);
    }

    public static final String MODELS_FOLDER = "models";
    public static final Map<String, Uri> documentsProvided = new HashMap<>();
    private static final Map<Uri, byte[]> binariesProvided = new HashMap<>();

    private static Context context = null;
    private static File currentDir = null;
    private static ContentResolver contentResolver = null;

    public static void setContext(@NonNull Context context) {
        // register
        ContentUtils.context = context;
    }

    public static void setContentResolver(ContentResolver contentResolver) {
        ContentUtils.contentResolver = contentResolver;
    }

    public static void setCurrentDir(File file) {
        ContentUtils.currentDir = file;
    }

    public static void clearDocumentsProvided() {
        documentsProvided.clear();
        binariesProvided.clear();
    }

    public static void provideAssets(Activity activity) {
        documentsProvided.clear();
        provideAssets(activity, MODELS_FOLDER);
    }

    private static void provideAssets(Activity activity, String directory) {
        try {
            final String[] files = activity.getAssets().list(directory);
            if (files != null && files.length > 0) {
                for (String document : files) {
                    final String[] files2 = activity.getAssets().list(directory + "/" + document);
                    if (files2 == null) continue;
                    if (files2.length == 0) {
                        documentsProvided.put(document, Uri.parse("android://" + activity.getPackageName() + "/assets/models/" + document));
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public static void addUri(String name, Uri uri) {
        documentsProvided.put(name, uri);
    }

    public static void addData(Uri uri, byte[] data) {
        binariesProvided.put(uri, data);
    }

    public static byte[] getData(Uri uri) {
        return binariesProvided.get(uri);
    }

    public static InputStream getInputStream(Uri uri) throws IOException {
        if (uri == null) throw new IllegalArgumentException("uri cannot be null");

        if (context == null){
            throw new IllegalStateException("There is no context configured. Did you call #setContext() before?");
        }
        final byte[] data = getData(uri);
        if (data != null){
            Log.i("ContentUtils", "Returning binary: " + uri);
            return new ByteArrayInputStream(data);
        }

        Log.v("ContentUtils", "Opening stream ..." + uri);
        if (uri.getScheme().equals("android")) {
            if (uri.getPath().startsWith("/binary/")) {
                final String path = uri.getPath().substring("/binary/".length());
                byte[] buf = binariesProvided.get(uri);
                if (buf != null) return new ByteArrayInputStream(buf, 0, buf.length);
                else throw new FileNotFoundException("File not found: " + path);
            }
            else if (uri.getPath().startsWith("/assets/")) {
                final String path = uri.getPath().substring("/assets/".length());
                Log.d("ContentUtils", "Opening asset: " + path);
                return context.getAssets().open(path);
            } else if (uri.getPath().startsWith("/res/drawable/")){
                final String path = uri.getPath().substring("/res/drawable/".length()).replace(".png","");
                Log.d("ContentUtils", "Opening drawable: " + path);
                final int resourceId = context.getResources()
                        .getIdentifier(path, "drawable", context.getPackageName());
                return context.getResources().openRawResource(resourceId);
            } else {
                throw new IllegalArgumentException("unknown android path: "+uri.getPath());
            }
        }
        else if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
            return new BufferedInputStream(new URL(uri.toString()).openConnection().getInputStream(),8192);
        }

        // Handle content:// or file://
        try {
            Uri finalUri = uri;
            if (uri.getScheme().equals("content") && documentsProvided.containsKey(uri.toString())) {
                finalUri = documentsProvided.get(uri.toString());
            }
            return new BufferedInputStream(context.getContentResolver().openInputStream(finalUri), 8192);
        } catch (FileNotFoundException | SecurityException e) {
            Log.w(TAG, "Access denied or file not found for " + uri + ". Attempting resolution...");

            // If we have a resolver and we are not on the main thread, we can try to resolve it
            if (contentResolver != null && Looper.myLooper() != Looper.getMainLooper()) {
                final CountDownLatch latch = new CountDownLatch(1);
                final AtomicReference<Uri> resolvedUri = new AtomicReference<>();

                // Ask the resolver to find the file
                // This usually triggers a UI prompt, so it must return via a callback or similar mechanism
                // For simplicity, we assume resolveUri might block or trigger an async operation
                new Thread(() -> {
                    try {
                        resolvedUri.set(contentResolver.resolveUri(uri));
                    } finally {
                        latch.countDown();
                    }
                }).start();

                try {
                    if (latch.await(60, TimeUnit.SECONDS) && resolvedUri.get() != null) {
                        Log.i(TAG, "Successfully resolved URI: " + resolvedUri.get());
                        // Cache the resolution for next time
                        addUri(uri.toString(), resolvedUri.get());
                        // Retry opening the stream with the new URI
                        return new BufferedInputStream(context.getContentResolver().openInputStream(resolvedUri.get()),8192) ;
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for URI resolution", ie);
                }
            }

            throw new IOException("Failed to open " + uri, e);
        }
    }

    public static List<String> readLines(String url) {
        List<String> ret = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                ret.add(line);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading lines from " + url, e);
        }
        return ret;
    }

    public static String read(URL url) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    public static Map<String, byte[]> readFiles(URL url) {
        Map<String, byte[]> ret = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(url.openStream()))) {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String name = ze.getName();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int readed;
                while ((readed = zis.read(buffer)) > 0) {
                    bos.write(buffer, 0, readed);
                }
                ret.put(name, bos.toByteArray());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading zip from " + url, e);
        }
        return ret;
    }

    public static void showDialog(Context context, String title, String message, String pos, String neg, DialogInterface.OnClickListener listener) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(pos, listener)
                        .setNegativeButton(neg, listener)
                        .show();
            });
        }
    }

    public static void showListDialog(Context context, String title, String[] items, DialogInterface.OnClickListener listener) {
        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(() -> {
                new AlertDialog.Builder(context)
                        .setTitle(title)
                        .setItems(items, listener)
                        .show();
            });
        }
    }

    @SuppressLint("Range")
    public static String getFileName(Context context, Uri uri) throws IOException {
        if (uri == null) return null;
        String result = null;
        if ("content".equals(uri.getScheme()) && context != null) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            } catch (SecurityException ex){
                Log.w(TAG, "Access denied or file not found for " + uri + ". Attempting resolution...");

                // If we have a resolver and we are not on the main thread, we can try to resolve it
                if (contentResolver != null && Looper.myLooper() != Looper.getMainLooper()) {
                    final CountDownLatch latch = new CountDownLatch(1);
                    final AtomicReference<Uri> resolvedUri = new AtomicReference<>();

                    // Ask the resolver to find the file
                    // This usually triggers a UI prompt, so it must return via a callback or similar mechanism
                    // For simplicity, we assume resolveUri might block or trigger an async operation
                    new Thread(() -> {
                        try {
                            resolvedUri.set(contentResolver.resolveUri(uri));
                        } finally {
                            latch.countDown();
                        }
                    }).start();

                    try {
                        if (latch.await(60, TimeUnit.SECONDS) && resolvedUri.get() != null) {
                            Log.i(TAG, "Successfully resolved URI: " + resolvedUri.get());
                            // Cache the resolution for next time
                            addUri(uri.toString(), resolvedUri.get());
                            // Retry opening the stream with the new URI
                            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                                if (cursor != null && cursor.moveToFirst()) {
                                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                                    if (index != -1) {
                                        result = cursor.getString(index);
                                    }
                                }
                            }
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting for URI resolution", ie);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying filename for " + uri, e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result;
    }



    public static InputStream getInputStream(String uriString) throws IOException {
        uriString = uriString.replace('\\','/');
        Uri uri = getUri(uriString);
        if (uri == null) uri = getUri("models/"+uriString);
        if (uri == null) uri = getUri("models/"+uriString.replace(' ', '+'));
        if (uri == null && currentDir != null) {
            uri = Uri.parse("file://" + new File(currentDir, uriString).getAbsolutePath());
        }
        if (uri != null) return getInputStream(uri);
        else {
            uri = Uri.parse(uriString);
            InputStream inputStream = getInputStream(uri);
            if (inputStream != null) return inputStream;
        }

        // If we have a resolver and we are not on the main thread, we can try to resolve it
        if (contentResolver != null && Looper.myLooper() != Looper.getMainLooper()) {
            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<Uri> resolvedUri = new AtomicReference<>();

            // Ask the resolver to find the file
            // This usually triggers a UI prompt, so it must return via a callback or similar mechanism
            // For simplicity, we assume resolveUri might block or trigger an async operation
            Uri finalUri = uri;
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            new Thread(() -> {
                try {
                    Log.i(TAG, "Resolving URI: " + stackTrace[0].getMethodName());
                    resolvedUri.set(contentResolver.resolveUri(finalUri));
                } finally {
                    latch.countDown();
                }
            }).start();

            try {
                if (latch.await(60, TimeUnit.SECONDS) && resolvedUri.get() != null) {
                    Log.i(TAG, "Successfully resolved URI: " + resolvedUri.get());
                    // Cache the resolution for next time
                    addUri(uri.toString(), resolvedUri.get());


                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for URI resolution", ie);
            }
        }
        Log.e(TAG, "File not found: " + uriString);
        throw new FileNotFoundException("File not found: " + uriString);
    }

    public static Uri getUri(String name) {
        Uri uri = documentsProvided.get(name);
        if (uri != null) return uri;
        return documentsProvided.get(name.replaceAll("\\\\","/"));
    }
}
