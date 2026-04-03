package org.the3deer.engine.android.touch;

import android.view.MotionEvent;

import org.the3deer.engine.event.GLEvent;
import org.the3deer.engine.event.TouchEvent;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;

import java.util.EventObject;

import javax.inject.Inject;

/**
 * <p>Improved Android Touch Screen Controller with Sticky Gesture Filtering</p>
 * @author Gemini AI
 */
public class TouchController implements EventListener {

    private int width;
    private int height;

    @Inject
    private EventManager eventManager;

    private int primaryId = -1;
    private int secondaryId = -1;

    private float startX, startY;
    private float lastX1, lastY1;
    private float lastX2, lastY2;
    private float lastPinchDist;
    private double lastRotateAngle;
    
    private boolean isTapCandidate;
    private static final float TAP_THRESHOLD = 20f; 

    // Sticky gesture state
    private enum Gesture { NONE, ZOOM, PAN, ROTATE }
    private Gesture activeGesture = Gesture.NONE;

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
                activeGesture = Gesture.NONE;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                isTapCandidate = false;
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
                        if (totalDist > TAP_THRESHOLD) isTapCandidate = false;
                    }

                    if (sIdx == -1) {
                        fireEvent(new TouchEvent(this, TouchEvent.MOVE, width, height, lastX1, lastY1, x1, y1, dx1, dy1, 0, 0f));
                    } else {
                        float x2 = event.getX(sIdx);
                        float y2 = event.getY(sIdx);
                        float dx2 = x2 - lastX2;
                        float dy2 = y2 - lastY2;

                        // Calculate gesture dominance
                        float currentDist = calculateDistance(x1, y1, x2, y2);
                        float distDelta = Math.abs(currentDist - lastPinchDist);
                        float moveX = Math.abs(dx1 + dx2) / 2f;
                        float moveY = Math.abs(dy1 + dy2) / 2f;
                        float totalMove = moveX + moveY;

                        // Decide gesture mode if not already locked
                        if (activeGesture == Gesture.NONE) {
                            if (distDelta > totalMove && distDelta > 5f) activeGesture = Gesture.ZOOM;
                            else if (totalMove > distDelta && totalMove > 5f) activeGesture = Gesture.PAN;
                        }

                        // Execute locked gesture
                        if (activeGesture == Gesture.ZOOM) {
                            fireEvent(new TouchEvent(this, TouchEvent.PINCH, width, height, x1, y1, x2, y2, dx1, dy1, currentDist - lastPinchDist, 0f));
                            lastPinchDist = currentDist;
                        } else if (activeGesture == Gesture.PAN) {
                            fireEvent(new TouchEvent(this, TouchEvent.SPREAD, width, height, x1, y1, x2, y2, (dx1+dx2)/2f, (dy1+dy2)/2f, 0, 0f));
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
                activeGesture = Gesture.NONE; // Reset lock when finger is lifted
                break;

            case MotionEvent.ACTION_UP:
                if (isTapCandidate) {
                    fireEvent(new TouchEvent(this, TouchEvent.CLICK, width, height, event.getX(), event.getY()));
                }
            case MotionEvent.ACTION_CANCEL:
                primaryId = -1;
                secondaryId = -1;
                isTapCandidate = false;
                activeGesture = Gesture.NONE;
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
