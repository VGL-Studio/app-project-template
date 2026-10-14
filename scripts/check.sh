#!/usr/bin/env bash
# Cổng chất lượng chạy trên JVM (ADR-0010). Dùng chung cho:
#   - AI ở bước 5 của phiên tự chạy (kiểm tra cục bộ),
#   - job build-and-test của .github/workflows/pr-check.yml,
#   - .github/workflows/main-guard.yml.
# Test trên emulator không nằm ở đây; chúng chạy riêng trong job ui-test của CI.
#
# Thay đổi file này là thay đổi cổng chất lượng: plan rủi ro cao, bắt buộc duyệt chéo.

set -euo pipefail
cd "$(dirname "$0")/.."

GRADLE="./gradlew --no-daemon --stacktrace"

echo "== 1. Quy tắc phụ thuộc module và định dạng code"
$GRADLE checkModuleDependencies ktlintCheck

echo "== 2. Unit test và test giao diện trên JVM"
$GRADLE testDebugUnitTest testStagingDebugUnitTest
scripts/test-summary.sh

echo "== 3. Screenshot test"
# Có ảnh chuẩn thì so sánh; chưa có (module mới) thì ghi ảnh chuẩn và cảnh báo để người xem.
if find . -path '*/src/test/screenshots/*.png' -not -path './build/*' | grep -q .; then
  $GRADLE verifyRoborazziDebug
else
  echo "::warning::Chưa có ảnh chuẩn screenshot. Ghi ảnh chuẩn mới, cần người xem và commit."
  $GRADLE recordRoborazziDebug
fi

echo "== 4. Android Lint (app và mọi module app phụ thuộc)"
$GRADLE :app:lintStagingDebug

echo "== 5. Build bản staging"
$GRADLE :app:assembleStagingDebug

scripts/test-summary.sh
echo "== Cổng chất lượng: QUA"
