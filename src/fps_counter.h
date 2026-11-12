/**
 * Clean FPS Counter for MCLA Android - Public API
 */

#pragma once

#include <cstdint>

namespace mcla {

struct FpsStats {
    uint32_t frame_count = 0;
    double avg_fps = 0.0;
    double avg_frame_ms = 0.0;
    double p50_ms = 0.0;
    double p90_ms = 0.0;
    double p95_ms = 0.0;
    double p99_ms = 0.0;
    double p1_low_ms = 0.0;   // 1% low = 99th percentile
    double p01_low_ms = 0.0;  // 0.1% low = 99.9th percentile
    double min_ms = 0.0;
    double max_ms = 0.0;
    double avg_cpu_ms = 0.0;
    double p95_cpu_ms = 0.0;
    double avg_gpu_ms = 0.0;
    double p95_gpu_ms = 0.0;
    bool is_menu = false;
};

// Call at the start of each frame (before CPU work)
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_StartFrame(uint32_t render_width, uint32_t render_height);

// Call after CPU work is done for the frame
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_EndCpuWork();

// Call when GPU work starts (command buffer submission)
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_StartGpuWork();

// Call when GPU work ends
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_EndGpuWork();

// Call when frame is presented (swapchain present)
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_Present();

// Get current statistics (menu=true for menu, false for gameplay)
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_GetStats(bool menu, FpsStats& out);

// Reset all counters
#if defined(__ANDROID__)
__attribute__((visibility("default")))
#endif
void FpsCounter_Reset();

}  // namespace mcla
