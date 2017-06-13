package com.wix.RNCameraKit.camera;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.wix.RNCameraKit.EventManager;

import java.util.Date;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class CameraView extends FrameLayout implements SurfaceHolder.Callback {
    static final int LONG_PRESS_TIME = 750;

    private ThemedReactContext context;
    private final SurfaceView surface;
    private float lastFingerSpacing;
    private long lastTouchStarted;
    private long lastTouchEnded;
    private final FocusRectangle focus;
    private final Handler delay = new Handler();
    private final EventManager manager;

    public CameraView(final ThemedReactContext context) {
        super(context);
        this.context = context;

        manager = new EventManager(context);
        focus = new FocusRectangle(context);
        surface = new SurfaceView(context);

        setBackgroundColor(Color.BLACK);

        addView(surface, MATCH_PARENT, MATCH_PARENT);
        addView(focus, MATCH_PARENT, MATCH_PARENT);

        surface.getHolder().addCallback(this);

        surface.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN) {
                    manager.sendEventWithLocation("OnPressStart", event);
                    lastTouchStarted = new Date().getTime();
                    final ThemedReactContext cachedContext = context;
                    delay.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // User hasn't completed last touch and is only using one finger (otherwise triggers while zooming)
                            if (lastTouchEnded < lastTouchStarted && event.getPointerCount() == 1) {
                                manager.sendEventWithLocation("OnHold", event);
                                Vibrator v = (Vibrator) cachedContext.getSystemService(Context.VIBRATOR_SERVICE);
                                v.vibrate(50);
                            }
                        }
                    }, LONG_PRESS_TIME);
                    return true;
                }

                if (CameraViewManager.getCamera() != null) {
                    Camera camera = CameraViewManager.getCamera();
                    Camera.Parameters params = camera.getParameters();

                    if (event.getPointerCount() > 1) { // Pinch
                        if (action == MotionEvent.ACTION_POINTER_DOWN) {
                            lastFingerSpacing = getFingerSpacing(event);
                            return true;
                        } else if (action == MotionEvent.ACTION_MOVE && params.isZoomSupported()) {
                            camera.cancelAutoFocus();
                            return handleZoom(event, params);
                        }
                        return false;
                    } else { // Tap
                        if (action == MotionEvent.ACTION_UP) {
                            lastTouchEnded = new Date().getTime();

                            if (lastTouchEnded - lastTouchStarted >= LONG_PRESS_TIME) {
                                manager.sendEventWithLocation("OnLongPress", event);
                                return true;
                            }

                            manager.sendEventWithLocation("OnPress", event);
                            return handleAutoFocus(v, event);
                        }
                    }
                }
                return false;
            }
        });
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int actualPreviewWidth = getResources().getDisplayMetrics().widthPixels;
        int actualPreviewHeight = getResources().getDisplayMetrics().heightPixels;
        surface.layout(0, 0, actualPreviewWidth, actualPreviewHeight);
        CameraViewManager.updateOrientation();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        CameraViewManager.setCameraView(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CameraViewManager.setCameraView(this);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        CameraViewManager.removeCameraView();
    }

    private boolean handleZoom(MotionEvent event, Camera.Parameters params) {
        Camera camera = CameraViewManager.getCamera();
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newFingerSpacing = getFingerSpacing(event);

        if (newFingerSpacing > lastFingerSpacing) {
            if (zoom < maxZoom) { // Zoom In
                zoom++;
            }
        } else if (newFingerSpacing < lastFingerSpacing) {
            if (zoom > 0) { // Zoom Out
                zoom--;
            }
        }

        lastFingerSpacing = newFingerSpacing;
        params.setZoom(zoom);
        camera.setParameters(params);

        WritableMap callbackEvent = Arguments.createMap();
        callbackEvent.putInt("current", params.getZoom());
        callbackEvent.putInt("maximum", params.getMaxZoom());
        manager.sendEvent("ZoomComplete", callbackEvent);

        return true;
    }

    private boolean handleAutoFocus(View v, MotionEvent event) {
        Camera camera = CameraViewManager.getCamera();

        int x = (int) event.getX();
        int y = (int) event.getY();

        focus.invalidate();

        focus.setBounds(
                x - 50,
                y - 50,
                x + 50,
                y + 50
        );

        manager.sendEventWithPairs("Message", m("ACTIVITY", "ACTIVE"), m("BOUNDS", focus.getBounds().flattenToString()));
        focus.setColor(FocusRectangle.FocusState.ACTIVE);

        try {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    manager.sendEventWithPairs("Message", m("ACTIVITY", "FOCUS"));
                    if (success) {
                        manager.sendEventWithPairs("Message", m("ACTIVITY", "SUCCESS"));
                        focus.setColor(FocusRectangle.FocusState.SUCCESS);
                    } else {
                        manager.sendEventWithPairs("Message", m("ACTIVITY", "FAILED"));
                        focus.setColor(FocusRectangle.FocusState.FAILED);
                    }

                    delay.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            manager.sendEventWithPairs("Message", m("ACTIVITY", "COMPLETE"));
                            focus.setColor(FocusRectangle.FocusState.INACTIVE);
                        }
                    }, 200);
                }
            });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String[] m(String a, String b) {
        return new String[]{a, b};
    }

    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    public SurfaceHolder getHolder() {
        return surface.getHolder();
    }
}
