#include "fps_counter.h"

#include <algorithm>
#include <array>
#include <chrono>
#include <mutex>
#include <vector>

namespace mcla {
namespace {
constexpr size_t kCapacity = 256;
constexpr uint64_t kWindowNs = 2'000'000'000;
std::mutex mutex;
std::array<uint64_t, kCapacity> updates{};
size_t total_updates = 0;

uint64_t NowNs() {
  return std::chrono::duration_cast<std::chrono::nanoseconds>(
             std::chrono::steady_clock::now().time_since_epoch())
      .count();
}
}  // namespace

void RecordGameUpdate() {
  std::lock_guard lock(mutex);
  updates[total_updates++ % updates.size()] = NowNs();
}

GameUpdateStats GetGameUpdateStats() {
  GameUpdateStats result{};
  const uint64_t now = NowNs();
  std::lock_guard lock(mutex);
  const size_t count = std::min(total_updates, updates.size());
  std::vector<double> intervals;
  uint64_t previous = 0;
  for (size_t i = 0; i < count; ++i) {
    const uint64_t timestamp = updates[(total_updates - count + i) % updates.size()];
    if (timestamp < now - std::min(now, kWindowNs)) continue;
    if (previous && timestamp > previous) {
      intervals.push_back(double(timestamp - previous) / 1e6);
    }
    previous = timestamp;
  }
  if (intervals.empty() || now - previous > 1'000'000'000) return result;
  double sum_ms = 0;
  for (double interval : intervals) sum_ms += interval;
  result.fps = double(intervals.size()) * 1000.0 / sum_ms;
  std::sort(intervals.begin(), intervals.end());
  result.p95_ms = intervals[(intervals.size() * 95 + 99) / 100 - 1];
  result.samples = intervals.size();
  return result;
}
}  // namespace mcla
