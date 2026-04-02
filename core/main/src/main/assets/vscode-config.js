// Configure VS Code settings for mobile
const fs = require("fs");
const path = require("path");

const HOME = process.env.HOME || "/root";
const settingsPath = path.join(HOME, ".vscode-server/data/User/settings.json");

console.log("Configuring VS Code for mobile...");

// 1. Configure default settings
try {
    let settings = {};
    try {
        if (fs.existsSync(settingsPath)) {
            settings = JSON.parse(fs.readFileSync(settingsPath, 'utf8'));
        }
    } catch(e) {
        console.log("No existing settings, creating new");
    }
    
    let needWrite = false;
    
    // Disable workspace trust (annoying on mobile)
    if (!settings.hasOwnProperty("security.workspace.trust.enabled")) {
        console.log("Setting security.workspace.trust.enabled: false");
        settings["security.workspace.trust.enabled"] = false;
        needWrite = true;
    }
    
    // Disable GPU acceleration (causes issues on some devices)
    if (!settings.hasOwnProperty("terminal.integrated.gpuAcceleration")) {
        console.log("Setting terminal.integrated.gpuAcceleration: off");
        settings["terminal.integrated.gpuAcceleration"] = "off";
        needWrite = true;
    }
    
    // Mobile-friendly settings
    if (!settings.hasOwnProperty("workbench.editor.enablePreview")) {
        console.log("Setting workbench.editor.enablePreview: false");
        settings["workbench.editor.enablePreview"] = false;
        needWrite = true;
    }
    
    if (!settings.hasOwnProperty("editor.minimap.enabled")) {
        console.log("Setting editor.minimap.enabled: false");
        settings["editor.minimap.enabled"] = false;
        needWrite = true;
    }
    
    if (!settings.hasOwnProperty("workbench.startupEditor")) {
        console.log("Setting workbench.startupEditor: none");
        settings["workbench.startupEditor"] = "none";
        needWrite = true;
    }
    
    if (needWrite) {
        const dir = path.dirname(settingsPath);
        fs.mkdirSync(dir, {recursive: true});
        fs.writeFileSync(settingsPath, JSON.stringify(settings, null, 2));
        console.log("Settings configured successfully");
    } else {
        console.log("Settings already configured");
    }
} catch(e) {
    console.error("Error configuring settings:", e.message);
}

console.log("VS Code configuration complete");
