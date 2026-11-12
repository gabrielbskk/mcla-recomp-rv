package org.mcla.recomp;

import android.os.Bundle;
import android.os.Build;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.system.Os;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.input.InputManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.View;
import android.widget.RelativeLayout;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import org.libsdl.app.SDLActivity;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;

public final class MclaActivity extends SDLActivity {
    private static final String TAG = "MCLA";
    private MclaTouchControls touchControls;
    private InputManager inputManager;
    private final InputManager.InputDeviceListener gamepadListener =
            new InputManager.InputDeviceListener() {
                @Override public void onInputDeviceAdded(int id) { updateGamepad(); }
                @Override public void onInputDeviceRemoved(int id) { updateGamepad(); }
                @Override public void onInputDeviceChanged(int id) { updateGamepad(); }
            };

    private void updateGamepad() {
        if (touchControls == null) return;
        boolean connected = false;
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id);
            if (device == null || device.isVirtual()) continue;
            int sources = device.getSources();
            if ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                    (sources & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) {
                connected = true;
                break;
            }
        }
        touchControls.setGamepadConnected(connected);
    }
    private final Handler logHandler = new Handler(Looper.getMainLooper());
    private final Runnable exportLogs = new Runnable() {
        @Override public void run() {
            MclaLogExporter.exportAsync(MclaActivity.this);
            logHandler.postDelayed(this, 60_000);
        }
    };

    @Override
    public void setRequestedOrientation(int orientation) {
        // SDL can request FULL_USER for a resizable window, which returns this
        // game to portrait when system auto-rotation is locked.
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    @Override
    protected void onCreate(Bundle state) {
        applyDriverDebugEnvironment();
        File source = new File(getApplicationInfo().nativeLibraryDir, "librexgpu-xenos.so");
        File target = new File(getFilesDir(), "librexgpu-xenos.so");
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not stage the Xenos Vulkan plugin", e);
        }
        super.onCreate(state);
        if (mLayout != null) {
            touchControls = new MclaTouchControls(this);
            mLayout.addView(touchControls, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));
            inputManager = (InputManager) getSystemService(Context.INPUT_SERVICE);
            if (inputManager != null) inputManager.registerInputDeviceListener(gamepadListener, logHandler);
            updateGamepad();
        }
        if (Build.VERSION.SDK_INT >= 24) {
            PowerManager power = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (power != null && power.isSustainedPerformanceModeSupported()) {
                getWindow().setSustainedPerformanceMode(true);
                Log.i(TAG, "Sustained performance mode enabled");
            } else {
                Log.i(TAG, "Sustained performance mode unavailable");
            }
        }
        applyImmersiveMode();
    }

    private void applyDriverDebugEnvironment() {
        // The Vulkan driver is loaded during SDLActivity.onCreate. Keep this
        // private, opt-in diagnostic setting outside mcla.toml and apply it first.
        File config = new File(getFilesDir(), "driver.env");
        String debug = null;
        try {
            if (config.exists()) {
                if (!config.isFile() || config.length() > 512) {
                    throw new IllegalArgumentException("driver.env must be a file of at most 512 bytes");
                }
                try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        if (debug != null || !line.startsWith("TU_DEBUG=")) {
                            throw new IllegalArgumentException("driver.env accepts one TU_DEBUG entry");
                        }
                        debug = line.substring("TU_DEBUG=".length());
                        if (!debug.equals("noubwc") && !debug.equals("syncdraw") &&
                                !debug.equals("flushall") && !debug.equals("nolrz") &&
                                !debug.equals("sysmem")) {
                            throw new IllegalArgumentException("unsupported TU_DEBUG value");
                        }
                    }
                }
            }
            if (debug == null) {
                // Activities can start again in the same process after changing
                // driver.env. Never carry the previous test into the next run.
                if (Os.getenv("TU_DEBUG") != null) {
                    Os.unsetenv("TU_DEBUG");
                    Log.i(TAG, "Driver debug: TU_DEBUG cleared");
                }
            } else {
                Os.setenv("TU_DEBUG", debug, true);
                Log.i(TAG, "Driver debug: TU_DEBUG=" + Os.getenv("TU_DEBUG"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not apply driver.env; no driver debug test is active", e);
            try {
                Os.unsetenv("TU_DEBUG");
            } catch (Exception clearError) {
                Log.e(TAG, "Could not clear TU_DEBUG", clearError);
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveMode();
        } else if (touchControls != null) {
            touchControls.clearInput();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGamepad();
        if (touchControls != null) touchControls.resumeControls();
        logHandler.removeCallbacks(exportLogs);
        logHandler.postDelayed(exportLogs, 30_000);
    }

    @Override
    protected void onPause() {
        logHandler.removeCallbacks(exportLogs);
        if (touchControls != null) touchControls.clearInput();
        super.onPause();
        MclaLogExporter.exportAsync(this);
    }

    @Override
    protected void onDestroy() {
        logHandler.removeCallbacks(exportLogs);
        if (inputManager != null) inputManager.unregisterInputDeviceListener(gamepadListener);
        if (touchControls != null) touchControls.clearInput();
        MclaLogExporter.exportAsync(this);
        super.onDestroy();
    }

    private void applyImmersiveMode() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        View decor = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }
}
