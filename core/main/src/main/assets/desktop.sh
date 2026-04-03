#!/bin/bash
# Desktop environment launcher for VS Mobile
# Installs Xfce + x11vnc + noVNC and starts the desktop

export DISPLAY=:1
VNC_PORT=5901
NOVNC_PORT=6080

install_desktop() {
    echo "[*] Installing Xfce desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal \
        x11vnc xvfb \
        dbus-x11 xfonts-base xkb-data \
        novnc websockify \
        mesa-utils libgl1-mesa-dri \
        2>/dev/null
    touch ~/.desktop_installed
    echo "[✓] Desktop installed"
}

start_desktop() {
    # Kill any existing sessions
    pkill -f "Xvfb :1" 2>/dev/null || true
    pkill -f "x11vnc" 2>/dev/null || true
    pkill -f "websockify" 2>/dev/null || true
    sleep 1

    # Start Xvfb with hardware acceleration support
    Xvfb :1 -screen 0 1280x720x24 \
        +extension GLX \
        +extension RANDR \
        +extension RENDER \
        -ac &
    sleep 2

    # Set DRI for software rendering (virgl/llvmpipe)
    export LIBGL_ALWAYS_SOFTWARE=1
    export GALLIUM_DRIVER=llvmpipe

    # Start dbus
    export DBUS_SESSION_BUS_ADDRESS=$(dbus-launch --sh-syntax 2>/dev/null | grep DBUS_SESSION_BUS_ADDRESS | cut -d= -f2- | tr -d "';")

    # Start Xfce
    startxfce4 &
    sleep 3

    # Start x11vnc (no password, 127.0.0.1 only)
    x11vnc -display :1 \
        -forever \
        -nopw \
        -shared \
        -rfbport $VNC_PORT \
        -listen 127.0.0.1 \
        -quiet &
    sleep 2

    # Start noVNC websocket proxy
    websockify --web /usr/share/novnc/ $NOVNC_PORT 127.0.0.1:$VNC_PORT &

    echo "[✓] Desktop started on port $NOVNC_PORT"
    echo "Open: http://localhost:$NOVNC_PORT/vnc.html"
}

# Install if not already done
if [ ! -f ~/.desktop_installed ]; then
    install_desktop
fi

start_desktop

# Keep running
wait
