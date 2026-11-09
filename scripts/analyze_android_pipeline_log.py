#!/usr/bin/env python3
"""Summarize Vulkan pipeline bursts in an Android MCLA native log."""

import argparse
import collections
import re
import sys


TIMESTAMP = re.compile(r"^\[(\d{4}-\d\d-\d\d \d\d:\d\d:\d\d)\.\d+\]")
SLOW_QUERY = re.compile(
    r"(?:pipeline of a draw inside an occlusion query took|"
    r"a draw inside an occlusion query waited) (\d+) ms"
)


def summarize(lines, limit):
    creations = collections.Counter()
    texture_loads = collections.Counter()
    slow_queries = []
    cache_lines = []
    shader_pack_lines = []
    for line in lines:
        match = TIMESTAMP.match(line)
        if not match:
            continue
        second = match.group(1)
        if "Creating graphics pipeline state" in line:
            creations[second] += 1
        if "VulkanTextureCache: texture_load_direct_unscaled:" in line:
            texture_loads[second] += 1
        query = SLOW_QUERY.search(line)
        if query:
            slow_queries.append((second, int(query.group(1))))
        if (("Loaded " in line and "VkPipelineCache data" in line)
                or "graphics pipelines from Vulkan storage" in line):
            cache_lines.append(line.rstrip())
        if "shader pack:" in line:
            shader_pack_lines.append(line.rstrip())

    print(f"Graphics pipeline creations: {sum(creations.values())}")
    print(f"Direct texture loads: {sum(texture_loads.values())}")
    print(f"Occlusion-query waits/creations >= logging threshold: {len(slow_queries)}")
    for line in cache_lines:
        print(line)
    for line in shader_pack_lines:
        print(line)
    print("Busiest seconds (pipelines, textures):")
    for second, count in sorted(creations.items(), key=lambda item: (-item[1], item[0]))[:limit]:
        print(f"  {second}: {count}, {texture_loads[second]}")
    for second, duration in sorted(slow_queries, key=lambda item: -item[1])[:limit]:
        print(f"  occlusion query {second}: {duration} ms")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("log", help="Native MCLA log path, or - for stdin")
    parser.add_argument("--top", type=int, default=10, help="Number of busiest seconds to print")
    args = parser.parse_args()
    if args.top < 1:
        parser.error("--top must be positive")
    if args.log == "-":
        summarize(sys.stdin, args.top)
    else:
        with open(args.log, encoding="utf-8", errors="replace") as stream:
            summarize(stream, args.top)


if __name__ == "__main__":
    main()
