package com.wix.RNCameraKit.camera;

import android.graphics.Color;
import android.hardware.Camera;
import android.os.Handler;
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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class CameraView extends FrameLayout implements SurfaceHolder.Callback {
    private ThemedReactContext context;
    private final SurfaceView surface;
    private float lastFingerSpacing;
    private final FocusRectangle focus;
    private final Handler delay = new Handler();

    public CameraView(ThemedReactContext context) {
        super(context);
        this.context = context;

        focus = new FocusRectangle(context);
        surface = new SurfaceView(context);

        setBackgroundColor(Color.BLACK);

        addView(surface, MATCH_PARENT, MATCH_PARENT);
        addView(focus, MATCH_PARENT, MATCH_PARENT);

        surface.getHolder().addCallback(this);

        surface.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN) return true;

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
                            int x = (int) event.getX();
                            int y = (int) event.getY();

                            focus.setBounds(
                                    x - 50,
                                    y - 50,
                                    x + 50,
                                    y + 50
                            );

                            sendMessage(m("ACTIVITY", "ACTIVE"), m("BOUNDS", focus.getBounds().flattenToString()));
                            focus.setColor(FocusRectangle.FocusState.ACTIVE);

                            try {
                                camera.autoFocus(new Camera.AutoFocusCallback() {
                                    @Override
                                    public void onAutoFocus(boolean success, Camera camera) {
                                        sendMessage(m("ACTIVITY", "FOCUS"));
                                        if (success) {
                                            sendMessage(m("ACTIVITY", "SUCCESS"));
                                            focus.setColor(FocusRectangle.FocusState.SUCCESS);
                                        } else {
                                            sendMessage(m("ACTIVITY", "FAILED"));
                                            focus.setColor(FocusRectangle.FocusState.FAILED);
                                        }

                                        delay.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendMessage(m("ACTIVITY", "COMPLETE"));
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
                        return false;
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
        sendMessage(m("Event", "Zoom Complete"));
        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("ZoomComplete", callbackEvent);

        return true;
    }

    private String[] m(String a, String b) {
        return new String[]{a, b};
    }
    private void sendMessage(String[] ...plops) {
        WritableMap callbackEvent = Arguments.createMap();
        for (String[] pl : plops) {
            callbackEvent.putString(pl[0], pl[1]);
        }
        context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit("Message", callbackEvent);
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
