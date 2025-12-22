#!/bin/bash
set -e

# Script to build GLFW3 as static library and create shared library
# The library is named libglfw.so/dylib/dll so jextract bindings can find it
# Usage: ./scripts/build-native.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_DIR/native"
BUILD_DIR="$NATIVE_DIR/build"

# Check for required dependencies on Linux
if [[ "$(uname -s)" == "Linux"* ]]; then
    echo "Checking for required development packages..."
    MISSING_DEPS=()
    
    # Wayland packages (preferred) - ECM, pkg-config, and wayland-protocols are required for Wayland support
    for pkg in libwayland-dev libxkbcommon-dev extra-cmake-modules pkg-config wayland-protocols; do
        if ! dpkg -l "$pkg" 2>/dev/null | grep -q "^ii"; then
            MISSING_DEPS+=("$pkg")
        fi
    done
    
    # X11 packages (fallback)
    for pkg in libxinerama-dev libxrandr-dev libxi-dev libxcursor-dev libx11-dev; do
        if ! dpkg -l "$pkg" 2>/dev/null | grep -q "^ii"; then
            MISSING_DEPS+=("$pkg")
        fi
    done
    
    if [ ${#MISSING_DEPS[@]} -gt 0 ]; then
        echo "✗ Missing required packages:"
        for pkg in "${MISSING_DEPS[@]}"; do
            echo "  - $pkg"
        done
        echo ""
        echo "Install with: sudo apt-get install ${MISSING_DEPS[*]}"
        exit 1
    fi
    echo "✓ All required packages are installed"
fi

# GLFW version to download
GLFW_VERSION="3.3.9"
GLFW_URL="https://github.com/glfw/glfw/releases/download/${GLFW_VERSION}/glfw-${GLFW_VERSION}.zip"

# Detect platform
OS="$(uname -s)"
ARCH="$(uname -m)"

echo "Building native libraries for platform: $OS $ARCH"

# Determine platform-specific settings
case "$OS" in
    Linux*)
        PLATFORM="linux-x64"
        SHARED_LIB="libglfw.so"
        CMAKE_GENERATOR="Unix Makefiles"
        ;;
    Darwin*)
        PLATFORM="macos-x64"
        SHARED_LIB="libglfw.dylib"
        CMAKE_GENERATOR="Unix Makefiles"
        ;;
    MINGW*|MSYS*|CYGWIN*)
        PLATFORM="windows-x64"
        SHARED_LIB="glfw.dll"
        CMAKE_GENERATOR="MinGW Makefiles"
        ;;
    *)
        echo "Unsupported platform: $OS"
        exit 1
        ;;
esac

PLATFORM_BUILD_DIR="$BUILD_DIR/$PLATFORM"
mkdir -p "$PLATFORM_BUILD_DIR"

echo "Platform build directory: $PLATFORM_BUILD_DIR"

# Download and extract GLFW
GLFW_SRC_DIR="$NATIVE_DIR/glfw-src"
if [ ! -d "$GLFW_SRC_DIR" ]; then
    echo ""
    echo "Downloading GLFW ${GLFW_VERSION}..."
    GLFW_ZIP="$NATIVE_DIR/glfw-${GLFW_VERSION}.zip"
    
    if [ ! -f "$GLFW_ZIP" ]; then
        curl -L -o "$GLFW_ZIP" "$GLFW_URL"
    fi
    
    echo "Extracting GLFW..."
    unzip -q "$GLFW_ZIP" -d "$NATIVE_DIR"
    mv "$NATIVE_DIR/glfw-${GLFW_VERSION}" "$GLFW_SRC_DIR"
    echo "✓ GLFW extracted to $GLFW_SRC_DIR"
else
    echo "✓ GLFW source already exists at $GLFW_SRC_DIR"
fi

# Build GLFW as static library
GLFW_BUILD_DIR="$NATIVE_DIR/glfw-build"
mkdir -p "$GLFW_BUILD_DIR"

echo ""
echo "Building GLFW as static library..."
echo "Note: GLFW will prefer Wayland if available, falling back to X11"
cd "$GLFW_BUILD_DIR"

# Try to configure GLFW
# GLFW will auto-detect Wayland and X11, preferring Wayland if both are available
cmake "$GLFW_SRC_DIR" \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=OFF \
    -DGLFW_BUILD_DOCS=OFF \
    -DGLFW_BUILD_TESTS=OFF \
    -DGLFW_BUILD_EXAMPLES=OFF \
    -DGLFW_USE_WAYLAND=ON \
    -G "$CMAKE_GENERATOR" 2>&1 | tee "$PLATFORM_BUILD_DIR/cmake_config.log"

if [ ${PIPESTATUS[0]} -ne 0 ]; then
    echo ""
    echo "✗ CMake configuration failed"
    echo "This usually means missing dependencies. Required packages:"
    echo "  sudo apt-get install libwayland-dev libxkbcommon-dev extra-cmake-modules pkg-config wayland-protocols libxinerama-dev libxrandr-dev libxi-dev libxcursor-dev libx11-dev"
    echo ""
    echo "See cmake_config.log for details: $PLATFORM_BUILD_DIR/cmake_config.log"
    exit 1
fi

cmake --build . --config Release

echo "✓ GLFW static library built"

# Find the built GLFW library
GLFW_LIB=""
if [ -f "$GLFW_BUILD_DIR/src/libglfw3.a" ]; then
    GLFW_LIB="$GLFW_BUILD_DIR/src/libglfw3.a"
elif [ -f "$GLFW_BUILD_DIR/src/Release/libglfw3.a" ]; then
    GLFW_LIB="$GLFW_BUILD_DIR/src/Release/libglfw3.a"
else
    echo "Error: GLFW static library not found"
    exit 1
fi

echo "Found GLFW library: $GLFW_LIB"

# No JNI wrapper needed - we're using FFM API, not JNI

# Link everything into shared library
echo ""
echo "Linking shared library..."

cd "$PLATFORM_BUILD_DIR"

case "$OS" in
    Linux*)
        # Link GLFW statically and export all symbols so jextract bindings can find them
        # GLFW supports both Wayland (preferred) and X11 (fallback)
        # Try to link Xxf86vm - link directly to .so.1 if symlink doesn't exist
        XF86VM_LIB=""
        if [ -f "/usr/lib/x86_64-linux-gnu/libXxf86vm.so" ]; then
            XF86VM_LIB="-lXxf86vm"
        elif [ -f "/usr/lib/x86_64-linux-gnu/libXxf86vm.so.1" ]; then
            # Link directly to the versioned library
            XF86VM_LIB="/usr/lib/x86_64-linux-gnu/libXxf86vm.so.1"
        fi
        
        # Link Wayland libraries first (preferred), then X11 libraries (fallback)
        # GLFW will auto-detect which backend to use at runtime
        gcc -shared \
            -o "$SHARED_LIB" \
            -Wl,--whole-archive "$GLFW_LIB" -Wl,--no-whole-archive \
            -lwayland-client -lwayland-cursor -lxkbcommon \
            -lX11 -lXrandr -lXi -lXcursor -lXinerama -lpthread -ldl -lm \
            $XF86VM_LIB \
            -Wl,-rpath,'$ORIGIN' \
            -Wl,--export-dynamic \
            -O2
        ;;
    Darwin*)
        # Link GLFW statically and export symbols
        gcc -shared \
            -o "$SHARED_LIB" \
            -Wl,-force_load,"$GLFW_LIB" \
            -framework Cocoa -framework IOKit -framework CoreVideo \
            -O2
        ;;
    MINGW*|MSYS*|CYGWIN*)
        # Link GLFW statically and export all symbols
        gcc -shared \
            -o "$SHARED_LIB" \
            "$GLFW_LIB" \
            -lgdi32 -luser32 -lkernel32 \
            -Wl,--export-all-symbols \
            -O2
        ;;
esac

if [ -f "$PLATFORM_BUILD_DIR/$SHARED_LIB" ]; then
    echo "✓ Shared library created: $PLATFORM_BUILD_DIR/$SHARED_LIB"
    ls -lh "$PLATFORM_BUILD_DIR/$SHARED_LIB"
else
    echo "✗ Failed to create shared library"
    exit 1
fi

# Copy library directly to resources directory
RESOURCES_DIR="$PROJECT_DIR/src/main/resources/natives/$PLATFORM"
mkdir -p "$RESOURCES_DIR"
cp "$PLATFORM_BUILD_DIR/$SHARED_LIB" "$RESOURCES_DIR/"
echo "✓ Library copied to: $RESOURCES_DIR/$SHARED_LIB"

echo ""
echo "Build complete!"
echo "Library location: $RESOURCES_DIR/$SHARED_LIB"
