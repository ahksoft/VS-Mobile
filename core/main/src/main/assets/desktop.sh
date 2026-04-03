#!/bin/bash
# Desktop environment launcher using WebX11 (no VNC needed)

export DISPLAY=:1
WEBX11_PORT=8080

install_desktop() {
    echo "[*] Installing desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal \
        xvfb dbus-x11 xfonts-base \
        python3 python3-pip python3-pil \
        git \
        2>/dev/null

    echo "[*] Installing WebX11..."
    pip3 install --quiet git+https://github.com/lp1dev/WebX11.git 2>/dev/null || \
    pip3 install --quiet --break-system-packages git+https://github.com/lp1dev/WebX11.git 2>/dev/null

    touch ~/.desktop_installed
    echo "[✓] Desktop installed"
}

start_desktop() {
    # Kill existing
    pkill -f "Xvfb :1" 2>/dev/null
    pkill -f "webx11" 2>/dev/null
    pkill -f "xfce4-session" 2>/dev/null
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

    # WebX11 settings
    cat > /tmp/webx11-settings.json << EOF
{
    "transport": "websocket",
    "image_quality": 75,
    "max_width": 1920,
    "max_height": 1080,
    "max_fps": 20,
    "resize_mode": "resize-x11",
    "host": "127.0.0.1",
    "image_format": "WEBP"
}
EOF

    # Start Xvfb
    Xvfb :1 -screen 0 1280x800x24 -ac &
    sleep 2

    # Start dbus
    eval $(dbus-launch --sh-syntax 2>/dev/null) || true

    # Start Xfce
    DISPLAY=:1 startxfce4 &
    sleep 4

    # Start WebX11
    cd /tmp && python3 -m webx11.server \
        --settings /tmp/webx11-settings.json \
        --port $WEBX11_PORT \
        2>/tmp/webx11.log &

    sleep 2
    echo "[✓] Desktop running on port $WEBX11_PORT"
    echo "Open: http://localhost:$WEBX11_PORT"
}

[ ! -f ~/.desktop_installed ] && install_desktop

start_desktop
wait
