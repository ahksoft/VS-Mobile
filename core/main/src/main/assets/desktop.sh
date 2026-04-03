#!/bin/bash
# Desktop environment launcher for VS Mobile

export DISPLAY=:1
VNC_PORT=5901
NOVNC_PORT=6080

install_desktop() {
    echo "[*] Installing desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal \
        x11vnc xvfb \
        dbus-x11 xfonts-base \
        novnc websockify \
        2>/dev/null
    touch ~/.desktop_installed
    echo "[✓] Desktop installed"
}

start_desktop() {
    # Kill existing
    pkill -f "Xvfb :1" 2>/dev/null; pkill -f "x11vnc" 2>/dev/null
    pkill -f "websockify" 2>/dev/null; pkill -f "xfce4-session" 2>/dev/null
    sleep 1

    export NO_AT_BRIDGE=1
    export LIBGL_ALWAYS_SOFTWARE=1

    # Disable xfwm4 compositing
    mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
    cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
  </property>
</channel>
EOF

    # Start Xvfb
    Xvfb :1 -screen 0 1280x800x24 -ac &
    sleep 2

    # Start dbus
    eval $(dbus-launch --sh-syntax 2>/dev/null) || true

    # Start Xfce
    startxfce4 --display :1 &
    sleep 5

    # Start x11vnc in foreground (background via &)
    x11vnc -display :1 \
        -forever \
        -nopw \
        -shared \
        -rfbport $VNC_PORT \
        -listen 127.0.0.1 \
        -noxdamage \
        -noxfixes \
        -noipv6 \
        -noshm \
        2>/tmp/x11vnc.log &
    sleep 3

    # Check if x11vnc is running
    if ! pgrep -x x11vnc > /dev/null; then
        echo "[✗] x11vnc failed:"
        cat /tmp/x11vnc.log
        exit 1
    fi

    # Find noVNC web root
    NOVNC_WEB=""
    for p in /usr/share/novnc /usr/share/novnc/utils /usr/share/novnc/app; do
        [ -f "$p/vnc.html" ] && NOVNC_WEB="$p" && break
        [ -f "$p/index.html" ] && NOVNC_WEB="$p" && break
    done
    [ -z "$NOVNC_WEB" ] && NOVNC_WEB=/usr/share/novnc

    # Start noVNC
    websockify --web "$NOVNC_WEB" $NOVNC_PORT 127.0.0.1:$VNC_PORT &

    echo "[✓] Desktop running — open Desktop tab in VS Mobile"
}

[ ! -f ~/.desktop_installed ] && install_desktop

start_desktop
wait
