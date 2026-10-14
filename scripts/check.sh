#!/usr/bin/env bash
# Cổng chất lượng chạy trên JVM (ADR-0010, ADR-0011). Dùng chung cho:
#   - AI ở bước 5 của phiên tự chạy (kiểm tra cục bộ),
#   - job build-and-test của .github/workflows/pr-check.yml,
#   - .github/workflows/main-guard.yml.
# Test trên emulator không nằm ở đây; chúng chạy riêng trong job ui-test của CI.
#
# Screenshot test xét theo TỪNG module có test chụp màn hình:
#   - có ảnh chuẩn trong src/test/screenshots: so sánh;
#   - chưa có ảnh chuẩn: trong CI thì báo lỗi (ảnh chuẩn phải được commit cùng PR),
#     chạy cục bộ thì ghi ảnh chuẩn mới để người xem và commit.
#
# Mọi việc chạy trong MỘT lần gọi Gradle, nên unit test chỉ chạy một lần
# (task Roborazzi dùng lại chính lần chạy unit test đó).
#
# Thay đổi file này là thay đổi cổng chất lượng: plan rủi ro cao, bắt buộc duyệt chéo.

set -euo pipefail
cd "$(dirname "$0")/.."

in_ci() { [ -n "${GITHUB_ACTIONS:-}" ]; }

screenshot_tasks=()
recorded=()
missing=()
for dir in core/*/ feature/*/; do
  dir="${dir%/}"
  [ -f "${dir}/build.gradle.kts" ] || continue
  grep -rqs "captureRoboImage" "${dir}/src/test" || continue
  module=":${dir//\//:}"
  if compgen -G "${dir}/src/test/screenshots/*.png" > /dev/null; then
    screenshot_tasks+=("${module}:verifyRoborazziDebug")
  elif in_ci; then
    missing+=("${module}")
  else
    screenshot_tasks+=("${module}:recordRoborazziDebug")
    recorded+=("${module}")
  fi
done

if [ "${#missing[@]}" -gt 0 ]; then
  for m in "${missing[@]}"; do
    echo "::error::Module ${m} có screenshot test nhưng chưa có ảnh chuẩn trong src/test/screenshots. Chạy ./gradlew ${m}:recordRoborazziDebug, xem ảnh và commit cùng PR."
  done
  exit 1
fi

echo "== Cổng chất lượng: quy tắc module, ktlint, unit test, screenshot, lint, build staging"
./gradlew --no-daemon --console=plain --continue \
  checkModuleDependencies \
  ktlintCheck \
  testDebugUnitTest \
  testStagingDebugUnitTest \
  ${screenshot_tasks[@]+"${screenshot_tasks[@]}"} \
  :app:lintStagingDebug \
  :app:assembleStagingDebug

scripts/test-summary.sh

if [ "${#recorded[@]}" -gt 0 ]; then
  echo "CHÚ Ý: đã ghi ảnh chuẩn screenshot mới cho: ${recorded[*]}. Xem ảnh trong src/test/screenshots và commit cùng thay đổi."
fi
echo "== Cổng chất lượng: QUA"
