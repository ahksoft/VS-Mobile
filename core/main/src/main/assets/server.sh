#!/system/bin/sh
# Start code-server outside proot

PREFIX=/data/user/0/com.ahk.uvscode.debug
INSTALL_DIR="$PREFIX/local/ubuntu/root/.local/share/code-server"
NODE="$INSTALL_DIR/node"
ENTRY="$INSTALL_DIR/code-server/release-standalone/out/node/entry.js"

echo "Checking files..."
if [ ! -f "$NODE" ]; then
    echo "Error: node not found at $NODE"
    exit 1
fi

if [ ! -f "$ENTRY" ]; then
    echo "Error: entry.js not found at $ENTRY"
    exit 1
fi

echo "Node found: $NODE"
echo "Entry found: $ENTRY"

# Set environment
export LD_LIBRARY_PATH="$INSTALL_DIR:${LD_LIBRARY_PATH}"
export PATH="$INSTALL_DIR/code-server/release-standalone/bin:$INSTALL_DIR:$PATH"
export HOME="$PREFIX/local/ubuntu/root"
export OSTYPE=linux-gnu
export SHELL="$PREFIX/local/bin/shell"

# Configure settings
$NODE <<SCRIPT
const fs = require("fs");
const path = require("path");

const PREFIX = "/data/user/0/com.ahk.uvscode.debug";
const HOME = PREFIX + "/local/ubuntu/root";
const INSTALL_DIR = PREFIX + "/local/ubuntu/root/.local/share/code-server";
const settingsPath = path.join(HOME, ".vscode-server/data/User/settings.json");
const productPath = path.join(INSTALL_DIR, "code-server/release-standalone/lib/vscode/product.json");

// Configure VS Code settings
try {
    let settings = {};
    try {
        if (fs.existsSync(settingsPath)) {
            settings = JSON.parse(fs.readFileSync(settingsPath, 'utf8'));
        }
    } catch(e) {}
    
    settings["security.workspace.trust.enabled"] = false;
    settings["terminal.integrated.gpuAcceleration"] = "off";
    
    fs.mkdirSync(path.dirname(settingsPath), {recursive: true});
    fs.writeFileSync(settingsPath, JSON.stringify(settings, null, 2));
    console.log("VS Code settings configured");
} catch(e) {
    console.log("Settings error:", e);
}

// Patch product.json to enable extensions
try {
    console.log("Checking product.json at:", productPath);
    if (fs.existsSync(productPath)) {
        const product = JSON.parse(fs.readFileSync(productPath, "utf8"));
        console.log("Product.json loaded, extensionsGallery exists:", !!product.extensionsGallery);
        
        if (product.extensionsGallery) {
            console.log("Removing extensionsGallery from product.json");
            delete product.extensionsGallery;
            fs.writeFileSync(productPath, JSON.stringify(product, null, 2));
            console.log("Extensions marketplace enabled - product.json updated");
        } else {
            console.log("extensionsGallery not found in product.json (good!)");
        }
    } else {
        console.log("WARNING: product.json not found at", productPath);
    }
} catch(e) {
    console.log("Product.json patch error:", e.message);
    console.log("Stack:", e.stack);
}

// Remove extensions gallery from code-server config.yaml
try {
    const configPath = path.join(HOME, ".config/code-server/config.yaml");
    console.log("Checking code-server config at:", configPath);
    if (fs.existsSync(configPath)) {
        let config = fs.readFileSync(configPath, "utf8");
        const lines = config.split("\n").filter(line => 
            !line.includes("extensions-gallery") && 
            !line.includes("service-url") && 
            !line.includes("item-url")
        );
        fs.writeFileSync(configPath, lines.join("\n"));
        console.log("Removed extensions gallery from config.yaml");
    }
} catch(e) {
    console.log("Config.yaml patch error:", e.message);
}
SCRIPT

# Start code-server
echo "Starting code-server..."

# Force remote extension host (not web-only mode)
export VSCODE_AGENT_FOLDER="$HOME/.vscode-server"

# Load global inject to patch Node.js platform detection
export NODE_OPTIONS="--require $PREFIX/local/globalinject.js"

echo "Node: $NODE"
echo "Entry: $ENTRY"
echo "Extensions dir: $HOME/.vscode-server/extensions"

"$NODE" "$ENTRY" \
    --bind-addr 0.0.0.0:6862 \
    --auth none \
    --disable-telemetry \
    --disable-update-check \
    --extensions-dir "$HOME/.vscode-server/extensions" \
    --user-data-dir "$HOME/.vscode-server/data" 2>&1

echo "Code-server exited with code: $?"
