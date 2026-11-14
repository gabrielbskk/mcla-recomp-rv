package org.mcla.recomp;

import android.content.Context;
import android.system.Os;
import android.util.Log;

/** Optional Android driving bindings for the SDK's existing synthetic MnK pad. */
final class MclaKeyboardMouse {
    static final String PREFS = "keyboard_mouse";
    static final String KEY = "mode";
    private MclaKeyboardMouse() {}

    static int normalize(int mode) { return mode >= 0 && mode <= 2 ? mode : 0; }

    static int mode(Context context) {
        return normalize(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 0));
    }

    static String[][] overrides(int mode) {
        int selected = normalize(mode);
        if (selected == 0) return new String[][]{{"REX_MNK_MODE", "false"}, {"REX_MNK_MOUSE", "false"}};
        boolean mouse = selected == 2;
        return new String[][]{
            {"REX_MNK_MODE", "true"}, {"REX_MNK_MOUSE", mouse ? "true" : "false"},
            {"REX_KEYBIND_RIGHT_TRIGGER", "W"}, {"REX_KEYBIND_LEFT_TRIGGER", "S"},
            {"REX_KEYBIND_LSTICK_LEFT", "A"}, {"REX_KEYBIND_LSTICK_RIGHT", "D"},
            {"REX_KEYBIND_LSTICK_UP", ""}, {"REX_KEYBIND_LSTICK_DOWN", ""},
            {"REX_KEYBIND_A", mouse ? "Space,LMB" : "Space"},
            {"REX_KEYBIND_B", mouse ? "Backspace,Escape,RMB" : "Backspace,Escape"},
            {"REX_KEYBIND_X", "R"}, {"REX_KEYBIND_Y", "F"},
            {"REX_KEYBIND_LEFT_SHOULDER", "Q"}, {"REX_KEYBIND_RIGHT_SHOULDER", "E"},
            {"REX_KEYBIND_LSTICK_PRESS", "G"}, {"REX_KEYBIND_RSTICK_PRESS", "C"},
            {"REX_KEYBIND_RSTICK_UP", "I"}, {"REX_KEYBIND_RSTICK_DOWN", "K"},
            {"REX_KEYBIND_RSTICK_LEFT", "J"}, {"REX_KEYBIND_RSTICK_RIGHT", "L"},
            {"REX_KEYBIND_DPAD_UP", "Up"}, {"REX_KEYBIND_DPAD_DOWN", "Down"},
            {"REX_KEYBIND_DPAD_LEFT", "Left"}, {"REX_KEYBIND_DPAD_RIGHT", "Right"},
            {"REX_KEYBIND_START", "Return"}, {"REX_KEYBIND_BACK", "Tab"}
        };
    }

    static void apply(Context context) {
        try {
            // Clear every binding owned by this preset on same-process relaunch.
            for (String[] entry : overrides(2)) Os.unsetenv(entry[0]);
            for (String[] entry : overrides(mode(context))) Os.setenv(entry[0], entry[1], true);
            Log.i("MCLA", "Keyboard/mouse mode=" + mode(context) +
                    " (0=off, 1=keyboard, 2=keyboard+mouse); experimental driving bindings");
        } catch (Exception e) {
            for (String[] entry : overrides(2)) {
                try { Os.unsetenv(entry[0]); } catch (Exception ignored) { }
            }
            Log.e("MCLA", "Could not configure keyboard/mouse", e);
        }
    }
}
