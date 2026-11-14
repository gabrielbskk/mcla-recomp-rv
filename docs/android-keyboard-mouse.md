# Android keyboard and mouse

The common Snapdragon/Adreno launcher offers **Extras → Keyboard / mouse**:
Off (default), Keyboard driving, or Keyboard + mouse camera. Select a mode
before Play and restart gameplay when changing modes.

This uses the existing rexglue SDK synthetic controller merged into guest
player 0. It is not a native PC keyboard/camera port, and does not use a second
Android event injector. No driver, audio, graphics or save edits are required.
Bindings are launch environment overrides, not TOML edits by the launcher.

| Input | Controller action |
| --- | --- |
| W / S | RT / LT (gas / brake in the standard mapping) |
| A / D | Left stick steering |
| Arrows | D-pad menus |
| Space / Backspace or Escape | A / B |
| R / F | X / Y |
| Q / E | LB / RB |
| G / C | Left / right stick press |
| Enter / Tab | Start / Back |
| I / J / K / L | Right stick camera |
| Mouse movement, in mouse mode | Right stick camera |
| Left / right click, in mouse mode | A / B |

Keys send full digital input, not pressure-sensitive triggers. The touch HUD
hides when a selected external keyboard/mouse is connected, as it does for a
physical controller. Android Home loses focus and releases SDK capture. Mouse
relative capture depends on Android/SDL/device support; fallback behavior,
focus transitions and reconnection still need broader hardware testing.

## Validation and experimental work

The user reported working mouse and keyboard on the Galaxy S23 with local
v0.1.54. This is a basic gameplay report, not validation of every peripheral,
menu-repeat, focus-loss or reconnect case. Policy tests check bindings and safe
OFF/invalid modes; Java compilation alone cannot establish working gameplay.

Run `python3 scripts/ci/test_android_keyboard_mouse.py --android-jar PATH`
with a Java runtime/compiler and Android platform JAR. Optionally pass
`--sdk-source PATH` to check cvar/key names against the exact SDK checkout.

The local RedMagic audio-queue candidate is awaiting device testing. The S23
visual-tradeoff candidate has no measured performance gain yet. Neither is
promoted to stable in this change. Cheats remain unimplemented: no verified
native setter/offset has been integrated and no save unlocks are modified.
