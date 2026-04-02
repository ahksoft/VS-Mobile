#!/system/bin/sh
# Start code-server outside proot (like VSDevEditor does)

PREFIX=/data/user/0/com.ahk.uvscode.debug
INSTALL_DIR="$PREFIX/local/ubuntu/root/.local/share/code-server"
NODE="$INSTALL_DIR/node"
ENTRY="$INSTALL_DIR/code-server/release-standalone/out/node/entry.js"

if [ ! -f "$NODE" ] || [ ! -f "$ENTRY" ]; then
    echo "Error: code-server not found"
    echo "  node:  $NODE"
    echo "  entry: $ENTRY"
    exit 1
fi

# Set environment FIRST
export LD_LIBRARY_PATH="$INSTALL_DIR:${LD_LIBRARY_PATH}"
export PATH="$INSTALL_DIR/code-server/release-standalone/bin:$INSTALL_DIR:$PATH"
export HOME="$PREFIX/local/ubuntu/root"
export SHELL="/bin/bash"

# Configure settings
$NODE <<SCRIPT
const fs = require("fs");
const path = require("path");

const HOME = "$PREFIX/local/ubuntu/root";
const settingsPath = path.join(HOME, ".vscode-server/data/User/settings.json");

try {
    let settings = {};
    try {
        if (fs.existsSync(settingsPath)) {
            settings = JSON.parse(fs.readFileSync(settingsPath, 'utf8'));
        }
    } catch(e) {}
    
    let needWrite = false;
    if (!settings.hasOwnProperty("security.workspace.trust.enabled")) {
        settings["security.workspace.trust.enabled"] = false;
        needWrite = true;
    }
    if (!settings.hasOwnProperty("terminal.integrated.gpuAcceleration")) {
        settings["terminal.integrated.gpuAcceleration"] = "off";
        needWrite = true;
    }
    if (!settings.hasOwnProperty("terminal.integrated.defaultProfile.linux")) {
        settings["terminal.integrated.defaultProfile.linux"] = "proot-ubuntu";
        needWrite = true;
    }
    if (!settings.hasOwnProperty("terminal.integrated.profiles.linux")) {
        settings["terminal.integrated.profiles.linux"] = {
            "proot-ubuntu": {
                "path": "$PREFIX/local/bin/proot-shell"
            }
        };
        needWrite = true;
    }
    
    if (needWrite) {
        fs.mkdirSync(path.dirname(settingsPath), {recursive: true});
        fs.writeFileSync(settingsPath, JSON.stringify(settings, null, 2));
        console.log("VS Code settings configured");
    }
} catch(e) {
    console.log("Error configuring settings:", e);
}
SCRIPT

# Start code-server
echo "Starting code-server..."
exec "$NODE" "$ENTRY" \
    --bind-addr 0.0.0.0:6862 \
    --auth none \
    --disable-telemetry \
    --disable-update-check
