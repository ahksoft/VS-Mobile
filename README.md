# VS Mobile

Run **Visual Studio Code** on your Android device — no PC required.

VS Mobile bundles a full Linux environment (via proot) and serves VS Code through a built-in WebView, giving you a real code editor on your phone or tablet.

---

## Features

- **Full VS Code** via code-server running inside a proot Ubuntu environment
- **Built-in terminal** with multiple sessions
- **WebView interface** — no external browser needed
- **Persistent background service** — keeps running when you switch apps
- **Offline page** with one-tap VS Code start button
- **Virtual mouse** support for precise cursor control

---

## Requirements

- Android 8.0+ (API 26)
- arm64-v8a device (64-bit)
- ~500MB free storage

---

## Build

### Debug
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/Fdroid/debug/app-Fdroid-debug.apk
```

### Release
Create `app/signing.properties`:
```properties
keyAlias=your-key-alias
keyPassword=your-key-password
storeFile=/path/to/your.keystore
storePassword=your-store-password
```

Then:
```bash
./gradlew assembleFdroidRelease
```

> **Note:** `cs-aarch64.tgz` (the Linux rootfs) is downloaded automatically from [vscode-android-server releases](https://github.com/ahksoft/vscode-android-server/releases) during build. No manual download needed.

---

## How It Works

1. On first launch, the app extracts a pre-built Ubuntu arm64 rootfs
2. `code-server` runs inside the proot environment on `localhost:6862`
3. The built-in WebView connects to it automatically
4. Terminal sessions run alongside VS Code in the same environment

---

## Project Structure

```
VSMobile/
├── app/                        # Main application module
├── core/
│   ├── main/                   # Core app logic, WebView, assets
│   ├── terminal-emulator/      # Terminal emulator (native)
│   ├── terminal-view/          # Terminal UI
│   ├── components/             # Shared UI components
│   └── resources/              # Shared resources
└── webview-virtualmouse-plugin/ # Virtual mouse plugin
```

---

## Related

- [vscode-android-server](https://github.com/ahksoft/vscode-android-server) — pre-built code-server rootfs

---

## Developer

**Abir Hasan AHK** — [github.com/ahksoft](https://github.com/ahksoft)

---

## License

[MIT](LICENSE)
