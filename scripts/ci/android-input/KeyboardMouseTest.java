package org.mcla.recomp;
import java.util.HashMap;
import java.util.Map;

public final class KeyboardMouseTest {
    static int checks;
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        ++checks;
    }
    static Map<String,String> map(int mode) {
        Map<String,String> result = new HashMap<>();
        for (String[] e : MclaKeyboardMouse.overrides(mode)) {
            check(e.length == 2 && e[0].startsWith("REX_"), "valid entry");
            check(result.put(e[0], e[1]) == null, "no duplicate override");
        }
        return result;
    }
    public static void main(String[] args) {
        Map<String,String> off = map(0), keys = map(1), mouse = map(2);
        check(off.size() == 2 && off.get("REX_MNK_MODE").equals("false"), "off is neutral");
        check(map(-1).equals(off) && map(3).equals(off), "invalid modes disabled");
        check(keys.get("REX_MNK_MOUSE").equals("false"), "keyboard does not capture mouse");
        check(mouse.get("REX_MNK_MOUSE").equals("true"), "mouse capture explicit");
        check(keys.get("REX_KEYBIND_RIGHT_TRIGGER").equals("W"), "gas");
        check(keys.get("REX_KEYBIND_LEFT_TRIGGER").equals("S"), "brake");
        check(keys.get("REX_KEYBIND_LSTICK_LEFT").equals("A") &&
              keys.get("REX_KEYBIND_LSTICK_RIGHT").equals("D"), "steering");
        check(keys.get("REX_KEYBIND_LSTICK_UP").isEmpty() &&
              keys.get("REX_KEYBIND_LSTICK_DOWN").isEmpty(), "no duplicate vertical stick");
        check(keys.get("REX_KEYBIND_DPAD_UP").equals("Up"), "menu navigation");
        check(mouse.get("REX_KEYBIND_A").contains("LMB") &&
              mouse.get("REX_KEYBIND_B").contains("RMB"), "SDK mouse key names");
        check(!keys.get("REX_KEYBIND_A").contains("LMB"), "mouse buttons opt-in");
        check(!mouse.containsKey("REX_MCLA_FPS") && !mouse.containsKey("TU_DEBUG"), "no graphics edits");
        for (String[] entry : MclaKeyboardMouse.overrides(2))
            System.out.println("BIND " + entry[0] + "=" + entry[1]);
        System.out.println("PASS " + checks + " keyboard/mouse policy checks");
    }
}
