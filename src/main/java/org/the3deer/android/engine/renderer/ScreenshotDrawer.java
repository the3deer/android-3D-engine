package org.the3deer.android.engine.renderer;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.util.Log;

import org.the3deer.android.engine.ModelEngineViewModel;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Responsible for capturing the current GL frame and fulfilling screenshot requests.
 */
@Bean
public class ScreenshotDrawer implements RenderListener {

    private static final String TAG = ScreenshotDrawer.class.getSimpleName();

    @Inject
    private ModelEngineViewModel viewModel;

    @Inject
    private Screen screen;

    @Override
    public void onPostFrame() {
        if (viewModel == null) return;

        final CompletableFuture<Bitmap> request = viewModel.getScreenshotRequest();
        if (request != null && !request.isDone()) {
            try {
                final Bitmap bitmap = captureScreenshot(screen.getWidth(), screen.getHeight());
                viewModel.fulfillScreenshotRequest(bitmap);
            } catch (Exception e) {
                Log.e(TAG, "Failed to capture screenshot", e);
            }
        }
    }

    private Bitmap captureScreenshot(int width, int height) {
        final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
        buffer.order(ByteOrder.nativeOrder());
        GLES20.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer);

        final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        buffer.rewind();
        bitmap.copyPixelsFromBuffer(buffer);

        // OpenGL uses bottom-left as (0,0), Android uses top-left. Flip it vertically.
        final android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.preScale(1.0f, -1.0f);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }
}
