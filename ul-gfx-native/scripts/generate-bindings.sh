#!/bin/bash
set -e

# Script to generate JNI bindings using jextract
# Usage: ./scripts/generate-bindings.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
OUTPUT_BASE="$PROJECT_DIR/src/main/java"

echo "Generating JNI bindings with jextract..."

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

# Find GLFW headers
# First try downloaded GLFW source, then system headers
GLFW_HEADER=""
if [ -d "$PROJECT_DIR/native/glfw/include/GLFW" ]; then
    GLFW_HEADER="$PROJECT_DIR/native/glfw/include/GLFW/glfw3.h"
    GLFW_INCLUDE="$PROJECT_DIR/native/glfw/include"
elif [ -f "/usr/include/GLFW/glfw3.h" ]; then
    GLFW_HEADER="/usr/include/GLFW/glfw3.h"
    GLFW_INCLUDE="/usr/include"
else
    echo "Error: GLFW headers not found"
    echo "Please ensure GLFW is installed or download GLFW source to native/glfw/"
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

echo "GLFW header: $GLFW_HEADER"
echo "Vulkan include: $VULKAN_INCLUDE"

# Generate GLFW bindings
# Note: We don't use -l :glfw because we load the library ourselves via NativeLibraryLoader
# The bindings will use loaderLookup() and defaultLookup() to find symbols in our loaded library
echo ""
echo "Generating GLFW bindings..."
jextract \
    --output "$OUTPUT_BASE" \
    -I "$GLFW_INCLUDE" \
    -t io.vacco.ujgl.gfx.glfw \
    -D GLFW_INCLUDE_VULKAN \
    "$GLFW_HEADER"

if [ $? -eq 0 ]; then
    echo "✓ GLFW bindings generated successfully"
else
    echo "✗ Failed to generate GLFW bindings"
    exit 1
fi

# Generate Vulkan bindings
# Note: We don't use -l :vulkan because Vulkan is loaded by the system loader at runtime
# The bindings will use loaderLookup() and defaultLookup() to find symbols
echo ""
echo "Generating Vulkan bindings..."
jextract \
    --output "$OUTPUT_BASE" \
    -I "$VULKAN_INCLUDE" \
    -t io.vacco.ujgl.gfx.vulkan \
    --header-class-name vulkan_h \
    "$VULKAN_INCLUDE/vulkan.h" "$VULKAN_INCLUDE/vulkan_core.h"

if [ $? -eq 0 ]; then
    echo "✓ Vulkan bindings generated successfully"
else
    echo "✗ Failed to generate Vulkan bindings"
    exit 1
fi

echo ""
echo "All bindings generated successfully!"
echo "Output directory: $OUTPUT_BASE"
