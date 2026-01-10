# UlGfContext and UlGfClient Usage Guide

## Overview

The `UlGfContext` class provides a GLFW context manager for Vulkan-based applications with full lifecycle event handling through the `UlGfClient` interface. It uses Java 25's Foreign Function & Memory (FFM) API with jextract-generated bindings.

## Quick Start

### Headless Mode

For applications that don't need a window:

```java
UlGfContext.run(() -> {
    // Your headless code here
    // GLFW is initialized and will be terminated automatically
});
```

### Windowed Mode with Callbacks

For applications with a window and event handling:

```java
var client = new UlGfClient() {
    @Override
    public void onWindowReady(MemorySegment window) {
        // Initialize graphics resources
        // Window is shown and ready for rendering
    }

    @Override
    public void onWindowResize(MemorySegment window, int width, int height) {
        // Handle window size changes (screen coordinates)
    }

    @Override
    public void onFramebufferResize(MemorySegment window, int width, int height) {
        // Handle framebuffer size changes (pixel resolution/DPI)
        // Recreate swapchains here
    }

    @Override
    public boolean onRender(MemorySegment window) {
        // Perform rendering per frame
        // Return true to continue, false to exit
        return true;
    }

    @Override
    public boolean onWindowClose(MemorySegment window) {
        // Handle close request
        // Return true to allow, false to veto
        return true;
    }
};

UlGfContext.runWithWindow(800, 600, "My Application", client);
```

## Lifecycle Flow

1. **Initialization**: GLFW is initialized, window created (hidden)
2. **Setup**: Callbacks registered via FFM upcall stubs
3. **Show**: Window shown
4. **Ready**: `onWindowReady()` called
5. **Main Loop**:
   - `glfwPollEvents()` - processes OS events, triggers callbacks
   - `onRender()` - called each frame
   - Loop continues until `onRender()` returns false or window should close
6. **Cleanup**: Callbacks cleared, window destroyed, GLFW terminated

## Event Callbacks

### onWindowReady(window)
Called once after window is shown. Initialize your graphics resources here.

### onWindowResize(window, width, height)
Called when window size changes (screen coordinates). May not fire on all platforms/window managers when window is not visible.

### onFramebufferResize(window, width, height)
Called when framebuffer (pixel) size changes. Critical for handling DPI scaling. Recreate Vulkan swapchains here.

### onRender(window)
Called every frame in the main loop. Perform all rendering here.
- Return `true` to continue the loop
- Return `false` to exit gracefully

### onWindowClose(window)
Called when user requests to close the window (e.g., clicking X button).
- Return `true` to allow closing
- Return `false` to veto/cancel the close request

## Technical Details

### FFM Upcall Stubs
Callbacks use jextract-generated upcall allocators:
- `GLFWwindowsizefun.allocate(fn, arena)`
- `GLFWframebuffersizefun.allocate(fn, arena)`
- `GLFWwindowclosefun.allocate(fn, arena)`

### Arena Management
A confined arena manages all callback stubs and the title string. It's automatically closed when the window is destroyed.

### Thread Safety
All callbacks execute on the main thread during `glfwPollEvents()`. No synchronization needed between callbacks and rendering.

### Window Configuration
Windows are created with:
- `GLFW_CLIENT_API = GLFW_NO_API` (Vulkan, no OpenGL)
- `GLFW_VISIBLE = GLFW_FALSE` initially (shown after setup)
- `GLFW_SCALE_TO_MONITOR = GLFW_FALSE` (manual DPI handling)

## Example: Simple Animation Loop

```java
var client = new UlGfClient() {
    private int frameCount = 0;

    @Override
    public void onWindowReady(MemorySegment window) {
        System.out.println("Window ready!");
    }

    @Override
    public void onWindowResize(MemorySegment window, int width, int height) {
        System.out.println("Window resized: " + width + "x" + height);
    }

    @Override
    public void onFramebufferResize(MemorySegment window, int width, int height) {
        System.out.println("Framebuffer resized: " + width + "x" + height);
    }

    @Override
    public boolean onRender(MemorySegment window) {
        frameCount++;
        if (frameCount % 60 == 0) {
            System.out.println("Frame " + frameCount);
        }
        return frameCount < 300; // Run for 300 frames
    }

    @Override
    public boolean onWindowClose(MemorySegment window) {
        System.out.println("Close requested");
        return true;
    }
};

UlGfContext.runWithWindow(1024, 768, "Animation Demo", client);
```

## Testing

The implementation includes comprehensive J8Spec tests covering:
- Headless mode execution
- Window creation and lifecycle
- Callback invocations
- Loop exit conditions
- Cleanup verification

Run tests with:
```bash
gradle test --tests UlGfClientTest
```
