package org.the3deer.android_3d_model_engine.controller;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import org.the3deer.android_3d_model_engine.view.GLEvent;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;

import java.util.EventObject;

import javax.inject.Inject;

/**
 * <p>Improved Android Touch Screen Controller</p>
 * <p>It fires events of this type @{@link TouchEvent}</p>
 * @author Gemini AI
 */
public class TouchController implements EventListener {

    private static final String TAG = TouchController.class.getSimpleName();

    // constants
    private int width;
    private int height;

    @Inject
    private EventManager eventManager;

    // Gesture tracking state
    private float previousX1, previousY1;
    private float previousX2, previousY2;
    private float previousPinchDist;
    private double previousRotateAngle;
    
    private long lastClickTime;
    private int moveCounter;
    private boolean isActionStarted;

    public TouchController() {
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    private void fireEvent(EventObject eventObject) {
        if (eventManager != null) {
            eventManager.propagate(eventObject);
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event.getSource() instanceof MotionEvent) {
            return onMotionEvent((MotionEvent) event.getSource());
        } else if (event instanceof GLEvent) {
            GLEvent glEvent = (GLEvent) event;
            if (glEvent.getCode() == GLEvent.Code.SURFACE_CHANGED) {
                this.setSize(glEvent.getWidth(), glEvent.getHeight());
            }
        }
        return false;
    }

    public boolean onMotionEvent(MotionEvent event) {
        final int action = event.getActionMasked();
        final int pointerCount = event.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                isActionStarted = true;
                moveCounter = 0;
                lastClickTime = SystemClock.uptimeMillis();
                
                // Initialize state for the new gesture
                previousX1 = event.getX(0);
                previousY1 = event.getY(0);
                if (pointerCount >= 2) {
                    previousX2 = event.getX(1);
                    previousY2 = event.getY(1);
                    previousPinchDist = calculateDistance(previousX1, previousY1, previousX2, previousY2);
                    previousRotateAngle = calculateAngle(previousX1, previousY1, previousX2, previousY2);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (!isActionStarted) break;
                moveCounter++;

                float x1 = event.getX(0);
                float y1 = event.getY(0);
                float dx1 = x1 - previousX1;
                float dy1 = y1 - previousY1;

                if (pointerCount == 1) {
                    // Single finger move -> Rotate camera/object
                    fireEvent(new TouchEvent(this, TouchEvent.MOVE, width, height, previousX1, previousY1, x1, y1, dx1, dy1, 0, 0f));
                } else if (pointerCount >= 2) {
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);
                    float dx2 = x2 - previousX2;
                    float dy2 = y2 - previousY2;

                    // 1. PINCH (Zoom) detection
                    float currentPinchDist = calculateDistance(x1, y1, x2, y2);
                    if (Math.abs(currentPinchDist - previousPinchDist) > 2f) {
                        // Delta is positive when fingers move apart (Zoom IN)
                        float zoomDelta = currentPinchDist - previousPinchDist;
                        fireEvent(new TouchEvent(this, TouchEvent.PINCH, width, height, x1, y1, x2, y2, dx1, dy1, zoomDelta, 0f));
                        previousPinchDist = currentPinchDist;
                    }

                    // 2. ROTATE (Twist) detection
                    double currentRotateAngle = calculateAngle(x1, y1, x2, y2);
                    double angleDelta = currentRotateAngle - previousRotateAngle;
                    
                    // Handle wrap-around for angles
                    if (angleDelta > 180) angleDelta -= 360;
                    else if (angleDelta < -180) angleDelta += 360;

                    if (Math.abs(angleDelta) > 0.5) {
                        fireEvent(new TouchEvent(this, TouchEvent.ROTATE, width, height, x1, y1, x2, y2, dx1, dy1, 0, (float) Math.toRadians(angleDelta)));
                        previousRotateAngle = currentRotateAngle;
                    }
                    
                    previousX2 = x2;
                    previousY2 = y2;
                }
                
                previousX1 = x1;
                previousY1 = y1;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                if (action == MotionEvent.ACTION_UP && moveCounter < 10) {
                    // It's a click!
                    fireEvent(new TouchEvent(this, TouchEvent.CLICK, width, height, event.getX(), event.getY()));
                }
                isActionStarted = false;
                break;
        }
        return true;
    }

    private float calculateDistance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private double calculateAngle(float x1, float y1, float x2, float y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }
}
