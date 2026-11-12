package org.mcla.recomp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.content.SharedPreferences;
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
    private final Map<Integer, float[]> stickOrigins = new HashMap<>();
    private final Map<Integer, float[]> editedPositions = new HashMap<>();
    private final SharedPreferences layout;
    private final boolean editing;
    private int dragPointer = -1;
    private int dragControl;
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
        this(context, false);
    }

    public MclaTouchControls(Context context, boolean editing) {
        super(context);
        this.editing = editing;
        layout = context.getSharedPreferences("touch_layout", Context.MODE_PRIVATE);
        setFocusable(false);
        if (!editing) scheduleHide();
    }

    public void setSizePercent(int percent) {
        layout.edit().putInt("size_percent", Math.max(60, Math.min(150, percent))).apply();
        invalidate();
    }

    public int getSizePercent() { return layout.getInt("size_percent", 100); }

    public void resetLayout() {
        layout.edit().clear().apply();
        editedPositions.clear();
        invalidate();
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
        stickOrigins.clear();
        if (!editing) nativeSetState(0, 0, 0, 0, 0, 0, 0);
        handler.removeCallbacks(hideIdle);
        invalidate();
    }

    public void resumeControls() {
        if (!gamepadConnected) scheduleHide();
    }

    private void scheduleHide() {
        handler.removeCallbacks(hideIdle);
        if (!editing && !gamepadConnected && fingers.isEmpty())
            handler.postDelayed(hideIdle, IDLE_MS);
    }

    private float r() {
        return Math.min(getWidth() / 2.1f, getHeight()) * .076f * getSizePercent() / 100f;
    }
    private float defaultX(int control) {
        switch (control) {
            case LEFT_STICK: return .15f;
            case RIGHT_STICK: return .72f;
            case LT: return .12f;
            case RT: return .89f;
            case 0x0100: return .26f; // LB
            case 0x0200: return .74f; // RB
            case 0x0010: return .55f; // Start
            case 0x0020: return .45f; // Back
            case 0x0040: return .245f; // LS
            case 0x0080: return .62f; // RS
            case 0x0001: case 0x0002: return .36f;
            case 0x0004: return .325f;
            case 0x0008: return .395f;
            case 0x8000: case 0x1000: return .89f; // Y, A
            case 0x4000: return .85f; // X
            case 0x2000: return .93f; // B
            default: return 0f;
        }
    }
    private float defaultY(int control) {
        switch (control) {
            case LEFT_STICK: case RIGHT_STICK: return .76f;
            case LT: case RT: return .12f;
            case 0x0100: case 0x0200: return .10f;
            case 0x0010: case 0x0020: return .18f;
            case 0x0040: case 0x0080: return .92f;
            case 0x0001: return .66f;
            case 0x0002: return .82f;
            case 0x0004: case 0x0008: return .74f;
            case 0x8000: return .42f;
            case 0x1000: return .62f;
            case 0x4000: case 0x2000: return .52f;
            default: return 0f;
        }
    }
    private float cx(int control) {
        float[] edit = editedPositions.get(control);
        return getWidth() * (edit == null
            ? layout.getFloat("x_" + control, defaultX(control)) : edit[0]);
    }
    private float cy(int control) {
        float[] edit = editedPositions.get(control);
        return getHeight() * (edit == null
            ? layout.getFloat("y_" + control, defaultY(control)) : edit[1]);
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

    private boolean editTouch(MotionEvent event) {
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN) {
            dragPointer = event.getPointerId(index);
            dragControl = hit(event.getX(index), event.getY(index));
        }
        if (dragControl != 0 && dragPointer >= 0) {
            int pointer = event.findPointerIndex(dragPointer);
            if (pointer >= 0) {
                float x = Math.max(.05f, Math.min(.95f, event.getX(pointer) / getWidth()));
                float y = Math.max(.07f, Math.min(.93f, event.getY(pointer) / getHeight()));
                editedPositions.put(dragControl, new float[] {x, y});
                invalidate();
            }
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            float[] pos = editedPositions.get(dragControl);
            if (pos != null && action == MotionEvent.ACTION_UP) {
                layout.edit().putFloat("x_" + dragControl, pos[0])
                    .putFloat("y_" + dragControl, pos[1]).apply();
            }
            editedPositions.clear();
            dragPointer = -1;
            dragControl = 0;
            invalidate();
        }
        return true;
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (editing) return editTouch(event);
        if (gamepadConnected) return false;
        int action = event.getActionMasked();
        int index = event.getActionIndex();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
            showing = true;
            int id = event.getPointerId(index);
            int control = hit(event.getX(index), event.getY(index));
            if (control != 0) {
                fingers.put(id, control);
                if (control == LEFT_STICK || control == RIGHT_STICK) {
                    stickOrigins.put(id, new float[] {event.getX(index), event.getY(index)});
                }
            }
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
                stickOrigins.remove(id);
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
                float[] origin = stickOrigins.get(finger.getKey());
                if (pos != null && origin != null) {
                    float scale = r() * 1.12f;
                    float dx = (pos[0] - origin[0]) / scale;
                    float dy = (origin[1] - pos[1]) / scale;
                    float length = (float) Math.hypot(dx, dy);
                    if (length < .08f) { dx = 0; dy = 0; }
                    else {
                        float magnitude = Math.min(1f, (length - .08f) / .92f);
                        dx = dx / length * magnitude;
                        dy = dy / length * magnitude;
                    }
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
            float baseX = cx(control), baseY = cy(control);
            float thumbX = baseX, thumbY = baseY;
            if (i < 2 && !editing) {
                for (Map.Entry<Integer, Integer> finger : fingers.entrySet()) {
                    if (finger.getValue() != control) continue;
                    float[] origin = stickOrigins.get(finger.getKey());
                    float[] pos = positions.get(finger.getKey());
                    if (origin == null || pos == null) continue;
                    baseX = origin[0];
                    baseY = origin[1];
                    float dx = pos[0] - baseX, dy = pos[1] - baseY;
                    float distance = (float) Math.hypot(dx, dy);
                    float travel = r() * .82f;
                    if (distance > travel) { dx *= travel / distance; dy *= travel / distance; }
                    thumbX = baseX + dx;
                    thumbY = baseY + dy;
                    break;
                }
            }
            paint.setColor(0x9A080B10);
            canvas.drawCircle(baseX, baseY, radius, paint);
            paint.setColor(COLORS[i]);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(2f, r() * .055f));
            canvas.drawCircle(baseX, baseY, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            if (i < 2) {
                paint.setColor(0xB048545C);
                canvas.drawCircle(thumbX, thumbY, r() * .48f, paint);
            }
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(r() * (i >= 10 && i <= 13 ? .38f : .48f));
            canvas.drawText(LABELS[i], i < 2 ? thumbX : baseX,
                (i < 2 ? thumbY : baseY) - (paint.ascent() + paint.descent()) / 2, paint);
        }
    }
}
