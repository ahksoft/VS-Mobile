#!/system/bin/sh
# Runs on Android host (outside proot) to start termux-x11 X server

PKG="com.ahk.uvscode.debug"
APP_TMPDIR="/data/user/0/$PKG/cache/tmp"

export CLASSPATH=$(pm path com.termux.x11 2>/dev/null | cut -d: -f2)

if [ -z "$CLASSPATH" ]; then
    echo "[✗] Termux:X11 not installed. Tap Desktop tab in VS Mobile to install."
    exit 1
fi

# Kill existing X server
kill -9 $(pgrep -f "termux.x11") 2>/dev/null
sleep 1

# Ensure tmp dir exists (this is where X socket will be created)
mkdir -p "$APP_TMPDIR/.X11-unix"

# Start X server with TMPDIR pointing to app cache
export TMPDIR="$APP_TMPDIR"
/system/bin/app_process / com.termux.x11.CmdEntryPoint :0 &
sleep 2

# Open the display Activity
am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1
echo "[✓] X server started on :0 (socket at $APP_TMPDIR/.X11-unix/X0)"
