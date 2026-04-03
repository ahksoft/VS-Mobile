#!/bin/bash
# Desktop environment launcher using WebX11

export DISPLAY=:1
WEBX11_PORT=8080

install_desktop() {
    echo "[*] Installing desktop environment..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal \
        xvfb dbus-x11 xfonts-base \
        python3 python3-pip python3-pil python3-xlib \
        git cargo rustc \
        2>/dev/null

    echo "[*] Installing WebX11 dependencies..."
    pip3 install --quiet --break-system-packages \
        "Pillow>=9.0.0" "python-xlib>=0.31" "websockets>=10.0" 2>&1

    echo "[*] Installing WebX11..."
    pip3 install --quiet --break-system-packages \
        git+https://github.com/lp1dev/WebX11.git 2>&1

    if ! python3 -c "import webx11" 2>/dev/null; then
        echo "[✗] WebX11 install failed, trying manual clone..."
        git clone --depth=1 https://github.com/lp1dev/WebX11.git /opt/webx11 2>/dev/null
        pip3 install --quiet --break-system-packages -e /opt/webx11 2>&1
    fi

    touch ~/.desktop_installed
    echo "[✓] Desktop installed"
}

start_desktop() {
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

    # Start Xvfb
    Xvfb :1 -screen 0 1280x800x24 -ac &
    sleep 2

    # Start dbus
    eval $(dbus-launch --sh-syntax 2>/dev/null) || true

    # Start Xfce
    DISPLAY=:1 startxfce4 &
    sleep 4

    # Verify WebX11 is installed
    if ! python3 -c "import webx11" 2>/dev/null; then
        echo "[✗] WebX11 not found. Run 'desktop' again to reinstall."
        rm -f ~/.desktop_installed
        exit 1
    fi

    # WebX11 settings (websocket only, no aioquic needed)
    cat > /tmp/webx11-settings.json << EOF
{
    "transport": "websocket",
    "image_quality": 75,
    "max_width": 1920,
    "max_height": 1080,
    "max_fps": 20,
    "resize_mode": "resize-x11",
    "host": "127.0.0.1",
    "image_format": "WEBP",
    "can_start_executables": false
}
EOF

    # Start WebX11
    python3 -m webx11.server \
        --settings /tmp/webx11-settings.json \
        2>&1 | tee /tmp/webx11.log &

    sleep 3

    if ! pgrep -f "webx11.server" > /dev/null; then
        echo "[✗] WebX11 failed to start:"
        cat /tmp/webx11.log
        exit 1
    fi

    echo "[✓] Desktop running on http://localhost:$WEBX11_PORT"
    echo "Switch to Desktop tab in VS Mobile"
}

if [ ! -f ~/.desktop_installed ]; then
    install_desktop
fi

start_desktop
wait
