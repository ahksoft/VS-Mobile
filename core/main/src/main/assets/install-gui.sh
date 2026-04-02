#!/bin/bash
#
# Manual GUI Installation Script
# Run this script manually if automatic installation doesn't work
#

echo -e "\e[35;1m[*] \e[0mManual GUI Desktop Environment Installation\e[0m"
echo -e "\e[36;1m[i] \e[0mThis will install GNOME desktop environment\e[0m"

# Update package lists first
echo -e "\e[33;1m[1/3] \e[0mUpdating package lists...\e[0m"
DEBIAN_FRONTEND=noninteractive apt update -qq

# Add Google Chrome repository
echo -e "\e[33;1m[2/3] \e[0mAdding Google Chrome repository...\e[0m"
wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | apt-key add - >/dev/null 2>&1
echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" > /etc/apt/sources.list.d/google-chrome.list
DEBIAN_FRONTEND=noninteractive apt update -qq

# Install GUI packages - GNOME desktop
gui_packages="gnome-core gnome-terminal nautilus google-chrome-stable pulseaudio dbus-x11 xfonts-base gnome-tweaks gnome-shell-extensions"

gui_count=$(echo $gui_packages | wc -w)
echo -e "\e[35;1m[3/3] \e[0mInstalling $gui_count GUI packages...\e[0m"

current=1
for pkg in $gui_packages; do
    echo -e "\e[33;1m[$current/$gui_count] \e[0mInstalling $pkg...\e[0m"
    if DEBIAN_FRONTEND=noninteractive apt install -y -qq --no-install-recommends $pkg; then
        echo -e "\e[32;1m[✓] \e[0m$pkg installed successfully\e[0m"
    else
        echo -e "\e[31;1m[✗] \e[0mFailed to install $pkg\e[0m"
    fi
    current=$((current + 1))
done

echo -e "\e[32;1m[+] \e[0mGNOME Desktop Environment installed successfully!\e[0m"
