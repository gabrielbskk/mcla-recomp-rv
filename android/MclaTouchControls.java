package org.mcla.recomp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import java.util.HashMap;
import java.util.Map;

/** Xbox-style multitouch pad over the SDL surface. This is an Android-only prototype. */
public final class MclaTouchControls extends View {
    private static final long IDLE_MS = 6000;
    private static final int LEFT_STICK = 0x10000, RIGHT_STICK = 0x20000;
    private static final int LT = 0x40000, RT = 0x80000;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Map<Integer, Integer> fingers = new HashMap<>();
    private final Map<Integer, float[]> positions = new HashMap<>();
    private boolean showing = true;
    private boolean gamepadConnected;
    private final Runnable hideIdle = new Runnable() {
        @Override public void run() {
            if (!gamepadConnected && fingers.isEmpty()) {
                showing = false;
                invalidate();
            }
        }
    };

    private static native void nativeSetState(int buttons, int lt, int rt,
                                                int lx, int ly, int rx, int ry);

    public MclaTouchControls(Context context) {
        super(context);
        setFocusable(false);
        scheduleHide();
    }

    public void setGamepadConnected(boolean connected) {
        if (gamepadConnected == connected) return;
        gamepadConnected = connected;
        clearInput();
        setVisibility(connected ? GONE : VISIBLE);
        showing = !connected;
        if (!connected) scheduleHide();
        invalidate();
    }

    public void clearInput() {
        fingers.clear();
        positions.clear();
        nativeSetState(0, 0, 0, 0, 0, 0, 0);
        handler.removeCallbacks(hideIdle);
        invalidate();
    }

    public void resumeControls() {
        if (!gamepadConnected) scheduleHide();
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideIdle);
        if (!gamepadConnected && fingers.isEmpty()) handler.postDelayed(hideIdle, IDLE_MS);
    }

    private float r() { return Math.min(getWidth() / 2.1f, getHeight()) * 0.076f; }
    private float cx(int control) {
        switch (control) {
            case LEFT_STICK: return getWidth() * .15f;
            case RIGHT_STICK: return getWidth() * .72f;
            case LT: return getWidth() * .12f;
            case RT: return getWidth() * .89f;
            case 0x0100: return getWidth() * .26f; // LB
            case 0x0200: return getWidth() * .74f; // RB
            case 0x0010: return getWidth() * .55f; // Start
            case 0x0020: return getWidth() * .45f; // Back
            case 0x0040: return getWidth() * .245f; // LS
            case 0x0080: return getWidth() * .62f; // RS
            case 0x0001: case 0x0002: return getWidth() * .36f;
            case 0x0004: return getWidth() * .325f;
            case 0x0008: return getWidth() * .395f;
            case 0x8000: case 0x1000: return getWidth() * .89f; // Y, A
            case 0x4000: return getWidth() * .85f; // X
            case 0x2000: return getWidth() * .93f; // B
            default: return 0;
        }
    }
    private float cy(int control) {
        switch (control) {
            case LEFT_STICK: case RIGHT_STICK: return getHeight() * .76f;
            case LT: case RT: return getHeight() * .12f;
            case 0x0100: case 0x0200: return getHeight() * .10f;
            case 0x0010: case 0x0020: return getHeight() * .18f;
            case 0x0040: case 0x0080: return getHeight() * .92f;
            case 0x0001: return getHeight() * .66f;
            case 0x0002: return getHeight() * .82f;
            case 0x0004: case 0x0008: return getHeight() * .74f;
            case 0x8000: return getHeight() * .42f;
            case 0x1000: return getHeight() * .62f;
            case 0x4000: case 0x2000: return getHeight() * .52f;
            default: return 0;
        }
    }
    private static final int[] CONTROLS = {
        LEFT_STICK, RIGHT_STICK, LT, RT, 0x0100, 0x0200, 0x0010, 0x0020,
        0x0040, 0x0080,
        0x0001, 0x0002, 0x0004, 0x0008, 0x8000, 0x1000, 0x4000, 0x2000
    };
    private static final String[] LABELS = {
        "L", "R", "LT", "RT", "LB", "RB", "BACK", "START", "LS", "RS",
        "↑", "↓", "←", "→", "Y", "A", "X", "B"
    };
    private static final int[] COLORS = {
        0xFF888888, 0xFF888888, 0xFF777777, 0xFF777777, 0xFF777777, 0xFF777777,
        0xFF888888, 0xFF888888, 0xFF777777, 0xFF777777,
        0xFF777777, 0xFF777777, 0xFF777777, 0xFF777777,
        0xFFF4C137, 0xFF50B743, 0xFF428BD6, 0xFFDE4F4B
    };

    private int hit(float x, float y) {
        // The two sticks have a wider capture area for sliding while driving.
        for (int i = 0; i < CONTROLS.length; i++) {
            int control = CONTROLS[i];
            float dx = x - cx(control), dy = y - cy(control);
            float radius = r() * (i < 2 ? 1.7f : i >= 10 && i <= 13 ? .57f : .8f);
            if (dx * dx + dy * dy < radius * radius) return control;
        }
        return 0;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (gamepadConnected) return false;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            showing = true;
            int id = event.getPointerId(index);
            int control = hit(event.getX(index), event.getY(index));
            if (control != 0) fingers.put(id, control);
            handler.removeCallbacks(hideIdle);
        }
        if (action == MotionEvent.ACTION_CANCEL) {
            clearInput();
            scheduleHide();
        } else {
            for (int i = 0; i < event.getPointerCount(); i++) {
                int id = event.getPointerId(i);
                if (fingers.containsKey(id)) positions.put(id,
                    new float[] {event.getX(i), event.getY(i)});
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
                int id = event.getPointerId(index);
                fingers.remove(id);
                positions.remove(id);
            }
            sendState();
            scheduleHide();
        }
        invalidate();
        return true;
    }

    private void sendState() {
        int buttons = 0, lt = 0, rt = 0, lx = 0, ly = 0, rx = 0, ry = 0;
        for (Map.Entry<Integer, Integer> finger : fingers.entrySet()) {
            int control = finger.getValue();
            if (control == LT || control == RT) {
                float[] pos = positions.get(finger.getKey());
                int pressure = pos == null ? 255 : (int)(255 * Math.max(0, Math.min(1,
                    1 - (pos[1] - cy(control)) / r())));
                if (control == LT) lt = pressure;
                else rt = pressure;
            }
            else if (control == LEFT_STICK || control == RIGHT_STICK) {
                float[] pos = positions.get(finger.getKey());
                if (pos != null) {
                    float scale = r() * 1.25f;
                    float dx = (pos[0] - cx(control)) / scale;
                    float dy = (cy(control) - pos[1]) / scale;
                    float length = (float) Math.hypot(dx, dy);
                    if (length > 1f) { dx /= length; dy /= length; }
                    if (control == LEFT_STICK) { lx = (int)(dx * 32767); ly = (int)(dy * 32767); }
                    else { rx = (int)(dx * 32767); ry = (int)(dy * 32767); }
                }
            } else buttons |= control;
        }
        nativeSetState(buttons, lt, rt, lx, ly, rx, ry);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!showing || gamepadConnected) return;
        for (int i = 0; i < CONTROLS.length; i++) {
            int control = CONTROLS[i];
            float radius = r() * (i < 2 ? 1.28f : i >= 10 && i <= 13 ? .52f : .66f);
            paint.setColor(0x9A080B10);
            canvas.drawCircle(cx(control), cy(control), radius, paint);
            paint.setColor(COLORS[i]);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, r() * .055f));
            canvas.drawCircle(cx(control), cy(control), radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(r() * (i >= 10 && i <= 13 ? .38f : .48f));
            canvas.drawText(LABELS[i], cx(control),
                cy(control) - (paint.ascent() + paint.descent()) / 2, paint);
        }
    }
}
