#pragma once

#include <cstddef>

namespace mcla {
struct GameUpdateStats {
  double fps = 0;
  double p95_ms = 0;
  size_t samples = 0;
};

// Guest engine timer updates, not host presentation or display scanout.
void RecordGameUpdate();
GameUpdateStats GetGameUpdateStats();
}  // namespace mcla
