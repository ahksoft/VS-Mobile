#!/bin/bash
# Start Xfce4 desktop with x11vnc for VNC access
# Tap Desktop tab in VS Mobile to open the VNC viewer

VNC_PORT=5905
VNC_PASS="123456"

# ── Install if needed ─────────────────────────────────────────────────────────
if ! command -v xfce4-session &>/dev/null || ! command -v x11vnc &>/dev/null; then
    echo "[*] Installing Xfce4 + x11vnc..."
    mkdir -p /etc/apt/preferences.d
    cat > /etc/apt/preferences.d/nosnap.pref << 'EOF'
Package: snapd
Pin: release a=*
Pin-Priority: -10
EOF
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal x11vnc xvfb dbus-x11 xfonts-base 2>/dev/null
    echo "[✓] Installed"
fi

# ── Kill existing sessions ────────────────────────────────────────────────────
pkill -f "Xvfb :5" 2>/dev/null
pkill -f "x11vnc" 2>/dev/null
pkill -f "xfce4-session" 2>/dev/null
sleep 1

export NO_AT_BRIDGE=1
export LIBGL_ALWAYS_SOFTWARE=1
export DISPLAY=:5

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

# ── Start Xvfb ────────────────────────────────────────────────────────────────
Xvfb :5 -screen 0 1280x720x24 -ac &
sleep 2

# ── Start Xfce ────────────────────────────────────────────────────────────────
dbus-launch --exit-with-session startxfce4 &
sleep 4

# ── Start x11vnc ─────────────────────────────────────────────────────────────
x11vnc -display :5 \
    -rfbport $VNC_PORT \
    -passwd "$VNC_PASS" \
    -forever \
    -shared \
    -noshm \
    -noxdamage \
    -noxfixes \
    -noipv6 \
    -listen 127.0.0.1 \
    -quiet &

sleep 1
echo "[✓] Desktop running on VNC port $VNC_PORT (password: $VNC_PASS)"
echo "Tap Desktop tab in VS Mobile to connect"
wait
