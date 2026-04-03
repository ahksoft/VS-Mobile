#!/bin/bash
# Start Xfce4 desktop - X server is started by VS Mobile app (DesktopActivity)
# Run this AFTER tapping Desktop tab in VS Mobile

# ── Install Xfce if needed ────────────────────────────────────────────────────
if ! command -v xfce4-session &>/dev/null; then
    echo "[*] Installing Xfce4..."
    mkdir -p /etc/apt/preferences.d
    cat > /etc/apt/preferences.d/nosnap.pref << 'EOF'
Package: snapd
Pin: release a=*
Pin-Priority: -10
EOF
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal dbus-x11 xfonts-base 2>/dev/null
    echo "[✓] Xfce4 installed"
fi

# ── Kill existing Xfce session ────────────────────────────────────────────────
kill -9 $(pgrep -f "xfce4-session") 2>/dev/null
sleep 1

export DISPLAY=:0
export TMPDIR=/tmp
export XDG_RUNTIME_DIR=/tmp
export NO_AT_BRIDGE=1
export LIBGL_ALWAYS_SOFTWARE=1

# Disable compositing
mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
  </property>
</channel>
EOF

echo "[*] Starting Xfce4 on DISPLAY=:0..."
echo "[!] Make sure you tapped Desktop tab first to start the X server"

dbus-launch --exit-with-session startxfce4
