#!/bin/bash
# Desktop environment using WebX11 (direct X11 → WebSocket streaming)

WEBX11_PORT=8080

install_desktop() {
    echo "[*] Installing dependencies..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal xvfb dbus-x11 xfonts-base \
        python3 python3-pip git \
        2>/dev/null

    echo "[*] Installing Python deps..."
    pip3 install --quiet --break-system-packages \
        "Pillow>=9.0.0" "python-xlib>=0.31" "websockets>=10.0" 2>/dev/null

    echo "[*] Installing WebX11..."
    rm -rf /opt/webx11
    git clone --depth=1 https://github.com/lp1dev/WebX11.git /opt/webx11 2>/dev/null

    pip3 install --quiet --break-system-packages --no-deps -e /opt/webx11 2>/dev/null

    if python3 -c "import webx11" 2>/dev/null; then
        echo "[✓] WebX11 installed"
    else
        echo "[✗] WebX11 install failed"
        exit 1
    fi

    touch ~/.desktop_installed
}

start_desktop() {
    pkill -f "xfce4-session" 2>/dev/null
    pkill -f "webx11.server" 2>/dev/null
    pkill -f "Xvfb :1" 2>/dev/null
    sleep 1

    # Always patch webtransport.py stub
    [ -f /opt/webx11/webx11/webtransport.py ] && cat > /opt/webx11/webx11/webtransport.py << 'STUB'
# WebTransport disabled (aioquic not available)
async def run_webtransport_server(*args, **kwargs):
    pass
STUB

    export NO_AT_BRIDGE=1
    export LIBGL_ALWAYS_SOFTWARE=1
    export DISPLAY=:1

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
    startxfce4 &
    sleep 4

    # WebX11 config (websocket only)
    cat > /tmp/webx11.json << EOF
{
    "transport": "websocket",
    "image_quality": 80,
    "max_width": 1920,
    "max_height": 1080,
    "max_fps": 24,
    "resize_mode": "resize-x11",
    "host": "127.0.0.1",
    "image_format": "WEBP",
    "can_start_executables": false
}
EOF

    # Start WebX11
    python3 -m webx11.server --settings /tmp/webx11.json 2>&1 | tee /tmp/webx11.log &
    sleep 3

    if pgrep -f "webx11.server" > /dev/null; then
        echo "[✓] Desktop running on http://localhost:$WEBX11_PORT"
        echo "Switch to Desktop tab in VS Mobile"
    else
        echo "[✗] WebX11 failed:"
        cat /tmp/webx11.log
        exit 1
    fi
}

[ ! -f ~/.desktop_installed ] && install_desktop

start_desktop
wait
