#!/usr/bin/env bash
# Records a test session of the staging app on a device, so the AI can analyse its log (docs/ops/test-sessions.md).
# The same script runs on a physical device by a person and on the emulator in .github/workflows/device-session.yml,
# so both produce the same folder and are analysed the same way.
#
# Usage:
#   scripts/device-session.sh start [<name>]   start recording on the connected device
#   scripts/device-session.sh scenarios --yes  run the standard scenarios: cold starts, first launch offline,
#                                              random taps (monkey). Clears the app's data and toggles airplane mode.
#   scripts/device-session.sh stop             stop recording, collect device data, write summary.md
#   scripts/device-session.sh summary <dir>    (re)write summary.md of a session folder; no device needed
# Exit codes: 0 done (summary: no blocking signal), 1 error or blocking signal (crash, ANR), 2 usage.
#
# Settings (environment):
#   ANDROID_SERIAL       device to use when several are connected (read by adb itself)
#   APP_PACKAGE          package to test; default: STAGING_APPLICATION_ID of .github/workflows/staging.yml
#   DEVICE_SESSION_DIR   where session folders go; default: test-sessions (ignored by git)
#   MONKEY_EVENTS        random events in `scenarios`; default 1500
# Messages are in Vietnamese for the team; comments are in English.
set -euo pipefail
cd "$(dirname "$0")/.."

SESSIONS_DIR="${DEVICE_SESSION_DIR:-test-sessions}"
STATE_FILE="${SESSIONS_DIR}/.current"
MONKEY_EVENTS="${MONKEY_EVENTS:-1500}"
MONKEY_SEED=20261017
MARKER_TAG="VGL.Session"

fail() { echo "LỖI: $*" >&2; exit 1; }
usage() { sed -n '5,10p' "$0" | sed 's/^# //' >&2; exit 2; }

app_package() {
  if [[ -n "${APP_PACKAGE:-}" ]]; then echo "${APP_PACKAGE}"; return; fi
  sed -n 's/^ *STAGING_APPLICATION_ID: *\([A-Za-z0-9_.]*\).*/\1/p' .github/workflows/staging.yml | head -n 1
}

need_device() {
  command -v adb > /dev/null || fail "không tìm thấy adb. Cài Android SDK platform-tools (docs/ops/test-sessions.md)."
  [[ "$(adb get-state 2>/dev/null || true)" == "device" ]] ||
    fail "không thấy thiết bị. Cắm cáp và bật USB debugging, hoặc ghép Wireless debugging; nhiều máy thì đặt ANDROID_SERIAL."
}

# Writes a line into the device log, so the analysis can split the log by step.
mark() { adb shell log -t "${MARKER_TAG}" "'$*'" > /dev/null 2>&1 || true; }

next_session_dir() {
  local day number=1
  day="$(date +%Y%m%d)"
  while [[ -e "${SESSIONS_DIR}/TS-${day}-$(printf '%02d' "${number}")" ]]; do number=$((number + 1)); done
  echo "${SESSIONS_DIR}/TS-${day}-$(printf '%02d' "${number}")"
}

cmd_start() {
  local name="${1:-phien-thu}" pkg dir
  need_device
  pkg="$(app_package)"
  [[ -n "${pkg}" ]] || fail "không xác định được tên gói; đặt APP_PACKAGE."
  adb shell pm path "${pkg}" > /dev/null 2>&1 || fail "máy chưa cài ${pkg}. Cài bản staging từ Firebase App Distribution trước."
  [[ ! -f "${STATE_FILE}" ]] || fail "đang có phiên chưa dừng ($(head -n 1 "${STATE_FILE}")). Chạy: scripts/device-session.sh stop"

  dir="$(next_session_dir)"
  mkdir -p "${dir}"
  adb logcat -b all -c || true
  adb shell dumpsys gfxinfo "${pkg}" reset > /dev/null 2>&1 || true
  nohup adb logcat -b all -v threadtime > "${dir}/logcat.txt" 2>&1 &
  printf '%s\n%s\n%s\n' "${dir}" "$!" "${pkg}" > "${STATE_FILE}"

  {
    echo "# PHIÊN THỬ $(basename "${dir}")"
    echo
    echo "- Tên: ${name}"
    echo "- Bắt đầu: $(date '+%Y-%m-%d %H:%M:%S %z')"
    echo "- Ứng dụng: ${pkg}, $(adb shell dumpsys package "${pkg}" | sed -n 's/^ *versionName=//p' | head -n 1 | tr -d '\r')"
    echo "- Máy: $(adb shell getprop ro.product.manufacturer | tr -d '\r') $(adb shell getprop ro.product.model | tr -d '\r'), Android $(adb shell getprop ro.build.version.release | tr -d '\r') (API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"
    echo "- Commit của repo trên máy chạy script: $(git rev-parse --short HEAD 2>/dev/null || echo 'không rõ')"
  } > "${dir}/session.md"
  [[ -f "${dir}/notes.md" ]] || cat > "${dir}/notes.md" << 'NOTES'
# GHI CHÚ CỦA NGƯỜI THỬ

<!-- Viết ngắn trong lúc thử. AI đọc file này cùng log. Không ghi dữ liệu cá nhân. -->

## Định thử gì

## Đã thử (giờ, việc)

- HH:MM

## Thấy lạ (giờ, chuyện gì)

- HH:MM
NOTES
  mark "session=start name=${name}"
  echo "Đang ghi phiên ${dir}. Thử app, ghi chú vào ${dir}/notes.md, xong thì chạy: scripts/device-session.sh stop"
}

cmd_scenarios() {
  [[ "${1:-}" == "--yes" ]] || fail "kịch bản sẽ xóa dữ liệu của app và bật, tắt chế độ máy bay. Chạy lại với --yes nếu đồng ý."
  need_device
  [[ -f "${STATE_FILE}" ]] || fail "chưa có phiên đang ghi. Chạy scripts/device-session.sh start trước."
  local dir pkg activity i out
  dir="$(sed -n 1p "${STATE_FILE}")"
  pkg="$(sed -n 3p "${STATE_FILE}")"
  activity="$(adb shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "${pkg}" | tail -n 1 | tr -d '\r')"
  [[ "${activity}" == */* ]] || fail "không tìm thấy màn hình khởi động của ${pkg}."
  : > "${dir}/scenarios.txt"

  # 1. Cold starts on a clean install state (the first one also shows the consent dialog).
  adb shell pm clear "${pkg}" > /dev/null
  for i in 1 2 3; do
    mark "scenario=cold_start run=${i}"
    adb shell am force-stop "${pkg}"
    out="$(adb shell am start -W -n "${activity}" | tr -d '\r')"
    echo "cold_start run=${i} total_ms=$(sed -n 's/^TotalTime: //p' <<<"${out}")" >> "${dir}/scenarios.txt"
    sleep 15
  done
  adb exec-out screencap -p > "${dir}/after-start.png" 2>/dev/null || true

  # 2. First launch without network: the spec expects an error and a retry button (loading, KB-2).
  mark "scenario=offline_first_launch"
  adb shell pm clear "${pkg}" > /dev/null
  adb shell cmd connectivity airplane-mode enable > /dev/null 2>&1 || true
  sleep 3
  adb shell am start -W -n "${activity}" > /dev/null
  sleep 20
  adb exec-out screencap -p > "${dir}/offline.png" 2>/dev/null || true
  adb shell cmd connectivity airplane-mode disable > /dev/null 2>&1 || true
  echo "offline_first_launch screenshot=offline.png" >> "${dir}/scenarios.txt"
  sleep 10

  # 3. Random taps and swipes inside the app, with a fixed seed so a crash can be reproduced.
  mark "scenario=monkey events=${MONKEY_EVENTS} seed=${MONKEY_SEED}"
  adb shell monkey -p "${pkg}" -s "${MONKEY_SEED}" --throttle 200 --pct-syskeys 0 -v "${MONKEY_EVENTS}" \
    > "${dir}/monkey.txt" 2>&1 || true
  echo "monkey events=${MONKEY_EVENTS} seed=${MONKEY_SEED}" >> "${dir}/scenarios.txt"
  mark "scenario=done"
  echo "Đã chạy xong kịch bản chuẩn. Chạy: scripts/device-session.sh stop"
}

cmd_stop() {
  [[ -f "${STATE_FILE}" ]] || fail "không có phiên đang ghi."
  local dir pid pkg
  dir="$(sed -n 1p "${STATE_FILE}")"
  pid="$(sed -n 2p "${STATE_FILE}")"
  pkg="$(sed -n 3p "${STATE_FILE}")"
  if [[ "$(adb get-state 2>/dev/null || true)" == "device" ]]; then
    mark "session=stop"
    sleep 1
    adb shell dumpsys gfxinfo "${pkg}" > "${dir}/gfxinfo.txt" 2>&1 || true
    adb shell dumpsys meminfo "${pkg}" > "${dir}/meminfo.txt" 2>&1 || true
    adb shell dumpsys dropbox --print data_app_crash data_app_anr > "${dir}/crash-anr.txt" 2>&1 || true
  else
    echo "CHÚ Ý: mất kết nối với máy; chỉ giữ log đã ghi." >&2
  fi
  kill "${pid}" 2>/dev/null || true
  rm -f "${STATE_FILE}"
  echo "- Kết thúc: $(date '+%Y-%m-%d %H:%M:%S %z')" >> "${dir}/session.md"
  local status=0
  cmd_summary "${dir}" "${pkg}" || status=$?
  echo "Đã lưu phiên vào ${dir}. Nhờ AI: \"Phân tích phiên thử ${dir}\" (docs/CLAUDE.md, mục Khi phân tích một phiên thử)."
  return "${status}"
}

# Counts the signals the analysis starts from. Blocking: the app crashed or did not respond.
cmd_summary() {
  local dir="${1:-}" pkg="${2:-}"
  [[ -d "${dir}" && -f "${dir}/logcat.txt" ]] || fail "không có ${dir}/logcat.txt."
  [[ -n "${pkg}" ]] || pkg="$(sed -n 's/^- Ứng dụng: \([^,]*\),.*/\1/p' "${dir}/session.md" 2>/dev/null | head -n 1)"
  [[ -n "${pkg}" ]] || pkg="$(app_package)"
  local log="${dir}/logcat.txt" crashes anrs app_errors app_warnings monkey_events monkey_problem blocking
  crashes="$(grep -c "AndroidRuntime: Process: ${pkg}," "${log}" || true)"
  anrs="$(grep -c "ANR in ${pkg}" "${log}" || true)"
  app_errors="$(grep -cE ' E VGL\.' "${log}" || true)"
  app_warnings="$(grep -cE ' W VGL\.' "${log}" || true)"
  monkey_events=""
  monkey_problem=0
  if [[ -f "${dir}/monkey.txt" ]]; then
    monkey_events="$(sed -n 's/^Events injected: //p' "${dir}/monkey.txt" | tail -n 1)"
    monkey_problem="$(grep -cE '^// (CRASH|NOT RESPONDING)' "${dir}/monkey.txt" || true)"
  fi
  blocking=$(( crashes + anrs + monkey_problem ))
  # The app's own log, crash traces and session markers, for a quick read before the full log.
  grep -E " (VGL[A-Za-z0-9_.]*|AndroidRuntime|ActivityManager) *:" "${log}" |
    grep -E "VGL|FATAL|Process: ${pkg}|ANR in ${pkg}|^\s|Caused by|at " > "${dir}/app-log.txt" || true

  {
    echo "# TÓM TẮT PHIÊN $(basename "${dir}")"
    echo
    echo "Sinh bởi \`scripts/device-session.sh\`. Tín hiệu đếm tự động; AI phân tích tiếp theo \`docs/templates/test-session.md\`."
    echo
    echo "| Tín hiệu | Số lần | Mức |"
    echo "| --- | --- | --- |"
    echo "| App dừng đột ngột (crash) | ${crashes:-0} | $([ "${crashes:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| App không phản hồi (ANR) | ${anrs:-0} | $([ "${anrs:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| Monkey báo crash hay treo | ${monkey_problem:-0} | $([ "${monkey_problem:-0}" -gt 0 ] && echo chặn || echo '') |"
    echo "| Log lỗi của app (\`E VGL.*\`) | ${app_errors:-0} | xem |"
    echo "| Log cảnh báo của app (\`W VGL.*\`) | ${app_warnings:-0} | xem |"
    echo
    if [[ -f "${dir}/scenarios.txt" ]]; then
      echo "## Kịch bản chuẩn"
      echo
      echo '```text'
      cat "${dir}/scenarios.txt"
      [[ -n "${monkey_events}" ]] && echo "monkey events_injected=${monkey_events}"
      echo '```'
      echo
    fi
    if [[ -f "${dir}/gfxinfo.txt" ]]; then
      echo "## Khung hình và bộ nhớ"
      echo
      echo '```text'
      grep -E 'Total frames rendered|Janky frames|90th percentile|99th percentile' "${dir}/gfxinfo.txt" | head -n 4 || true
      grep -E '^ *TOTAL PSS|^ *TOTAL:' "${dir}/meminfo.txt" 2>/dev/null | head -n 1 || true
      echo '```'
      echo
      echo "Số liệu khung hình trên emulator không có GPU chỉ để so giữa các lần chạy, không phản ánh máy thật."
    fi
  } > "${dir}/summary.md"
  cat "${dir}/summary.md"
  [[ "${blocking}" -eq 0 ]]
}

case "${1:-}" in
  start) shift; cmd_start "$@" ;;
  scenarios) shift; cmd_scenarios "$@" ;;
  stop) cmd_stop ;;
  summary) shift; [[ $# -ge 1 ]] || usage; cmd_summary "$1" "${2:-}" ;;
  *) usage ;;
esac
