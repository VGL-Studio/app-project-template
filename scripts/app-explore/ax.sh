#!/usr/bin/env bash
# Drives a reference app on a device for the AI study of apps to clone (docs/ops/clone-study.md, ADR-0021).
# Every tap goes through guard.py: taps that would open an ad or start a purchase are refused, and a jump to the
# Play Store, a browser or a mail app (an ad click, a "rate us" link) is undone and recorded in incidents.log.
#
# Usage:
#   ax.sh check                         device present, accepts input (Xiaomi needs "USB debugging (Security
#                                       settings)"), and online
#   ax.sh shot <dir> <name>             screenshot (full + preview, longest side 1000 px) and compact UI tree
#   ax.sh tree                          compact UI tree of the current screen, then ad status
#   ax.sh tap <x> <y>                   tap at pixel coordinates (guarded)
#   ax.sh taptext <text>                tap the node whose text or description matches best (guarded)
#   ax.sh tapid <resource-id>           tap the first node with this resource-id, short name (guarded)
#   ax.sh drag <x1> <y1> <x2> <y2> [ms] slow drag; the start point is guarded; default 800 ms
#   ax.sh swipe up|down|left|right
#   ax.sh adclose                       find and tap the close or collapse button of an ad
#   ax.sh adcorner tl|tr                tap the top-left or top-right corner, for ad close buttons not in the tree
#   ax.sh type <text>                   type ASCII text into the focused field
#   ax.sh back | home | wake | launch <pkg> | stop <pkg> | top
#   ax.sh color <png> <x> <y> ...       hex colour of pixels of a full screenshot
#
# Exit codes: 0 done, 1 not found, 2 usage, 3 input refused by the device, 4 tap refused by the guard,
# 5 no device, 6 left the app (undone), 7 screen locked, 8 offline.
# Settings: ANDROID_SERIAL (several devices), AX_INCIDENTS (incident log, default ./incidents.log in the study
# folder), AX_SETTLE (seconds to wait after a tap, default 1.5). Messages are in Vietnamese; comments in English.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")" && pwd)"
LOG="${AX_INCIDENTS:-$PWD/incidents.log}"
CUR="${TMPDIR:-/tmp}/ax_cur_$$.xml"
trap 'rm -f "$CUR"' EXIT

need_device() {
  [[ "$(adb get-state 2>/dev/null || true)" == "device" ]] || { echo "LỖI: không thấy thiết bị (adb). Chờ rồi thử lại." >&2; exit 5; }
}
# Screen size and density, for the dp values in the tree and the size rules of the guard.
screen_env() {
  local size density
  size="$(adb shell wm size 2>/dev/null | tr -d '\r' | sed -n 's/.*: *\([0-9]*x[0-9]*\).*/\1/p' | tail -n 1)"
  density="$(adb shell wm density 2>/dev/null | tr -d '\r' | sed -n 's/.*: *\([0-9]*\).*/\1/p' | tail -n 1)"
  export AX_WIDTH="${size%x*}" AX_HEIGHT="${size#*x}" AX_DENSITY="${density:-440}"
  [[ -n "${AX_WIDTH}" && "${AX_WIDTH}" != "${size}" ]] || export AX_WIDTH=1080 AX_HEIGHT=2400
}
# adb shell exits 0 even when the device refuses injected input, so read the output.
run_input() {
  local out; out="$(adb shell input "$@" 2>&1 || true)"
  if grep -q "Exception" <<<"$out"; then
    echo "LỖI: máy từ chối thao tác giả lập (input $*). Xiaomi: bật Tùy chọn nhà phát triển > Gỡ lỗi USB (Cài đặt bảo mật)." >&2
    exit 3
  fi
}
dump() { adb shell uiautomator dump /sdcard/ax.xml > /dev/null 2>&1 || true; adb exec-out cat /sdcard/ax.xml; }
top_line() { adb shell dumpsys activity activities 2>/dev/null | grep -m1 -E "topResumedActivity|mResumedActivity" | sed 's/^ *//' || true; }
# After a tap, undo a jump to the store, a browser or a mail app, and record it.
after_tap() {
  sleep "${AX_SETTLE:-1.5}"
  local t; t="$(top_line)"
  if grep -qE "com.android.vending|chrome|browser|com.mi.globalbrowser|webview|com.google.android.gm|ProxyBilling" <<<"$t"; then
    run_input keyevent 4
    echo "$(date '+%F %T') rời app sau khi chạm $1: $t (đã back)" >> "$LOG"
    echo "CẢNH BÁO: chạm $1 đã mở app khác ($t); đã back. Ghi vào báo cáo." >&2
    exit 6
  fi
}
guarded_tap() {
  local x="$1" y="$2"
  dump > "$CUR"
  python3 -I "$TOOLS/guard.py" check "$CUR" "$(top_line)" "$x" "$y" || exit 4
  run_input tap "$x" "$y"; echo "tapped $x $y"
  after_tap "($x,$y)"
}

[[ $# -ge 1 ]] || { sed -n '5,24p' "$0"; exit 2; }
if [[ "$1" == "color" ]]; then
  png="$2"; shift 2
  exec python3 -P -c 'import sys
from PIL import Image
im = Image.open(sys.argv[1]).convert("RGB"); a = sys.argv[2:]
for i in range(0, len(a), 2):
    x, y = int(a[i]), int(a[i + 1])
    if not (0 <= x < im.width and 0 <= y < im.height):
        print(x, y, "ngoài ảnh %dx%d" % im.size); continue
    print(x, y, "#%02x%02x%02x" % im.getpixel((x, y)))' "$png" "$@"
fi

need_device
screen_env
case "$1" in
  check)
    run_input keyevent 0; echo "input: ok"
    net="$(adb shell dumpsys connectivity 2>/dev/null | tr -d '\r' | sed -n 's/^ *Active default network: *//p' | head -n 1)"
    if [[ "${net:-none}" == "none" ]]; then
      echo "CHÚ Ý: máy không có mạng. Quảng cáo và nội dung tải về sẽ không hiện; nhờ người bật mạng trước khi khảo sát." >&2
      exit 8
    fi
    echo "network: ok" ;;
  shot)
    out="$2"; name="$3"; mkdir -p "$out/full" "$out/preview" "$out/tree"
    for _ in 1 2 3; do
      adb exec-out screencap -p > "$out/full/$name.png"
      w="$(sips -g pixelWidth "$out/full/$name.png" 2>/dev/null | awk '/pixelWidth/{print $2}' || true)"
      # A capture taken during a rotation comes out landscape; take it again.
      [[ -z "$w" || "$w" == "$AX_WIDTH" ]] && break; sleep 1
    done
    python3 -P -c 'import sys
from PIL import Image
im = Image.open(sys.argv[1]); im.thumbnail((1000, 1000)); im.save(sys.argv[2])
if max(im.convert("L").resize((54, 120)).getdata()) < 8:
    print("CHÚ Ý: ảnh đen toàn bộ, có thể màn hình chặn chụp (FLAG_SECURE); dựa vào cây giao diện.")' \
      "$out/full/$name.png" "$out/preview/$name.png"
    dump > "$out/tree/$name.xml"
    python3 -I "$TOOLS/tree.py" "$out/tree/$name.xml" | tee "$out/tree/$name.txt"
    python3 -I "$TOOLS/guard.py" status "$out/tree/$name.xml" "$(top_line)"
    echo "preview: $out/preview/$name.png" ;;
  tree) dump > "$CUR"; python3 -I "$TOOLS/tree.py" "$CUR"; python3 -I "$TOOLS/guard.py" status "$CUR" "$(top_line)" ;;
  tap) guarded_tap "$2" "$3" ;;
  taptext|tapid)
    dump > "$CUR"
    flag="--find"; [[ "$1" == "tapid" ]] && flag="--findid"
    xy="$(python3 -I "$TOOLS/tree.py" "$flag" "$2" "$CUR")" || { echo "không thấy: $2"; exit 1; }
    read -r x y <<<"${xy}"; guarded_tap "$x" "$y" ;;
  drag)
    dump > "$CUR"
    python3 -I "$TOOLS/guard.py" check "$CUR" "$(top_line)" "$2" "$3" || exit 4
    run_input swipe "$2" "$3" "$4" "$5" "${6:-800}"; after_tap "drag" ;;
  swipe)
    cx=$((AX_WIDTH / 2)); cy=$((AX_HEIGHT / 2)); top=$((AX_HEIGHT * 3 / 10)); bottom=$((AX_HEIGHT * 3 / 4))
    left=$((AX_WIDTH / 8)); right=$((AX_WIDTH * 7 / 8))
    case "$2" in
      up) run_input swipe "$cx" "$bottom" "$cx" "$top" 300 ;;
      down) run_input swipe "$cx" "$top" "$cx" "$bottom" 300 ;;
      left) run_input swipe "$right" "$cy" "$left" "$cy" 300 ;;
      right) run_input swipe "$left" "$cy" "$right" "$cy" 300 ;;
      *) echo "up|down|left|right"; exit 2 ;;
    esac ;;
  adclose)
    dump > "$CUR"
    found="$(python3 -I "$TOOLS/guard.py" close "$CUR")" ||
      { echo "không thấy nút đóng trong cây giao diện; xem ảnh rồi dùng 'ax.sh adcorner tl|tr'"; exit 1; }
    read -r x y label <<<"$found"
    run_input tap "$x" "$y"; echo "đã đóng bằng $label tại $x $y"; after_tap "adclose" ;;
  adcorner)
    case "$2" in
      tl) x=$((AX_WIDTH * 7 / 100)); y=$((AX_HEIGHT * 8 / 100)) ;;
      tr) x=$((AX_WIDTH * 93 / 100)); y=$((AX_HEIGHT * 8 / 100)) ;;
      *) echo "tl|tr"; exit 2 ;;
    esac
    run_input tap "$x" "$y"; echo "đã chạm góc $2"; after_tap "adcorner $2" ;;
  type) run_input text "$(printf '%s' "$2" | sed 's/ /%s/g')" ;;
  back) run_input keyevent 4 ;;
  home) run_input keyevent 3 ;;
  wake)
    run_input keyevent 224
    if adb shell dumpsys window 2>/dev/null | grep -qE "isKeyguardShowing=true|mShowingLockscreen=true|mDreamingLockscreen=true"; then
      echo "Màn hình đang khóa: cần người mở khóa máy." >&2; exit 7
    fi ;;
  launch) adb shell monkey -p "$2" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1 ;;
  stop) adb shell am force-stop "$2" ;;
  top) top_line ;;
  *) sed -n '5,24p' "$0"; exit 2 ;;
esac
