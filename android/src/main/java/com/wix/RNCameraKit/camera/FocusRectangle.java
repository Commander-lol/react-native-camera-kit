package com.wix.RNCameraKit.camera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

public final class FocusRectangle extends View {

    private static Paint INACTIVE_PAINT;
    private static Paint ACTIVE_PAINT;
    private static Paint FAILED_PAINT;
    private static Paint SUCCESS_PAINT;

    static {
        INACTIVE_PAINT = new Paint();
        INACTIVE_PAINT.setAlpha(0);
        INACTIVE_PAINT.setStrokeWidth(2);
        INACTIVE_PAINT.setStyle(Paint.Style.STROKE);

        ACTIVE_PAINT = new Paint();
        ACTIVE_PAINT.setARGB(255, 200, 200, 200);
        ACTIVE_PAINT.setStrokeWidth(2);
        ACTIVE_PAINT.setStyle(Paint.Style.STROKE);

        FAILED_PAINT = new Paint();
        FAILED_PAINT.setARGB(255, 230, 100, 100);
        FAILED_PAINT.setStrokeWidth(2);
        FAILED_PAINT.setStyle(Paint.Style.STROKE);

        SUCCESS_PAINT = new Paint();
        SUCCESS_PAINT.setARGB(255, 100, 230, 100);
        SUCCESS_PAINT.setStrokeWidth(2);
        SUCCESS_PAINT.setStyle(Paint.Style.STROKE);
    }

    private FocusState state;
    private Paint color;
    private Rect bounds;

    public FocusRectangle(Context context) {
        super(context);
        this.setEnabled(true);
        this.setClickable(false);
        this.setWillNotDraw(false);
        this.setColor(FocusState.INACTIVE);
    }

    public final void setColor(FocusState state) {
        this.state = state;
        Log.d("cameraview_rect", "Set state " + state.toString());
        switch (state) {
            case INACTIVE:
                color = INACTIVE_PAINT;
            case ACTIVE:
                color = ACTIVE_PAINT;
            case FAILED:
                color = FAILED_PAINT;
            case SUCCESS:
                color = SUCCESS_PAINT;
        }
        this.invalidate();
        this.requestLayout();
    }

    public final void setBounds(Rect bounds) {
        this.bounds = bounds;
        Log.d("cameraview_rect", "Set bounds " + bounds.flattenToString());
        this.invalidate();
    }

    public final void setBounds(int left, int top, int right, int bottom) {
        setBounds(new Rect(left, top, right, bottom));
    }

    public final Rect getBounds() {
        return bounds;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Log.d("cameraview_rect", "onDraw");

        if (this.state != FocusState.INACTIVE) {
            Log.d("cameraview_rect", "not inactive");
            canvas.drawRect(this.bounds, this.color);
        }
    }

    public static enum FocusState {
        INACTIVE,
        ACTIVE,
        FAILED,
        SUCCESS,
    }
}
