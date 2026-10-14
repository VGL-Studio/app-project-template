#!/usr/bin/env bash
# Tóm tắt kết quả test JVM từ các file JUnit XML. Trong CI, in thêm chú thích (::notice::)
# để đọc được qua API check-runs mà không cần tải log.
set -euo pipefail
cd "$(dirname "$0")/.."

total=0; failed=0; lines=()
while IFS= read -r dir; do
  module="${dir%%/build/*}"
  t=0; f=0
  for xml in "$dir"/*.xml; do
    [ -e "$xml" ] || continue
    t=$((t + $(grep -o 'tests="[0-9]*"' "$xml" | head -1 | tr -dc 0-9)))
    f=$((f + $(grep -o 'failures="[0-9]*"' "$xml" | head -1 | tr -dc 0-9) + $(grep -o 'errors="[0-9]*"' "$xml" | head -1 | tr -dc 0-9)))
  done
  total=$((total + t)); failed=$((failed + f))
  lines+=("${module}: ${t} test, ${f} lỗi")
done < <(find . -type d -path '*/build/test-results/test*UnitTest' -not -path './build-logic/*' | sort)

shots=$(find . -path '*/src/test/screenshots/*.png' -not -path '*/build/*' | wc -l | tr -d ' ')
summary="Test JVM: ${total} test, ${failed} lỗi. Ảnh screenshot: ${shots}. $(IFS='; '; echo "${lines[*]:-không có kết quả}")"
echo "$summary"
if [ -n "${GITHUB_ACTIONS:-}" ]; then
  echo "::notice title=Tóm tắt test::${summary}"
fi
