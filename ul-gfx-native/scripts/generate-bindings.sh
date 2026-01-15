#!/bin/bash
set -e

# Script to generate Panama FFI bindings using jextract for RGFW
# Usage: ./scripts/generate-bindings.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_BASE="$PROJECT_DIR/src/main/java"
NATIVE_DIR="$PROJECT_DIR/native"

echo "Generating Panama FFI bindings with jextract..."

# Check if jextract is available
if ! command -v jextract &> /dev/null; then
    echo "Error: jextract not found in PATH"
    echo "Please ensure jextract is installed and available in your PATH"
    exit 1
fi

# Detect platform
OS="$(uname -s)"
ARCH="$(uname -m)"

echo "Platform: $OS $ARCH"

# Create output base directory (jextract will create the package structure)
mkdir -p "$OUTPUT_BASE"

# Find RGFW headers
RGFW_HEADER=""
RGFW_INCLUDE=""
if [ -f "$NATIVE_DIR/RGFW/RGFW.h" ]; then
    RGFW_HEADER="$NATIVE_DIR/RGFW/RGFW.h"
    RGFW_INCLUDE="$NATIVE_DIR/RGFW"
    echo "✓ Found RGFW header: $RGFW_HEADER"
else
    echo "✗ RGFW headers not found at $NATIVE_DIR/RGFW/RGFW.h"
    echo "Please run build-native.sh first to download RGFW"
    exit 1
fi

# Find Vulkan headers
VULKAN_INCLUDE=""
if [ -d "/usr/include/vulkan" ]; then
    VULKAN_INCLUDE="/usr/include/vulkan"
elif [ -d "/usr/local/include/vulkan" ]; then
    VULKAN_INCLUDE="/usr/local/include/vulkan"
else
    echo "Error: Vulkan headers not found"
    echo "Please install libvulkan-dev package"
    exit 1
fi

# Find Vulkan headers
VULKAN_INCLUDE=""
if [ -d "/usr/include/vulkan" ]; then
    VULKAN_INCLUDE="/usr/include"
    echo "✓ Found Vulkan headers: /usr/include/vulkan"
elif [ -d "/usr/local/include/vulkan" ]; then
    VULKAN_INCLUDE="/usr/local/include"
    echo "✓ Found Vulkan headers: /usr/local/include/vulkan"
else
    echo "✗ Vulkan headers not found"
    echo "Please install libvulkan-dev package"
    exit 1
fi

# Generate RGFW bindings
# Note: RGFW is a header-only library with inline functions
# jextract will generate types and callback definitions, but we need manual wrappers for functions
echo ""
echo "Generating RGFW bindings..."
jextract \
    --output "$OUTPUT_BASE" \
    -I "$RGFW_INCLUDE" \
    -I "$VULKAN_INCLUDE" \
    -t io.vacco.ujgl.gfx.rgfw \
    --header-class-name rgfw_h \
    -D RGFW_VULKAN \
    -D RGFW_WAYLAND \
    "$RGFW_HEADER"

if [ $? -eq 0 ]; then
    echo "✓ RGFW bindings generated successfully"
    echo "  Note: Function wrappers are in UlRgfwFunctions.java (manually maintained)"
else
    echo "✗ Failed to generate RGFW bindings"
    exit 1
fi

# Generate Vulkan bindings
echo ""
echo "Generating Vulkan bindings..."
jextract \
    --output "$OUTPUT_BASE" \
    -I "$VULKAN_INCLUDE" \
    -t io.vacco.ujgl.gfx.vulkan \
    --header-class-name vulkan_h \
    "$VULKAN_INCLUDE/vulkan/vulkan.h"

if [ $? -eq 0 ]; then
    echo "✓ Vulkan bindings generated successfully"
else
    echo "✗ Failed to generate Vulkan bindings"
    exit 1
fi

echo ""
echo "All bindings generated successfully!"
echo "Output directory: $OUTPUT_BASE"
echo ""
echo "Summary:"
echo "  - RGFW types: io.vacco.ujgl.gfx.rgfw.*"
echo "  - RGFW functions: UlRgfwFunctions.java (manual wrappers)"
echo "  - Vulkan types: io.vacco.ujgl.gfx.vulkan.*"