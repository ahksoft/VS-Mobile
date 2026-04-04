#!/bin/bash
# Desktop environment launcher for VS Mobile
# Supports XFCE4 (xubuntu-desktop) and KDE Plasma (kubuntu-desktop)

VNC_PORT=5905
VNC_PASS="123456"

# ── Read config ───────────────────────────────────────────────────────────────
WIDTH=720
HEIGHT=1600
DE=xfce
[ -f ~/.vnc_config ] && source ~/.vnc_config

# ── Block snap ────────────────────────────────────────────────────────────────
mkdir -p /etc/apt/preferences.d
cat > /etc/apt/preferences.d/nosnap.pref << 'EOF'
Package: snapd
Pin: release a=*
Pin-Priority: -10
EOF

# ── Install desktop environment if needed ────────────────────────────────────
install_flag=~/.desktop_installed_${DE}

if [ ! -f "$install_flag" ]; then
    echo "[*] Installing ${DE} desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq

    if [ "$DE" = "kde" ]; then
        echo "[*] Installing KDE Plasma (kubuntu-desktop)..."
        DEBIAN_FRONTEND=noninteractive apt install -y kubuntu-desktop x11vnc xvfb dbus-x11 2>/dev/null
    else
        echo "[*] Installing XFCE4 (xubuntu-desktop)..."
        DEBIAN_FRONTEND=noninteractive apt install -y xubuntu-desktop x11vnc xvfb dbus-x11 2>/dev/null
    fi

    touch "$install_flag"
    echo "[✓] ${DE} installed"
fi

# ── Kill existing sessions ────────────────────────────────────────────────────
pkill -f "Xvfb :5" 2>/dev/null
pkill -f "x11vnc" 2>/dev/null
pkill -f "xfce4-session\|startplasma\|kwin\|plasmashell" 2>/dev/null
sleep 1

export NO_AT_BRIDGE=1
export LIBGL_ALWAYS_SOFTWARE=1
export DISPLAY=:5

# ── Disable compositing for XFCE ─────────────────────────────────────────────
if [ "$DE" = "xfce" ]; then
    mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
    cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
  </property>
</channel>
EOF
fi

# ── Start Xvfb ────────────────────────────────────────────────────────────────
Xvfb :5 -screen 0 ${WIDTH}x${HEIGHT}x24 \
    +extension RANDR \
    +extension RENDER \
    -ac &
sleep 2

DISPLAY=:5 xrandr --fb ${WIDTH}x${HEIGHT} 2>/dev/null || true

# ── Start dbus ────────────────────────────────────────────────────────────────
eval $(dbus-launch --sh-syntax 2>/dev/null) || true

# ── Launch desktop session ────────────────────────────────────────────────────
if [ "$DE" = "kde" ]; then
    echo "[*] Starting KDE Plasma..."
    DISPLAY=:5 startplasma-x11 &
else
    echo "[*] Starting XFCE4..."
    DISPLAY=:5 startxfce4 &
fi
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
    -xrandr resize \
    -listen 127.0.0.1 \
    -quiet &

sleep 1
echo "[✓] ${DE} desktop running on VNC port $VNC_PORT"
echo "Switching back to Desktop tab..."
wait
