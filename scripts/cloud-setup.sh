#!/usr/bin/env bash
# Setup script for the cloud environment of the auto-run routine (ADR-0009).
# Paste this file into the "Setup script" field of the cloud environment.
# Needs the Custom network level: the default allowlist plus dl.google.com.
# The environment already ships JDK and Gradle; this script only installs the Android SDK.
# Safe to run any number of times: every step checks what is already there.

set -euo pipefail

SDK_ROOT="${HOME}/android-sdk"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
SDKMANAGER="${SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager"

if [ ! -x "${SDKMANAGER}" ]; then
  # Download and unpack in a temp dir, then move into place in one step,
  # so a failed earlier run never leaves a half-installed directory behind.
  work_dir="$(mktemp -d)"
  trap 'rm -rf "${work_dir}"' EXIT
  curl -fsSL "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}" -o "${work_dir}/tools.zip"
  unzip -q -o "${work_dir}/tools.zip" -d "${work_dir}/unpacked"
  mkdir -p "${SDK_ROOT}/cmdline-tools"
  rm -rf "${SDK_ROOT}/cmdline-tools/latest"
  mv "${work_dir}/unpacked/cmdline-tools" "${SDK_ROOT}/cmdline-tools/latest"
fi

export ANDROID_HOME="${SDK_ROOT}"
yes | "${SDKMANAGER}" --licenses > /dev/null || true
# sdkmanager skips packages that are already installed.
"${SDKMANAGER}" --install \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0" > /dev/null

# Tell Gradle and later shells where the SDK is. Each line is added only once.
for line in "export ANDROID_HOME=${SDK_ROOT}" "export ANDROID_SDK_ROOT=${SDK_ROOT}"; do
  grep -qxF "${line}" "${HOME}/.bashrc" 2>/dev/null || echo "${line}" >> "${HOME}/.bashrc"
done

# Maven Central answers 429 (too many requests) to cloud sessions now and then. Let Gradle retry longer, with
# backoff, instead of failing the build. User-level properties, so the repo's gradle.properties stays unchanged.
GRADLE_USER_PROPERTIES="${HOME}/.gradle/gradle.properties"
mkdir -p "${HOME}/.gradle"
for line in \
  "systemProp.org.gradle.internal.repository.max.retries=8" \
  "systemProp.org.gradle.internal.repository.initial.backoff=1000"; do
  grep -qxF "${line}" "${GRADLE_USER_PROPERTIES}" 2>/dev/null || echo "${line}" >> "${GRADLE_USER_PROPERTIES}"
done

echo "Android SDK đã sẵn sàng tại ${SDK_ROOT}"
