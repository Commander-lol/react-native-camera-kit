package com.wix.RNCameraKit;

import android.view.MotionEvent;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class EventManager {
    private final ThemedReactContext context;

    public EventManager(ThemedReactContext context) {
        this.context = context;
    }

    public final void sendEvent(String name) {
        WritableMap args = Arguments.createMap();
        sendEvent(name, args);
    }

    public final void sendEvent(String name, WritableMap args) {
        getEventEmitter().emit(name, args);
    }

    public final void sendEventWithPairs(String name, String[] ...plops) {
        WritableMap payload = Arguments.createMap();
        for (String[] pl : plops) {
            payload.putString(pl[0], pl[1]);
        }
        sendEvent(name, payload);
    }

    public final void sendEventWithLocation(String name, MotionEvent event) {
        WritableMap locationPayload = Arguments.createMap();
        locationPayload.putDouble("x", (double) event.getX());
        locationPayload.putDouble("y", (double) event.getY());
        sendEvent(name, locationPayload);
    }

    private DeviceEventManagerModule.RCTDeviceEventEmitter getEventEmitter() {
        return context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    }
}
