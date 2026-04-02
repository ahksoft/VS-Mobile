#!/bin/bash
set -e

ANDROID_SDK="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== NDK ARM64 Linux Fix Script ===${NC}"

#--- Step 1: Install required packages ---
install_packages() {
    echo -e "\n${YELLOW}Step 1: Checking/Installing required packages...${NC}"
    PACKAGES="clang-19 lld-19 llvm-19 build-essential python3"
    MISSING=""
    
    for pkg in clang-19 lld-19 llvm-strip-19 make python3; do
        if ! command -v $pkg &> /dev/null; then
            MISSING="1"
        fi
    done
    
    if [ -n "$MISSING" ]; then
        echo "Installing: $PACKAGES"
        sudo apt update && sudo apt install -y $PACKAGES
    else
        echo "All required packages already installed"
    fi
}

#--- Step 2: Find NDK versions in project ---
find_project_ndk() {
    echo -e "\n${YELLOW}Step 2: Scanning project for NDK requirements...${NC}"
    
    PROJECT_NDKS=$(grep -rh "ndkVersion" --include="*.gradle" --include="*.gradle.kts" . 2>/dev/null | \
        grep -oP '"\K[0-9]+\.[0-9]+\.[0-9]+' | sort -u)
    
    if [ -n "$PROJECT_NDKS" ]; then
        echo "NDK versions found in project:"
        echo "$PROJECT_NDKS" | while read v; do echo "  - $v"; done
    else
        echo "No NDK version specified in project"
    fi
}

#--- Step 3: List installed NDKs and select ---
select_ndk() {
    echo -e "\n${YELLOW}Step 3: Available installed NDKs:${NC}"
    
    if [ ! -d "$ANDROID_SDK/ndk" ]; then
        echo -e "${RED}Error: No NDK directory found at $ANDROID_SDK/ndk${NC}"
        exit 1
    fi
    
    INSTALLED_NDKS=($(ls -1 "$ANDROID_SDK/ndk" 2>/dev/null))
    
    if [ ${#INSTALLED_NDKS[@]} -eq 0 ]; then
        echo -e "${RED}Error: No NDK installed${NC}"
        exit 1
    fi
    
    for i in "${!INSTALLED_NDKS[@]}"; do
        echo "  [$((i+1))] ${INSTALLED_NDKS[$i]}"
    done
    
    echo ""
    read -p "Select NDK version [1-${#INSTALLED_NDKS[@]}]: " choice
    
    if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le ${#INSTALLED_NDKS[@]} ]; then
        NDK_VERSION="${INSTALLED_NDKS[$((choice-1))]}"
        echo -e "${GREEN}Selected: $NDK_VERSION${NC}"
    else
        echo -e "${RED}Invalid selection${NC}"
        exit 1
    fi
}

#--- Step 4: Update project build files ---
update_project_ndk() {
    echo -e "\n${YELLOW}Step 4: Updating project NDK version to $NDK_VERSION...${NC}"
    
    find . -name "*.gradle" -o -name "*.gradle.kts" 2>/dev/null | while read f; do
        if grep -q "ndkVersion" "$f" 2>/dev/null; then
            sed -i "s/ndkVersion *= *\"[^\"]*\"/ndkVersion = \"$NDK_VERSION\"/" "$f"
            sed -i "s/ndkVersion *= *'[^']*'/ndkVersion = '$NDK_VERSION'/" "$f"
            echo "  Updated: $f"
        fi
    done
}

#--- Step 5: Patch NDK ---
patch_ndk() {
    echo -e "\n${YELLOW}Step 5: Patching NDK $NDK_VERSION for ARM64 Linux...${NC}"
    
    NDK_PATH="$ANDROID_SDK/ndk/$NDK_VERSION"
    NDK_BIN="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/bin"
    
    # Find clang version in NDK
    CLANG_VER=$(ls "$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/" 2>/dev/null | head -1)
    if [ -z "$CLANG_VER" ]; then
        echo -e "${RED}Error: Cannot detect NDK clang version${NC}"
        exit 1
    fi
    echo "  NDK Clang version: $CLANG_VER"
    
    NDK_LIB="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/$CLANG_VER/lib/linux"
    NDK_SYSROOT="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/sysroot/usr/lib"
    
    # Create clang wrapper
    echo "  Creating clang wrapper..."
    cat > "$NDK_BIN/clang-wrapper" << EOF
#!/bin/bash
SYSROOT="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
RESOURCE_DIR="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/lib/clang/$CLANG_VER"
exec /usr/bin/clang-19 --sysroot="\$SYSROOT" -resource-dir="\$RESOURCE_DIR" "\$@"
EOF
    chmod +x "$NDK_BIN/clang-wrapper"
    ln -sf clang-wrapper "$NDK_BIN/clang"
    ln -sf clang-wrapper "$NDK_BIN/clang++"
    
    # Replace linker
    echo "  Replacing linker..."
    ln -sf /usr/bin/ld.lld-19 "$NDK_BIN/ld.lld"
    ln -sf /usr/bin/ld.lld-19 "$NDK_BIN/ld"
    ln -sf /usr/bin/ld.lld-19 "$NDK_BIN/lld"
    
    # Replace llvm-strip
    echo "  Replacing llvm-strip..."
    ln -sf /usr/bin/llvm-strip-19 "$NDK_BIN/llvm-strip"
    
    # Replace make
    echo "  Replacing make..."
    ln -sf /usr/bin/make "$NDK_PATH/prebuilt/linux-x86_64/bin/make"
    
    # Replace python3
    echo "  Replacing python3..."
    ln -sf /usr/bin/python3 "$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64/python3/bin/python3"
    
    # Patch ndk_bin_common.sh
    echo "  Patching ndk_bin_common.sh..."
    NDK_SCRIPT="$NDK_PATH/build/tools/ndk_bin_common.sh"
    if [ -f "$NDK_SCRIPT" ] && grep -q "arm64) HOST_ARCH=arm64;;" "$NDK_SCRIPT"; then
        sed -i 's/arm64) HOST_ARCH=arm64;;/arm64|aarch64) HOST_ARCH=x86_64;;/' "$NDK_SCRIPT"
    fi
    
    # Create libgcc.a symlinks
    echo "  Creating libgcc.a symlinks..."
    declare -A ARCH_MAP=(
        ["aarch64-linux-android"]="aarch64"
        ["arm-linux-androideabi"]="arm"
        ["i686-linux-android"]="i386"
        ["x86_64-linux-android"]="x86_64"
    )
    for arch_dir in "${!ARCH_MAP[@]}"; do
        arch="${ARCH_MAP[$arch_dir]}"
        src="$NDK_LIB/$arch/libunwind.a"
        if [ -f "$src" ] && [ -d "$NDK_SYSROOT/$arch_dir" ]; then
            for api in $NDK_SYSROOT/$arch_dir/*/; do
                ln -sf "$src" "$api/libgcc.a" 2>/dev/null
            done
        fi
    done
}

#--- Main ---
install_packages
find_project_ndk
select_ndk
update_project_ndk
patch_ndk

echo -e "\n${GREEN}=== Done! ===${NC}"
echo "Build with: ./gradlew assembleFdroidDebug"
