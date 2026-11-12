// Android touchscreen input is a synthetic Xbox 360 pad; the SDK merges it
// with a physical controller for guest user 0. No game data is involved.
#ifdef __ANDROID__
#include <jni.h>

#include <algorithm>
#include <cstdint>
#include <cstring>
#include <memory>
#include <mutex>
#include <vector>

#include <rex/input/input_driver.h>
#include <rex/input/input_system.h>
#include <rex/runtime.h>
#include <rex/ui/virtual_key.h>

namespace {
using rex::X_RESULT;
using rex::X_STATUS;
using rex::input::X_INPUT_CAPABILITIES;
using rex::input::X_INPUT_KEYSTROKE;
using rex::input::X_INPUT_STATE;
using rex::input::X_INPUT_VIBRATION;
constexpr auto kTouchId = static_cast<rex::input::DeviceId>(0x544f5543);  // TOUC
struct TouchState {
  uint16_t buttons = 0;
  uint8_t lt = 0, rt = 0;
  int16_t lx = 0, ly = 0, rx = 0, ry = 0;
  uint32_t packet = 0;
};
std::mutex g_touch_mutex;
TouchState g_touch;

class TouchPad final : public rex::input::InputDriver {
 public:
  TouchPad() : InputDriver(nullptr, 2) {}
  X_STATUS Setup() override { return X_STATUS_SUCCESS; }
  void EnumerateDevices(std::vector<rex::input::DeviceInfo>& out) override {
    rex::input::DeviceInfo info;
    info.id = kTouchId;
    info.name = "Android touch pad";
    info.synthetic = true;
    out.push_back(info);
  }
  X_RESULT GetDeviceState(rex::input::DeviceId id, X_INPUT_STATE* out) override {
    if (id != kTouchId) return X_ERROR_DEVICE_NOT_CONNECTED;
    if (!out) return X_ERROR_BAD_ARGUMENTS;
    std::lock_guard lock(g_touch_mutex);
    std::memset(out, 0, sizeof(*out));
    out->packet_number = g_touch.packet;
    out->gamepad.buttons = g_touch.buttons;
    out->gamepad.left_trigger = g_touch.lt;
    out->gamepad.right_trigger = g_touch.rt;
    out->gamepad.thumb_lx = g_touch.lx;
    out->gamepad.thumb_ly = g_touch.ly;
    out->gamepad.thumb_rx = g_touch.rx;
    out->gamepad.thumb_ry = g_touch.ry;
    return X_ERROR_SUCCESS;
  }
  X_RESULT GetDeviceCapabilities(rex::input::DeviceId id, uint32_t,
                                 X_INPUT_CAPABILITIES* out) override {
    if (id != kTouchId) return X_ERROR_DEVICE_NOT_CONNECTED;
    if (!out) return X_ERROR_BAD_ARGUMENTS;
    std::memset(out, 0, sizeof(*out));
    out->type = 1;
    out->sub_type = 1;
    out->gamepad.buttons = 0xFFFF;
    out->gamepad.left_trigger = out->gamepad.right_trigger = 0xFF;
    out->gamepad.thumb_lx = out->gamepad.thumb_ly =
        out->gamepad.thumb_rx = out->gamepad.thumb_ry = 0x7FFF;
    return X_ERROR_SUCCESS;
  }
  X_RESULT SetDeviceVibration(rex::input::DeviceId id, X_INPUT_VIBRATION*) override {
    return id == kTouchId ? X_ERROR_SUCCESS : X_ERROR_DEVICE_NOT_CONNECTED;
  }
  X_RESULT GetDeviceKeystroke(rex::input::DeviceId id, uint32_t,
                              X_INPUT_KEYSTROKE* out) override {
    if (id != kTouchId) return X_ERROR_DEVICE_NOT_CONNECTED;
    if (!out) return X_ERROR_BAD_ARGUMENTS;
    static constexpr rex::ui::VirtualKey keys[] = {
        rex::ui::VirtualKey::kXInputPadDpadUp, rex::ui::VirtualKey::kXInputPadDpadDown,
        rex::ui::VirtualKey::kXInputPadDpadLeft, rex::ui::VirtualKey::kXInputPadDpadRight,
        rex::ui::VirtualKey::kXInputPadStart, rex::ui::VirtualKey::kXInputPadBack,
        rex::ui::VirtualKey::kXInputPadLThumbPress, rex::ui::VirtualKey::kXInputPadRThumbPress,
        rex::ui::VirtualKey::kXInputPadLShoulder, rex::ui::VirtualKey::kXInputPadRShoulder,
        rex::ui::VirtualKey::kNone, rex::ui::VirtualKey::kNone,
        rex::ui::VirtualKey::kXInputPadA, rex::ui::VirtualKey::kXInputPadB,
        rex::ui::VirtualKey::kXInputPadX, rex::ui::VirtualKey::kXInputPadY};
    std::lock_guard lock(g_touch_mutex);
    const uint16_t changed = g_touch.buttons ^ last_buttons_;
    for (unsigned i = 0; i < 16; ++i) {
      if (!(changed & (1u << i)) || keys[i] == rex::ui::VirtualKey::kNone) continue;
      bool down = (g_touch.buttons & (1u << i)) != 0;
      std::memset(out, 0, sizeof(*out));
      out->virtual_key = static_cast<uint16_t>(keys[i]);
      out->flags = down ? rex::input::X_INPUT_KEYSTROKE_KEYDOWN
                        : rex::input::X_INPUT_KEYSTROKE_KEYUP;
      last_buttons_ ^= (1u << i);
      return X_ERROR_SUCCESS;
    }
    return X_ERROR_EMPTY;
  }
 private:
  uint16_t last_buttons_ = 0;
};
}  // namespace

extern "C" JNIEXPORT void JNICALL
Java_org_mcla_recomp_MclaTouchControls_nativeSetState(JNIEnv*, jclass, jint buttons,
                                                       jint lt, jint rt, jint lx, jint ly,
                                                       jint rx, jint ry) {
  std::lock_guard lock(g_touch_mutex);
  g_touch.buttons = static_cast<uint16_t>(buttons);
  g_touch.lt = static_cast<uint8_t>(std::clamp(lt, 0, 255));
  g_touch.rt = static_cast<uint8_t>(std::clamp(rt, 0, 255));
  g_touch.lx = static_cast<int16_t>(std::clamp(lx, -32767, 32767));
  g_touch.ly = static_cast<int16_t>(std::clamp(ly, -32767, 32767));
  g_touch.rx = static_cast<int16_t>(std::clamp(rx, -32767, 32767));
  g_touch.ry = static_cast<int16_t>(std::clamp(ry, -32767, 32767));
  ++g_touch.packet;
}

void MclaTouchControlsConfigure(rex::RuntimeConfig& config) {
  auto inner = config.input_factory;
  config.input_factory = [inner](bool tool_mode) -> std::unique_ptr<rex::system::IInputSystem> {
    auto system = inner ? inner(tool_mode) : nullptr;
    if (system && !tool_mode) {
      static_cast<rex::input::InputSystem*>(system.get())->AddDriver(std::make_unique<TouchPad>());
    }
    return system;
  };
}
#endif
