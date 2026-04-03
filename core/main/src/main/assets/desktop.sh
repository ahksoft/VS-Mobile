#!/bin/bash
# Start Xfce desktop using termux-x11 as the X server
# termux-x11 renders directly to Android SurfaceView - no VNC/WebView needed

DISPLAY_NUM=0

start_desktop() {
    pkill -f "xfce4-session" 2>/dev/null
    pkill -f "termux-x11" 2>/dev/null
    sleep 1

    export NO_AT_BRIDGE=1
    export DISPLAY=:$DISPLAY_NUM

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

    # Set XKB config root for proot
    export XKB_CONFIG_ROOT=/usr/share/X11/xkb
    export TMPDIR=/tmp

    # Start termux-x11 X server (runs as Android app_process)
    # This sends ACTION_START broadcast to open the X11 display Activity
    export CLASSPATH=$(pm path com.termux.x11 2>/dev/null | cut -d: -f2)

    if [ -z "$CLASSPATH" ]; then
        echo "[✗] termux-x11 not installed. Open Desktop tab in VS Mobile to install it first."
        exit 1
    fi

    echo "[*] Starting termux-x11 X server..."
    /system/bin/app_process / com.termux.x11.CmdEntryPoint :$DISPLAY_NUM &
    sleep 2

    echo "[*] Starting Xfce desktop..."
    dbus-launch --exit-with-session xfce4-session &

    echo "[✓] Desktop started on DISPLAY=:$DISPLAY_NUM"
    echo "The X11 display window should open automatically."
    wait
}

# Install Xfce if needed
if ! command -v xfce4-session &>/dev/null; then
    echo "[*] Installing Xfce..."
    DEBIAN_FRONTEND=noninteractive apt update -qq
    DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends \
        xfce4 xfce4-terminal dbus-x11 xfonts-base 2>/dev/null
fi

start_desktop
