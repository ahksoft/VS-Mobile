#!/bin/bash
# Start Xfce4 desktop using Termux:X11 as the display server
# Based on: https://github.com/LinuxDroidMaster/Termux-Desktops

# ── Install Xfce if needed ────────────────────────────────────────────────────
if ! command -v xfce4-session &>/dev/null; then
    echo "[*] Installing Xfce4 desktop environment..."
    # Block snap (can't run in proot)
    mkdir -p /etc/apt/preferences.d
    cat > /etc/apt/preferences.d/nosnap.pref << 'EOF'
Package: snapd
Pin: release a=*
Pin-Priority: -10
EOF
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal xfce4-goodies \
        dbus-x11 xfonts-base \
        2>/dev/null
    echo "[✓] Xfce4 installed"
fi

# ── Kill existing X11 processes ───────────────────────────────────────────────
kill -9 $(pgrep -f "termux.x11") 2>/dev/null
kill -9 $(pgrep -f "xfce4-session") 2>/dev/null
sleep 1

# ── Start Termux:X11 X server ─────────────────────────────────────────────────
# termux-x11 binary is provided by the Termux:X11 app via app_process
export CLASSPATH=$(pm path com.termux.x11 2>/dev/null | cut -d: -f2)

if [ -z "$CLASSPATH" ]; then
    echo "[✗] Termux:X11 not installed."
    echo "    Open the Desktop tab in VS Mobile to install it, then run 'desktop' again."
    exit 1
fi

export DISPLAY=:0
export TMPDIR=/tmp
export XDG_RUNTIME_DIR=/tmp

echo "[*] Starting Termux:X11 X server on :0..."
# startx11 uses #!/system/bin/sh so it runs on Android host outside proot
startx11
sleep 1

# ── Start Xfce4 session ───────────────────────────────────────────────────────
echo "[*] Starting Xfce4 session on DISPLAY=:0..."
export NO_AT_BRIDGE=1
export LIBGL_ALWAYS_SOFTWARE=1

# Disable compositing (not supported in proot)
mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
  </property>
</channel>
EOF

dbus-launch --exit-with-session startxfce4 &

echo "[✓] Desktop started. Switch to the X11 window."
wait
