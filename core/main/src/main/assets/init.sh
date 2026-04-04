set -e  # Exit immediately on Failure

export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/share/bin:/usr/share/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
export HOME=/root

# Ensure basic dirs exist (rootfs may be freshly extracted)
mkdir -p /etc /tmp /root 2>/dev/null || true

if [ ! -s /etc/resolv.conf ]; then
    echo "nameserver 8.8.8.8" > /etc/resolv.conf
    echo "nameserver 1.1.1.1" >> /etc/resolv.conf
fi

# Fix Android group ID warnings
if [ ! -f /etc/group.bak ]; then
    cp /etc/group /etc/group.bak 2>/dev/null || true
    cat >> /etc/group << 'EOF'
android_aid_inet:x:3003:
android_aid_net_raw:x:3004:
android_aid_admin:x:3005:
android_aid_everybody:x:9997:
android_aid_misc:x:9998:
android_aid_nobody:x:9999:
android_aid_app:x:10000:
android_aid_user:x:100000:
u0_a294:x:10294:
u0_a294_cache:x:20294:
u0_a294_ext:x:30294:
u0_a294_ext_cache:x:40294:
u0_a294_shared:x:50294:
android_aid_sdcard_rw:x:1015:
android_aid_media_rw:x:1023:
android_aid_package_info:x:1032:
android_aid_sdcard_r:x:1028:
android_aid_log:x:1007:
android_aid_adb:x:1011:
android_aid_install:x:1012:
android_aid_media:x:1013:
android_aid_dhcp:x:1014:
android_aid_shell:x:2000:
android_aid_cache:x:2001:
android_aid_diag:x:2002:
android_aid_oem_reserved_start:x:2900:
android_aid_oem_reserved_end:x:2999:
android_aid_net_bt_admin:x:3001:
android_aid_net_bt:x:3002:
android_aid_net_bt_stack:x:3008:
android_aid_readproc:x:3009:
android_aid_wakelock:x:3010:
android_aid_uhid:x:3011:
all_a1077:x:1077:
EOF
fi

export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@ubuntu \[\033[39m\]\w \[\033[0m\]\\$ "
export PIP_BREAK_SYSTEM_PACKAGES=1

# Suppress group ID warnings
export GROUPS=""

# GUI installation function
install_gui_mode() {
    echo -e "\e[35;1m[*] \e[0mInstalling Complete GNOME Desktop Environment...\e[0m"
    
    # Update package lists first
    echo -e "\e[33;1m[*] \e[0mUpdating package lists...\e[0m"
    DEBIAN_FRONTEND=noninteractive apt update -qq >/dev/null 2>&1
    
    # Install complete GNOME desktop with all components
    gnome_packages="ubuntu-desktop-minimal gnome-session gnome-shell gnome-terminal nautilus firefox-esr gnome-control-center gnome-tweaks gnome-shell-extensions gnome-themes-extra adwaita-icon-theme"
    
    echo -e "\e[33;1m[*] \e[0mInstalling complete GNOME desktop...\e[0m"
    for pkg in $gnome_packages; do
        echo -e "\e[33;1m[*] \e[0mInstalling $pkg...\e[0m"
        if DEBIAN_FRONTEND=noninteractive apt install -y -qq --no-install-recommends $pkg >/dev/null 2>&1; then
            echo -e "\e[32;1m[✓] \e[0m$pkg installed\e[0m"
        else
            echo -e "\e[31;1m[✗] \e[0mFailed to install $pkg\e[0m"
        fi
    done
    
    # Install audio and additional components
    extra_packages="pulseaudio pulseaudio-utils dbus-x11 xfonts-base fonts-dejavu-core"
    echo -e "\e[33;1m[*] \e[0mInstalling additional components...\e[0m"
    for pkg in $extra_packages; do
        if DEBIAN_FRONTEND=noninteractive apt install -y -qq --no-install-recommends $pkg >/dev/null 2>&1; then
            echo -e "\e[32;1m[✓] \e[0m$pkg installed\e[0m"
        else
            echo -e "\e[33;1m[!] \e[0m$pkg installation skipped\e[0m"
        fi
    done
    
    # Create GUI installation flag
    touch ~/.gui_installed
    
    echo -e "\e[32;1m[+] \e[0mGUI Desktop Environment installed successfully!\e[0m"
}

required_packages="bash sudo nano curl wget git python3 ca-certificates micro build-essential make gdb"

# Add zsh if selected
if [ "${DEFAULT_SHELL:-0}" = "1" ]; then
    required_packages="$required_packages zsh"
fi

# Check for missing packages (always check, even if flag exists)
missing_packages=""
for pkg in $required_packages; do
    if ! dpkg -l | grep -q "^ii  $pkg "; then
        missing_packages="$missing_packages $pkg"
    fi
done

if [ -n "$missing_packages" ]; then
    total_packages=$(echo $missing_packages | wc -w)
    
    LOG_FILE="/tmp/install.log"
    > "$LOG_FILE"
    
    echo -e "\e[33;1m[1/$((total_packages + 1))] \e[0mUpdating package lists...\e[0m"
    if ! DEBIAN_FRONTEND=noninteractive apt update >> "$LOG_FILE" 2>&1; then
        echo -e "\e[31;1m[!] \e[0mFailed to update\e[0m"
        echo -e "\e[33;1m[!] \e[0mContinuing without installing packages...\e[0m"
    else
        current=2
        installed_list=""
        for pkg in $missing_packages; do
            # Clear and show header
            printf "\033c"
            echo -e "\e[34;1m[*] \e[0mInstalling $total_packages packages\e[0m"
            
            # Show progress bar
            progress=$((current * 30 / (total_packages + 1)))
            printf "\e[33;1m[$current/$((total_packages + 1))]\e[0m \e[32m["
            for i in $(seq 1 $progress); do printf "█"; done
            printf "\e[37m"
            for i in $(seq $((progress + 1)) 30); do printf "░"; done
            printf "]\e[0m Installing $pkg...\n"
            
            # Show all previously installed packages
            if [ -n "$installed_list" ]; then
                echo "$installed_list"
            fi
            
            # Install and show realtime log
            DEBIAN_FRONTEND=noninteractive apt install -y --no-install-recommends $pkg 2>&1 | tee -a "$LOG_FILE"
            
            # Add to installed list
            installed_list="${installed_list}\e[32;1m✓\e[0m $pkg installed\n"
            current=$((current + 1))
        done
        
        # Final screen
        printf "\033c"
        echo -e "\e[34;1m[*] \e[0mInstalling $total_packages packages\e[0m"
        echo -e "\e[32;1m[+]\e[0m Installation complete \e[32m["
        for i in $(seq 1 30); do printf "█"; done
        printf "]\e[0m\n"
        echo -e "$installed_list"
        
        touch ~/.packages_installed
    fi
    
    # Install Zsh configuration if Zsh is selected
    if [ "${DEFAULT_SHELL:-0}" = "1" ] && command -v zsh >/dev/null 2>&1; then
        if [ ! -d ~/.oh-my-zsh ]; then
            echo -e "\e[34;1m[*] \e[0mSetting up Zsh configuration...\e[0m"
            update-ca-certificates >/dev/null 2>&1 || true
            curl -fsSL https://raw.githubusercontent.com/ahksoft/AiDevSpace-resources/refs/heads/main/zsh-setup.sh | bash
        fi
    fi
    
    # Start VS Code setup after Ubuntu setup completes
    # Check if embedded code-server exists first
    if [ -f /root/.local/share/code-server/node ] && [ -f /root/.local/share/code-server/code-server/release-standalone/out/node/entry.js ]; then
        echo -e "\e[32;1m[✓] \e[0mEmbedded code-server found\e[0m"
        
        # Apply mobile patches if not already done
        if [ ! -f ~/.cs_mobile_patched ]; then
            echo -e "\e[33;1m[*] \e[0mApplying mobile patches...\e[0m"
            if curl -fsSL https://raw.githubusercontent.com/ahksoft/AiDevSpace-resources/refs/heads/main/scripts/cs-mobile-patch.sh -o /tmp/cs-mobile-patch.sh; then
                chmod +x /tmp/cs-mobile-patch.sh
                sh /tmp/cs-mobile-patch.sh patch --yes 2>&1 && touch ~/.cs_mobile_patched
                rm -f /tmp/cs-mobile-patch.sh
                echo -e "\e[32;1m[✓] \e[0mMobile patches applied\e[0m"
            else
                echo -e "\e[31;1m[✗] \e[0mFailed to download patch script\e[0m"
            fi
        fi
        
        touch ~/.vscode_setup_done
    elif [ ! -f ~/.vscode_setup_done ]; then
        echo -e "\e[35;1m[*] \e[0mStarting VS Code setup...\e[0m"
        echo -e "\e[33;1m[*] \e[0mDownloading setup script from GitHub...\e[0m"
        
        if curl -fsSL https://raw.githubusercontent.com/ahksoft/AiDevSpace-resources/refs/heads/main/scripts/vscode-mobile.sh -o /tmp/setup-vscode.sh; then
            chmod +x /tmp/setup-vscode.sh
            echo -e "\e[32;1m[✓] \e[0mSetup script downloaded\e[0m"
            bash /tmp/setup-vscode.sh
            rm -f /tmp/setup-vscode.sh
        else
            echo -e "\e[31;1m[✗] \e[0mFailed to download VS Code setup script\e[0m"
        fi
    fi
fi

# Show Shizuku status briefly
if [ "$SHIZUKU_AVAILABLE" = "1" ]; then
    echo -e "\e[32m[✓] Shizuku available - elevated permissions enabled\e[0m"
else
    echo -e "\e[33m[!] Shizuku not available - limited to app sandbox\e[0m"
fi

# Fix linker warning
if [[ ! -f /linkerconfig/ld.config.txt ]]; then
    mkdir -p /linkerconfig
    touch /linkerconfig/ld.config.txt
fi

if [ "$#" -eq 0 ]; then
    source /etc/profile 2>/dev/null || true
    export PS1="\[\e[38;5;46m\]\u\[\033[39m\]@ubuntu \[\033[39m\]\w \[\033[0m\]\\$ "
  cd $HOME
    
    # Show neofetch and developer info first
    #if command -v neofetch >/dev/null 2>&1; then
        #neofetch --ascii_distro ubuntu_small
    #fi
    echo -e "\n\e[36mThis app Develop and maintenance by Abir Hasan AHK.\e[0m"
    echo -e "\e[34mGitHub: https://github.com/ahksoft\e[0m\n"
    
    # Check for shell preference (0=bash, 1=zsh)
    SHELL_PREF=${DEFAULT_SHELL:-0}
    
    if [ "$SHELL_PREF" = "1" ]; then
        if [ -x "/usr/bin/zsh" ]; then
            exec /usr/bin/zsh
        else
            exec /usr/bin/bash
        fi
    else
        exec /usr/bin/bash
    fi
else
    exec "$@"
fi
