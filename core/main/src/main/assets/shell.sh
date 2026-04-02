#!/system/bin/sh
# Wrapper to launch shell inside proot Ubuntu for code-server terminals

#PREFIX=/data/user/0/com.ahk.uvscode.debug
UBUNTU_DIR=$PREFIX/local/ubuntu
# Check if proot exists
if [ ! -f "$PREFIX/local/bin/proot" ]; then
    echo "Error: proot not found" >&2
    exit 127
fi

# Check if Ubuntu exists
if [ ! -d "$PREFIX/local/ubuntu" ]; then
    echo "Error: Ubuntu not found" >&2
    exit 127
fi

LINKER=""
if [ -f "$PREFIX/local/LINKER" ]; then
    LINKER=$(cat "$PREFIX/local/LINKER")
fi

# Minimal proot args
ARGS="-r $PREFIX/local/ubuntu"
ARGS="$ARGS -b /proc -b /dev -b /sys -b $PREFIX"
ARGS="$ARGS -0 --link2symlink --sysvipc -L"

# Execute with proper environment
if [ -n "$LINKER" ] && [ -f "$LINKER" ]; then
    exec "$LINKER" "$PREFIX/local/bin/proot" $ARGS /bin/bash -c '
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin
export HOME="$PREFIX/local/ubuntu/root"
export TERM=xterm-256color
export TMPDIR=/tmp
export LANG=en_US.UTF-8
export GROUPS=""
exec /bin/bash -l "$@"
' -- "$@"
else
    exec "$PREFIX/local/bin/proot" $ARGS /bin/bash -c '
export PATH=/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin
export HOME="$PREFIX/local/ubuntu/root"
export TERM=xterm-256color
export TMPDIR=/tmp
export LANG=en_US.UTF-8
export GROUPS=""
exec /bin/bash -l "$@"
' -- "$@"
fi
