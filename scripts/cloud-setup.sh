#!/usr/bin/env bash
# Setup script cho môi trường cloud của routine phiên tự chạy (ADR-0009).
# Dán nội dung file này vào ô "Setup script" của cloud environment.
# Yêu cầu mức mạng Custom: danh sách mặc định + dl.google.com.
# Môi trường đã có sẵn JDK 21 và Gradle; script chỉ cài Android SDK.

set -euo pipefail

SDK_ROOT="${HOME}/android-sdk"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"

if [ ! -x "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "${SDK_ROOT}/cmdline-tools"
  curl -fsSL "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}" -o /tmp/cmdline-tools.zip
  unzip -q /tmp/cmdline-tools.zip -d "${SDK_ROOT}/cmdline-tools"
  mv "${SDK_ROOT}/cmdline-tools/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
  rm /tmp/cmdline-tools.zip
fi

export ANDROID_HOME="${SDK_ROOT}"
yes | "${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" --licenses > /dev/null
"${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" --install \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0" > /dev/null

# Cho Gradle và các phiên sau biết vị trí SDK.
{
  echo "export ANDROID_HOME=${SDK_ROOT}"
  echo "export ANDROID_SDK_ROOT=${SDK_ROOT}"
} >> "${HOME}/.bashrc"

echo "Android SDK đã sẵn sàng tại ${SDK_ROOT}"
