#!/bin/bash
set -e

# Script to build RGFW as shared library
# The library is named libRGFW.so/dylib/dll so jextract bindings can find it
# Usage: ./scripts/build-native.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
NATIVE_DIR="$PROJECT_DIR/native"
BUILD_DIR="$NATIVE_DIR/build"

# Check for required dependencies on Linux
if [[ "$(uname -s)" == "Linux"* ]]; then
	echo "Checking for required development packages..."
	MISSING_DEPS=()

	# Wayland packages (required for RGFW)
	for pkg in libwayland-dev libxkbcommon-dev wayland-protocols libwayland-cursor0 libwayland-egl1; do
		if ! dpkg -l "$pkg" 2>/dev/null | grep -q "^ii"; then
			MISSING_DEPS+=("$pkg")
		fi
	done

	# libdecor for Client-Side Decorations (CSD) support
	for pkg in libdecor-0-dev libdecor-0-plugin-1-gtk; do
		if ! dpkg -l "$pkg" 2>/dev/null | grep -q "^ii"; then
			MISSING_DEPS+=("$pkg")
		fi
	done

	# X11 packages (fallback for RGFW)
	for pkg in libxrandr-dev libx11-dev; do
		if ! dpkg -l "$pkg" 2>/dev/null | grep -q "^ii"; then
			MISSING_DEPS+=("$pkg")
		fi
	done

	# Build tools
	for pkg in git wayland-scanner; do
		if ! command -v "$pkg" &>/dev/null; then
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

# RGFW from GitHub fork with Wayland fixes
RGFW_VERSION="1.9.0-dev-wayland-fixes"
RGFW_URL="https://raw.githubusercontent.com/jjzazuet/RGFW/refs/heads/main/RGFW.h"

# Detect platform
OS="$(uname -s)"
ARCH="$(uname -m)"

echo "Building native libraries for platform: $OS $ARCH"

# Determine platform-specific settings
case "$OS" in
Linux*)
	PLATFORM="linux-x64"
	SHARED_LIB="libRGFW.so"
	;;
Darwin*)
	PLATFORM="macos-x64"
	SHARED_LIB="libRGFW.dylib"
	;;
MINGW* | MSYS* | CYGWIN*)
	PLATFORM="windows-x64"
	SHARED_LIB="RGFW.dll"
	;;
*)
	echo "Unsupported platform: $OS"
	exit 1
	;;
esac

PLATFORM_BUILD_DIR="$BUILD_DIR/$PLATFORM"
mkdir -p "$PLATFORM_BUILD_DIR"

echo "Platform build directory: $PLATFORM_BUILD_DIR"

# Download RGFW.h from GitHub fork (always re-download for latest fixes)
RGFW_SRC_DIR="$NATIVE_DIR/RGFW"
mkdir -p "$RGFW_SRC_DIR"

echo ""
echo "Downloading RGFW $RGFW_VERSION from GitHub fork..."
curl -L "$RGFW_URL" -o "$RGFW_SRC_DIR/RGFW.h"
if [ $? -eq 0 ]; then
	echo "✓ RGFW.h downloaded to $RGFW_SRC_DIR"
else
	echo "✗ Failed to download RGFW.h"
	exit 1
fi

# Generate Wayland protocol headers (required for RGFW on Linux)
if [[ "$OS" == "Linux"* ]]; then
	echo ""
	echo "Generating Wayland protocol headers..."

	WAYLAND_PROTOCOLS_DIR="/usr/share/wayland-protocols"

	generate_protocol() {
		local protocol_name="$1"
		local protocol_file="$2"

		if [ ! -f "$RGFW_SRC_DIR/${protocol_name}.h" ]; then
			echo "Generating ${protocol_name}.h..."
			wayland-scanner client-header "$protocol_file" "$RGFW_SRC_DIR/${protocol_name}.h"
			wayland-scanner private-code "$protocol_file" "$RGFW_SRC_DIR/${protocol_name}.c"
		fi
	}

	generate_protocol "xdg-shell" "$WAYLAND_PROTOCOLS_DIR/stable/xdg-shell/xdg-shell.xml"
	generate_protocol "xdg-decoration-unstable-v1" "$WAYLAND_PROTOCOLS_DIR/unstable/xdg-decoration/xdg-decoration-unstable-v1.xml"
	generate_protocol "relative-pointer-unstable-v1" "$WAYLAND_PROTOCOLS_DIR/unstable/relative-pointer/relative-pointer-unstable-v1.xml"
	generate_protocol "pointer-constraints-unstable-v1" "$WAYLAND_PROTOCOLS_DIR/unstable/pointer-constraints/pointer-constraints-unstable-v1.xml"
	generate_protocol "xdg-output-unstable-v1" "$WAYLAND_PROTOCOLS_DIR/unstable/xdg-output/xdg-output-unstable-v1.xml"
	generate_protocol "viewporter" "$WAYLAND_PROTOCOLS_DIR/stable/viewporter/viewporter.xml"
	generate_protocol "fractional-scale-v1" "$WAYLAND_PROTOCOLS_DIR/staging/fractional-scale/fractional-scale-v1.xml"
	generate_protocol "xdg-toplevel-icon-v1" "$WAYLAND_PROTOCOLS_DIR/staging/xdg-toplevel-icon/xdg-toplevel-icon-v1.xml"
	generate_protocol "xdg-toplevel-icon-v1" "$WAYLAND_PROTOCOLS_DIR/staging/xdg-toplevel-icon/xdg-toplevel-icon-v1.xml"
	generate_protocol "viewporter" "$WAYLAND_PROTOCOLS_DIR/stable/viewporter/viewporter.xml"
	generate_protocol "fractional-scale-v1" "$WAYLAND_PROTOCOLS_DIR/staging/fractional-scale/fractional-scale-v1.xml"

	echo "✓ Wayland protocol headers generated"
fi

# Build RGFW shared library
echo ""
echo "Building RGFW as shared library..."
case "$OS" in
Linux*)
	# Build RGFW with Wayland support
	# Create a wrapper C file since RGFW.h is header-only
	cat >"$RGFW_SRC_DIR/rgfw_wrapper.c" <<'EOF'
#define RGFW_IMPLEMENTATION
#define RGFW_WAYLAND
#define RGFW_LIBDECOR
#define RGFW_VULKAN
#include "RGFW.h"

/* HiDPI scale accessors for FFI bindings - now in RGFW.h, no longer needed here */
EOF

	# Collect Wayland protocol .c files (exclude wrapper)
	WAYLAND_C_FILES=""
	for cfile in "$RGFW_SRC_DIR"/*.c; do
		if [ "$(basename "$cfile")" != "rgfw_wrapper.c" ]; then
			WAYLAND_C_FILES="$WAYLAND_C_FILES $cfile"
		fi
	done

	gcc -shared -fPIC \
		-I "$RGFW_SRC_DIR" \
		-DRGFW_LIBDECOR \
		-DRGFW_VULKAN \
		-DRGFW_EXPORT \
		$(pkg-config --cflags libdecor-0 vulkan) \
		-o "$RGFW_SRC_DIR/libRGFW.so" \
		"$RGFW_SRC_DIR/rgfw_wrapper.c" \
		$WAYLAND_C_FILES \
		-lwayland-client -lwayland-cursor -lwayland-egl -lxkbcommon -lEGL -lGL \
		$(pkg-config --libs libdecor-0 vulkan) \
		2>&1 | tee "$PLATFORM_BUILD_DIR/rgfw_build.log"

	if [ ! -f "$RGFW_SRC_DIR/libRGFW.so" ]; then
		echo "✗ Failed to build libRGFW.so"
		echo "See build log: $PLATFORM_BUILD_DIR/rgfw_build.log"
		exit 1
	fi

	cp "$RGFW_SRC_DIR/libRGFW.so" "$PLATFORM_BUILD_DIR/$SHARED_LIB"
	;;
Darwin*)
	# Build RGFW for macOS
	cat >"$RGFW_SRC_DIR/rgfw_wrapper.c" <<'EOF'
#define RGFW_IMPLEMENTATION
#include "RGFW.h"
EOF

	gcc -shared -fPIC \
		-I "$RGFW_SRC_DIR" \
		-o "$RGFW_SRC_DIR/libRGFW.dylib" \
		"$RGFW_SRC_DIR/rgfw_wrapper.c" \
		-framework Cocoa -framework CoreVideo -framework OpenGL -framework IOKit \
		2>&1 | tee "$PLATFORM_BUILD_DIR/rgfw_build.log"

	if [ ! -f "$RGFW_SRC_DIR/libRGFW.dylib" ]; then
		echo "✗ Failed to build libRGFW.dylib"
		echo "See build log: $PLATFORM_BUILD_DIR/rgfw_build.log"
		exit 1
	fi

	cp "$RGFW_SRC_DIR/libRGFW.dylib" "$PLATFORM_BUILD_DIR/$SHARED_LIB"
	;;
MINGW* | MSYS* | CYGWIN*)
	# Build RGFW for Windows
	cat >"$RGFW_SRC_DIR/rgfw_wrapper.c" <<'EOF'
#define RGFW_IMPLEMENTATION
#include "RGFW.h"
EOF

	gcc -shared \
		-I "$RGFW_SRC_DIR" \
		-o "$RGFW_SRC_DIR/RGFW.dll" \
		"$RGFW_SRC_DIR/rgfw_wrapper.c" \
		-lopengl32 -lgdi32 -lshell32 -lwinmm -ldwmapi \
		2>&1 | tee "$PLATFORM_BUILD_DIR/rgfw_build.log"

	if [ ! -f "$RGFW_SRC_DIR/RGFW.dll" ]; then
		echo "✗ Failed to build RGFW.dll"
		echo "See build log: $PLATFORM_BUILD_DIR/rgfw_build.log"
		exit 1
	fi

	cp "$RGFW_SRC_DIR/RGFW.dll" "$PLATFORM_BUILD_DIR/$SHARED_LIB"
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
echo ""
echo "Note: To use the library in tests, add to gradle.properties:"
echo "  org.gradle.jvmargs=-Djava.library.path=$RESOURCES_DIR"
