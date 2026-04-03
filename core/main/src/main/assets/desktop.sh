#!/bin/bash
# Desktop environment launcher for VS Mobile
# Installs Xfce + x11vnc + noVNC and starts the desktop

export DISPLAY=:1
VNC_PORT=5901
NOVNC_PORT=6080

install_desktop() {
    echo "[*] Installing desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal \
        x11vnc xvfb \
        dbus-x11 xfonts-base xkb-data \
        novnc websockify \
        2>/dev/null
    touch ~/.desktop_installed
    echo "[✓] Desktop installed"
}

start_desktop() {
    # Kill any existing sessions
    pkill -f "Xvfb :1" 2>/dev/null || true
    pkill -f "x11vnc" 2>/dev/null || true
    pkill -f "websockify" 2>/dev/null || true
    pkill -f "xfce4-session" 2>/dev/null || true
    sleep 1

    # Disable GL/compositing to avoid llvmpipe rejection
    export LIBGL_ALWAYS_SOFTWARE=1
    export GALLIUM_DRIVER=softpipe
    export XFWM4_USE_COMPOSITING=0

    # Start Xvfb
    Xvfb :1 -screen 0 1280x800x24 -ac -nolisten tcp &
    XVFB_PID=$!
    sleep 2

    # Verify Xvfb started
    if ! kill -0 $XVFB_PID 2>/dev/null; then
        echo "[✗] Xvfb failed to start"
        exit 1
    fi

    # Start dbus
    eval $(dbus-launch --sh-syntax 2>/dev/null) || true

    # Disable xfwm4 compositing via config
    mkdir -p ~/.config/xfce4/xfconf/xfce-perchannel-xml
    cat > ~/.config/xfce4/xfconf/xfce-perchannel-xml/xfwm4.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<channel name="xfwm4" version="1.0">
  <property name="general" type="empty">
    <property name="use_compositing" type="bool" value="false"/>
  </property>
</channel>
EOF

    # Start Xfce session
    startxfce4 --display :1 &
    sleep 4

    # Start x11vnc
    x11vnc -display :1 \
        -forever \
        -nopw \
        -shared \
        -rfbport $VNC_PORT \
        -listen 127.0.0.1 \
        -bg \
        -o /tmp/x11vnc.log \
        -quiet

    sleep 1

    # Verify x11vnc started
    if ! grep -q "Listening" /tmp/x11vnc.log 2>/dev/null && ! netstat -tlnp 2>/dev/null | grep -q ":$VNC_PORT"; then
        echo "[!] x11vnc log:"
        cat /tmp/x11vnc.log 2>/dev/null
    fi

    # Start noVNC
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
