#!/bin/bash
# Desktop environment launcher using Xpra + HTML5 client

XPRA_PORT=14500
DISPLAY_NUM=10

install_desktop() {
    echo "[*] Installing Xpra + Xfce..."
    DEBIAN_FRONTEND=noninteractive apt update -qq

    # Add Xpra repo for latest version
    apt install -y --no-install-recommends wget gnupg 2>/dev/null
    wget -qO /usr/share/keyrings/xpra.asc https://xpra.org/xpra.asc 2>/dev/null || true
    echo "deb [signed-by=/usr/share/keyrings/xpra.asc] https://xpra.org/dists/bookworm/ ./"\
        > /etc/apt/sources.list.d/xpra.list 2>/dev/null || true
    apt update -qq 2>/dev/null || true

    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xpra xpra-html5 \
        xfce4 xfce4-terminal \
        xvfb dbus-x11 xfonts-base \
        python3 \
        2>/dev/null

    # Fallback if xpra repo fails
    if ! command -v xpra &>/dev/null; then
        echo "[*] Trying apt xpra..."
        DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
            xpra xpra-html5 2>/dev/null || \
        DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
            xpra 2>/dev/null
    fi

    touch ~/.desktop_installed
    echo "[✓] Installed"
}

start_desktop() {
    pkill -f "xpra" 2>/dev/null
    pkill -f "xfce4-session" 2>/dev/null
    sleep 1

    export NO_AT_BRIDGE=1
    export LIBGL_ALWAYS_SOFTWARE=1

    if ! command -v xpra &>/dev/null; then
        echo "[✗] xpra not found. Run: rm ~/.desktop_installed && desktop"
        exit 1
    fi

    echo "[*] Starting Xpra with HTML5 client on port $XPRA_PORT..."

    xpra start :$DISPLAY_NUM \
        --bind-tcp=127.0.0.1:$XPRA_PORT \
        --html=on \
        --start="startxfce4" \
        --exit-with-children=no \
        --daemon=no \
        --no-mdns \
        --pulseaudio=no \
        --notifications=no \
        --bell=no \
        --webcam=no \
        --printing=no \
        --file-transfer=no \
        --opengl=no \
        --encoding=webp \
        --quality=75 \
        --speed=75 \
        2>&1 | tee /tmp/xpra.log &

    sleep 3

    if pgrep -f "xpra" > /dev/null; then
        echo "[✓] Desktop running on http://localhost:$XPRA_PORT"
        echo "Switch to Desktop tab in VS Mobile"
    else
        echo "[✗] Xpra failed:"
        cat /tmp/xpra.log
        exit 1
    fi
}

[ ! -f ~/.desktop_installed ] && install_desktop

start_desktop
wait
