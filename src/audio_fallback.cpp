// Audio backend selection with a silent fallback.
//
// The game aborts its audio engine init if the host cannot create an audio
// driver, and later dereferences the engine it never created. So a driver must
// always exist. SilentAudioDriver accepts frames, discards them and paces the
// guest at real-time rate. It is used when the host has no usable output
// device, or on request with --mcla_audio=none.

#include "generated/default/mcla_init.h"

#include "mcla_app.h"

#include <atomic>
#include <chrono>
#include <memory>
#include <thread>

#include <rex/audio/audio_driver.h>
#include <rex/audio/sdl/sdl_audio_system.h>
#include <rex/cvar.h>
#include <rex/logging.h>
#include <rex/platform.h>
#include <rex/runtime.h>
#include <rex/thread.h>

#if REX_PLATFORM_ANDROID
#include <SDL3/SDL.h>
#endif

REXCVAR_DEFINE_STRING(mcla_audio, "sdl", "MCLA",
                      "Audio output: sdl (falls back to silent if no device), or none");
#if REX_PLATFORM_ANDROID
REXCVAR_DEFINE_BOOL(mcla_audio_low_latency, false, "MCLA",
                    "Android: open the audio output on the AAudio low-latency path. Off uses the normal mixer "
                    "path, which some devices need before they route any game sound (they stay silent with a "
                    "fast stream until another app opens the mixer, for example while screen recording). "
                    "Takes effect at the next start")
    .lifecycle(rex::cvar::Lifecycle::kInitOnly);
#endif

// A platform with its own audio output (the PS5 host) sets this to a function
// that makes a driver, or returns nullptr if it cannot; it is tried before
// SDL. The driver is deleted through its virtual destructor.
rex::audio::AudioDriver* (*g_mcla_platform_audio_driver)(rex::memory::Memory* memory,
                                                         rex::thread::Semaphore* semaphore) =
    nullptr;

namespace {

using rex::X_STATUS;

// One XAudio frame is 256 samples per channel at 48 kHz.
constexpr auto kFramePeriod = std::chrono::nanoseconds(256ull * 1'000'000'000ull / 48000ull);

class SilentAudioDriver : public rex::audio::AudioDriver {
 public:
  SilentAudioDriver(rex::memory::Memory* memory, rex::thread::Semaphore* semaphore)
      : AudioDriver(memory), semaphore_(semaphore), worker_([this] { Run(); }) {}

  ~SilentAudioDriver() override {
    running_ = false;
    worker_.join();
  }

  void SubmitFrame(uint32_t /*samples_ptr*/) override { ++pending_; }

 private:
  void Run() {
    auto next = std::chrono::steady_clock::now();
    while (running_) {
      next += kFramePeriod;
      std::this_thread::sleep_until(next);
      // Each consumed frame frees one slot for the guest, as a real device would.
      if (pending_.load() > 0) {
        --pending_;
        semaphore_->Release(1, nullptr);
      }
    }
  }

  rex::thread::Semaphore* semaphore_;
  std::atomic<int> pending_{0};
  std::atomic<bool> running_{true};
  std::thread worker_;
};

class FallbackAudioSystem : public rex::audio::sdl::SDLAudioSystem {
 public:
  using SDLAudioSystem::SDLAudioSystem;

  static std::unique_ptr<rex::audio::AudioSystem> Create(
      rex::runtime::FunctionDispatcher* function_dispatcher) {
    return std::make_unique<FallbackAudioSystem>(function_dispatcher);
  }

  X_STATUS CreateDriver(size_t index, rex::thread::Semaphore* semaphore,
                        rex::audio::AudioDriver** out_driver) override {
    if (REXCVAR_GET(mcla_audio) != "none") {
      if (g_mcla_platform_audio_driver) {
        if (auto* driver = g_mcla_platform_audio_driver(memory_, semaphore)) {
          platform_driver_ = driver;
          *out_driver = driver;
          return X_STATUS_SUCCESS;
        }
      }
      if (SDLAudioSystem::CreateDriver(index, semaphore, out_driver) == X_STATUS_SUCCESS) {
        return X_STATUS_SUCCESS;
      }
      REXLOG_WARN("audio: no usable output device, running silent");
    }
    *out_driver = new SilentAudioDriver(memory_, semaphore);
    return X_STATUS_SUCCESS;
  }

  void DestroyDriver(rex::audio::AudioDriver* driver) override {
    if (driver == platform_driver_) {
      platform_driver_ = nullptr;
      delete driver;
      return;
    }
    if (auto* silent = dynamic_cast<SilentAudioDriver*>(driver)) {
      delete silent;
      return;
    }
    SDLAudioSystem::DestroyDriver(driver);
  }

 private:
  rex::audio::AudioDriver* platform_driver_ = nullptr;
};

}  // namespace

std::unique_ptr<rex::system::IAudioSystem> CreateMclaAudioSystem(
    rex::runtime::FunctionDispatcher* function_dispatcher) {
#if REX_PLATFORM_ANDROID
  // SDL3 opens AAudio with AAUDIO_PERFORMANCE_MODE_LOW_LATENCY by default.
  // On some devices that fast stream is inaudible until another stream opens
  // the normal mixer, so the game only gets sound while something else is
  // recording or playing. The normal mixer path costs latency and is the safe
  // default; mcla_audio_low_latency restores the old behaviour.
  const bool low_latency = REXCVAR_GET(mcla_audio_low_latency);
  SDL_SetHint(SDL_HINT_ANDROID_LOW_LATENCY_AUDIO, low_latency ? "1" : "0");
  REXLOG_INFO("audio: Android low-latency path {} (mcla_audio_low_latency)",
              low_latency ? "enabled" : "disabled; using the normal mixer path");
#endif
  return FallbackAudioSystem::Create(function_dispatcher);
}
