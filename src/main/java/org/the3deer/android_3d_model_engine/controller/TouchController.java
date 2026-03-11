package org.the3deer.android_3d_model_engine.controller;

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

    private int width;
    private int height;

    @Inject
    private EventManager eventManager;

    // Pointer ID tracking
    private int primaryId = -1;
    private int secondaryId = -1;

    // Gesture tracking state
    private float startX, startY;
    private float lastX1, lastY1;
    private float lastX2, lastY2;
    private float lastPinchDist;
    private double lastRotateAngle;
    
    private boolean isTapCandidate;
    private static final float TAP_THRESHOLD = 20f; // Pixels

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
        final int index = event.getActionIndex();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                primaryId = event.getPointerId(0);
                startX = lastX1 = event.getX(0);
                startY = lastY1 = event.getY(0);
                isTapCandidate = true;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isTapCandidate = false; // Multiple fingers -> Not a tap
                if (secondaryId == -1) {
                    secondaryId = event.getPointerId(index);
                    lastX2 = event.getX(index);
                    lastY2 = event.getY(index);
                    
                    int pIdx = event.findPointerIndex(primaryId);
                    if (pIdx != -1) {
                        lastX1 = event.getX(pIdx);
                        lastY1 = event.getY(pIdx);
                        lastPinchDist = calculateDistance(lastX1, lastY1, lastX2, lastY2);
                        lastRotateAngle = calculateAngle(lastX1, lastY1, lastX2, lastY2);
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                int pIdx = event.findPointerIndex(primaryId);
                int sIdx = (secondaryId != -1) ? event.findPointerIndex(secondaryId) : -1;

                if (pIdx != -1) {
                    float x1 = event.getX(pIdx);
                    float y1 = event.getY(pIdx);
                    float dx1 = x1 - lastX1;
                    float dy1 = y1 - lastY1;

                    if (isTapCandidate) {
                        float totalDist = (float) Math.sqrt(Math.pow(x1 - startX, 2) + Math.pow(y1 - startY, 2));
                        if (totalDist > TAP_THRESHOLD) {
                            isTapCandidate = false;
                        }
                    }

                    if (sIdx == -1) {
                        // Single finger move
                        fireEvent(new TouchEvent(this, TouchEvent.MOVE, width, height, lastX1, lastY1, x1, y1, dx1, dy1, 0, 0f));
                    } else {
                        // Two finger gestures
                        float x2 = event.getX(sIdx);
                        float y2 = event.getY(sIdx);

                        float currentDist = calculateDistance(x1, y1, x2, y2);
                        if (Math.abs(currentDist - lastPinchDist) > 2f) {
                            float zoomDelta = currentDist - lastPinchDist;
                            fireEvent(new TouchEvent(this, TouchEvent.PINCH, width, height, x1, y1, x2, y2, dx1, dy1, zoomDelta, 0f));
                            lastPinchDist = currentDist;
                        }

                        double currentAngle = calculateAngle(x1, y1, x2, y2);
                        double angleDelta = currentAngle - lastRotateAngle;
                        if (angleDelta > 180) angleDelta -= 360;
                        else if (angleDelta < -180) angleDelta += 360;

                        if (Math.abs(angleDelta) > 0.5) {
                            fireEvent(new TouchEvent(this, TouchEvent.ROTATE, width, height, x1, y1, x2, y2, dx1, dy1, 0, (float) Math.toRadians(angleDelta)));
                            lastRotateAngle = currentAngle;
                        }
                        lastX2 = x2;
                        lastY2 = y2;
                    }
                    lastX1 = x1;
                    lastY1 = y1;
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                int upId = event.getPointerId(index);
                if (upId == primaryId) {
                    primaryId = secondaryId;
                    secondaryId = -1;
                    int newPIdx = event.findPointerIndex(primaryId);
                    if (newPIdx != -1) {
                        lastX1 = event.getX(newPIdx);
                        lastY1 = event.getY(newPIdx);
                    }
                } else if (upId == secondaryId) {
                    secondaryId = -1;
                }
                break;

            case MotionEvent.ACTION_UP:
                if (isTapCandidate) {
                    fireEvent(new TouchEvent(this, TouchEvent.CLICK, width, height, event.getX(), event.getY()));
                }
                // Fall through
            case MotionEvent.ACTION_CANCEL:
                primaryId = -1;
                secondaryId = -1;
                isTapCandidate = false;
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
