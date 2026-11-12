/**
 * Clean FPS Counter for MCLA Android
 *
 * Provides per-frame timing with rolling statistics, menu/gameplay separation,
 * and detailed percentile reporting for hitch analysis.
 */

#include "fps_counter.h"

#include <algorithm>
#include <array>
#include <atomic>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <mutex>
#include <rex/cvar.h>
#include <rex/logging.h>

REXCVAR_DEFINE_BOOL(mcla_fps_counter_enabled, true, "MCLA/FPS",
                    "Enable the clean FPS counter with detailed statistics");
REXCVAR_DEFINE_INT32(mcla_fps_counter_window_frames, 120, "MCLA/FPS",
                     "Rolling window size in frames (2 seconds at 60 FPS)")
    .range(30, 600);
REXCVAR_DEFINE_INT32(mcla_fps_counter_log_interval_ms, 5000, "MCLA/FPS",
                     "Log interval in milliseconds")
    .range(1000, 60000);
REXCVAR_DEFINE_BOOL(mcla_fps_counter_separate_menu, true, "MCLA/FPS",
                    "Separate menu and gameplay statistics by render size");

namespace mcla {

namespace {

using Clock = std::chrono::steady_clock;
using TimePoint = Clock::time_point;
using Microseconds = std::chrono::microseconds;

constexpr size_t kMaxWindowFrames = 600;
constexpr uint32_t kMenuHeightThreshold = 800;

struct FrameRecord {
    uint64_t frame_number = 0;
    uint64_t cpu_start_us = 0;
    uint64_t cpu_end_us = 0;
    uint64_t gpu_start_us = 0;
    uint64_t gpu_end_us = 0;
    uint64_t present_us = 0;
    uint32_t render_width = 0;
    uint32_t render_height = 0;
    bool is_menu = false;
};

class FpsCounterImpl {
public:
    FpsCounterImpl() = default;

    void StartFrame(uint32_t render_width, uint32_t render_height) {
        if (!REXCVAR_GET(mcla_fps_counter_enabled)) return;

        FrameRecord rec;
        rec.frame_number = frame_counter_++;
        rec.cpu_start_us = NowMicros();
        rec.render_width = render_width;
        rec.render_height = render_height;
        rec.is_menu = (render_height < kMenuHeightThreshold);

        std::lock_guard lock(mutex_);
        current_frame_ = rec;
        frame_in_progress_ = true;
    }

    void EndCpuWork() {
        if (!REXCVAR_GET(mcla_fps_counter_enabled) || !frame_in_progress_) return;
        current_frame_.cpu_end_us = NowMicros();
    }

    void StartGpuWork() {
        if (!REXCVAR_GET(mcla_fps_counter_enabled) || !frame_in_progress_) return;
        current_frame_.gpu_start_us = NowMicros();
    }

    void EndGpuWork() {
        if (!REXCVAR_GET(mcla_fps_counter_enabled) || !frame_in_progress_) return;
        current_frame_.gpu_end_us = NowMicros();
    }

    void Present() {
        if (!REXCVAR_GET(mcla_fps_counter_enabled) || !frame_in_progress_) return;

        current_frame_.present_us = NowMicros();
        current_frame_.cpu_end_us = std::max(current_frame_.cpu_end_us, current_frame_.present_us);
        current_frame_.gpu_end_us = std::max(current_frame_.gpu_end_us, current_frame_.present_us);

        std::lock_guard lock(mutex_);
        AddFrame(current_frame_);
        frame_in_progress_ = false;
        MaybeLog();
    }

    void GetStats(bool menu, FpsStats& out) const {
        std::lock_guard lock(mutex_);
        ComputeStats(menu, out);
    }

    void Reset() {
        std::lock_guard lock(mutex_);
        frame_count_ = 0;
        write_index_ = 0;
        frame_counter_ = 0;
        last_log_us_ = NowMicros();
    }

private:
    mutable std::mutex mutex_;
    std::array<FrameRecord, kMaxWindowFrames> frames_{};
    size_t frame_count_ = 0;
    size_t write_index_ = 0;
    uint64_t frame_counter_ = 0;
    FrameRecord current_frame_{};
    bool frame_in_progress_ = false;
    uint64_t last_log_us_ = 0;

    uint64_t NowMicros() const {
        return std::chrono::duration_cast<Microseconds>(Clock::now().time_since_epoch()).count();
    }

    void AddFrame(const FrameRecord& frame) {
        if (frame_count_ < kMaxWindowFrames) {
            frames_[frame_count_++] = frame;
        } else {
            frames_[write_index_] = frame;
            write_index_ = (write_index_ + 1) % kMaxWindowFrames;
        }
    }

    void ComputeStats(bool menu, FpsStats& out) const {
        out = {};

        if (frame_count_ == 0) return;

        std::vector<double> frame_times_ms;
        std::vector<double> cpu_times_ms;
        std::vector<double> gpu_times_ms;
        frame_times_ms.reserve(frame_count_);
        cpu_times_ms.reserve(frame_count_);
        gpu_times_ms.reserve(frame_count_);

        for (size_t i = 0; i < frame_count_; ++i) {
            const FrameRecord& f = frames_[i];
            if (f.is_menu != menu) continue;
            if (f.present_us == 0 || f.cpu_start_us == 0) continue;

            static uint64_t last_present_us = 0;
            if (last_present_us > 0) {
                double dt_ms = double(f.present_us - last_present_us) / 1000.0;
                if (dt_ms > 0.1 && dt_ms < 1000.0) {
                    frame_times_ms.push_back(dt_ms);
                }
            }
            last_present_us = f.present_us;

            if (f.cpu_end_us > f.cpu_start_us) {
                cpu_times_ms.push_back(double(f.cpu_end_us - f.cpu_start_us) / 1000.0);
            }
            if (f.gpu_end_us > f.gpu_start_us) {
                gpu_times_ms.push_back(double(f.gpu_end_us - f.gpu_start_us) / 1000.0);
            }
        }

        if (frame_times_ms.empty()) return;

        std::sort(frame_times_ms.begin(), frame_times_ms.end());
        std::sort(cpu_times_ms.begin(), cpu_times_ms.end());
        std::sort(gpu_times_ms.begin(), gpu_times_ms.end());

        auto percentile = [](const std::vector<double>& v, double p) -> double {
            if (v.empty()) return 0.0;
            size_t idx = size_t(std::ceil(p * v.size()) - 1);
            idx = std::min(idx, v.size() - 1);
            return v[idx];
        };

        out.frame_count = frame_times_ms.size();
        out.avg_fps = 1000.0 / PercentileMean(frame_times_ms);
        out.avg_frame_ms = PercentileMean(frame_times_ms);
        out.p50_ms = percentile(frame_times_ms, 0.50);
        out.p90_ms = percentile(frame_times_ms, 0.90);
        out.p95_ms = percentile(frame_times_ms, 0.95);
        out.p99_ms = percentile(frame_times_ms, 0.99);
        out.p1_low_ms = percentile(frame_times_ms, 0.99);
        out.p01_low_ms = percentile(frame_times_ms, 0.999);
        out.min_ms = frame_times_ms.front();
        out.max_ms = frame_times_ms.back();

        if (!cpu_times_ms.empty()) {
            out.avg_cpu_ms = PercentileMean(cpu_times_ms);
            out.p95_cpu_ms = percentile(cpu_times_ms, 0.95);
        }
        if (!gpu_times_ms.empty()) {
            out.avg_gpu_ms = PercentileMean(gpu_times_ms);
            out.p95_gpu_ms = percentile(gpu_times_ms, 0.95);
        }

        out.is_menu = menu;
    }

    static double PercentileMean(const std::vector<double>& v) {
        if (v.empty()) return 0.0;
        double sum = 0.0;
        for (double x : v) sum += x;
        return sum / v.size();
    }

    void MaybeLog() {
        uint64_t now = NowMicros();
        uint32_t interval_ms = REXCVAR_GET(mcla_fps_counter_log_interval_ms);
        if (now - last_log_us_ < uint64_t(interval_ms) * 1000) return;
        last_log_us_ = now;

        FpsStats menu_stats, game_stats;
        ComputeStats(true, menu_stats);
        ComputeStats(false, game_stats);

        if (menu_stats.frame_count > 0) {
            REXLOG_INFO("FPS [MENU]: frames={} avg={:.1f} p50={:.1f}ms p95={:.1f}ms p99={:.1f}ms 1%low={:.1f}ms 0.1%low={:.1f}ms cpu={:.1f}ms gpu={:.1f}ms",
                        menu_stats.frame_count, menu_stats.avg_fps, menu_stats.p50_ms, menu_stats.p95_ms,
                        menu_stats.p99_ms, menu_stats.p1_low_ms, menu_stats.p01_low_ms,
                        menu_stats.avg_cpu_ms, menu_stats.avg_gpu_ms);
        }
        if (game_stats.frame_count > 0) {
            REXLOG_INFO("FPS [GAME]: frames={} avg={:.1f} p50={:.1f}ms p95={:.1f}ms p99={:.1f}ms 1%low={:.1f}ms 0.1%low={:.1f}ms cpu={:.1f}ms gpu={:.1f}ms",
                        game_stats.frame_count, game_stats.avg_fps, game_stats.p50_ms, game_stats.p95_ms,
                        game_stats.p99_ms, game_stats.p1_low_ms, game_stats.p01_low_ms,
                        game_stats.avg_cpu_ms, game_stats.avg_gpu_ms);
        }
    }
};

static FpsCounterImpl g_fps_counter;

}  // namespace

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_StartFrame(uint32_t render_width, uint32_t render_height) {
    g_fps_counter.StartFrame(render_width, render_height);
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_EndCpuWork() {
    g_fps_counter.EndCpuWork();
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_StartGpuWork() {
    g_fps_counter.StartGpuWork();
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_EndGpuWork() {
    g_fps_counter.EndGpuWork();
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_Present() {
    g_fps_counter.Present();
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_GetStats(bool menu, FpsStats& out) {
    g_fps_counter.GetStats(menu, out);
}

#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_Reset() {
    g_fps_counter.Reset();
}

}  // namespace mcla
