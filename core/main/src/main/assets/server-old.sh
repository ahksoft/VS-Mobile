UBUNTU_DIR=$PREFIX/local/ubuntu


if [ -z "$(ls -A "$UBUNTU_DIR" | grep -vE '^(root|tmp)$')" ]; then
    tar --no-same-owner --no-same-permissions -xf "$PREFIX/files/ubuntu.tar.gz" -C "$UBUNTU_DIR" 2>/dev/null || true
fi

[ ! -e "$PREFIX/local/bin/proot" ] && cp "$PREFIX/files/proot" "$PREFIX/local/bin"

for sofile in "$PREFIX/files/"*.so.2; do                                                                                                                    dest="$PREFIX/local/lib/$(basename "$sofile")"
    [ ! -e "$dest" ] && cp "$sofile" "$dest"
done

ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

for system_mnt in /apex /odm /product /system /system_ext /vendor \                                                                                      /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do
                                                                                                                                                         if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"

if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi

ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b /sys"

if [ ! -d "$PREFIX/local/ubuntu/tmp" ]; then
 mkdir -p "$PREFIX/local/ubuntu/tmp"
 chmod 1777 "$PREFIX/local/ubuntu/tmp"
fi
ARGS="$ARGS -b $PREFIX/local/ubuntu/tmp:/dev/shm"

ARGS="$ARGS -r $PREFIX/local/ubuntu"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

$LINKER $PREFIX/local/bin/proot $ARGS sh $PREFIX/local/bin/init "$@" &                    
# Wait container to initialize
sleep 0.1

exec $PREFIX/local/bin/proot $ARGS /bin/bash -c '
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin
export HOME=/root
export TERM=xterm-256color
export TMPDIR=/tmp
export LANG=en_US.UTF-8
export GROUPS=""

INSTALL_DIR="/root/.local/share/code-server"
NODE="$INSTALL_DIR/node"
ENTRY="$INSTALL_DIR/code-server/release-standalone/out/node/entry.js"

if [ -f "$NODE" ] && [ -f "$ENTRY" ]; then
    echo "Using embedded code-server"
    export LD_LIBRARY_PATH="$INSTALL_DIR${LD_LIBRARY_PATH:+:}${LD_LIBRARY_PATH}"
    export PATH="$INSTALL_DIR/code-server/release-standalone/bin:$INSTALL_DIR:$PATH"
    
    # Use proot-shell wrapper for terminals
    export SHELL=/data/user/0/com.ahk.uvscode.debug/local/bin/proot-shell
    
    # Force platform detection to Linux
    export OSTYPE=linux-gnu
    
    # Disable separate pty host process (causes issues on Android)
    export VSCODE_DISABLE_SEPARATE_PTY_HOST=1
    
    # Configure VS Code default settings
    echo "Configuring VS Code settings..."
    "$NODE" <<'VSCODE_CONFIG'
const fs = require("fs");
const path = require("path");

try {
    const settingsDir = "/root/.local/share/code-server/User";
    const settingsFile = path.join(settingsDir, "settings.json");
    
    let settings = {};
    try {
        settings = JSON.parse(fs.readFileSync(settingsFile, "utf8"));
    } catch(e) {}
    
    let needWrite = false;
    
    // Disable workspace trust (causes issues on mobile)
    if (!settings.hasOwnProperty("security.workspace.trust.enabled")) {
        console.log("Setting security.workspace.trust.enabled: false");
        settings["security.workspace.trust.enabled"] = false;
        needWrite = true;
    }
    
    // Disable GPU acceleration for terminal (Android compatibility)
    if (!settings.hasOwnProperty("terminal.integrated.gpuAcceleration")) {
        console.log("Setting terminal.integrated.gpuAcceleration: off");
        settings["terminal.integrated.gpuAcceleration"] = "off";
        needWrite = true;
    }
    
    if (needWrite) {
        fs.mkdirSync(settingsDir, { recursive: true });
        fs.writeFileSync(settingsFile, JSON.stringify(settings, null, 2));
        console.log("VS Code settings configured");
    }
} catch(e) {
    console.error("Error configuring settings:", e);
}
VSCODE_CONFIG

    # Patch product.json to enable extensions (runtime patch)
    echo "Patching product.json for extensions..."
    "$NODE" <<'PRODUCT_PATCH'
const fs = require("fs");
const productPath = "$INSTALL_DIR/code-server/release-standalone/lib/vscode/product.json";

try {
    const product = JSON.parse(fs.readFileSync(productPath, "utf8"));
    
    // DELETE extensionsGallery to let VS Code use built-in Open VSX
    if (product.extensionsGallery) {
        console.log("Removing extensionsGallery from product.json");
        delete product.extensionsGallery;
        fs.writeFileSync(productPath, JSON.stringify(product, null, 2));
        console.log("Extensions marketplace enabled");
    } else {
        console.log("extensionsGallery already removed");
    }
} catch(e) {
    console.error("Error patching product.json:", e);
}
PRODUCT_PATCH
    
    # Start code-server (uses ~/.vscode-server/data/ by default)
    exec "$NODE" "$ENTRY" --bind-addr 0.0.0.0:6862 --auth none --extensions-dir /root/.vscode-server/extensions
fi

# Fallback: run vsc.sh if exists
if [ -f /bin/vsc ]; then
    /bin/vsc
else
    echo "code-server not found, starting bash shell"
    exec /bin/bash
fi
'
