package io.vacco.ujgl.gfx.context;

import java.lang.foreign.MemorySegment;

/**
 * Client interface for handling GLFW window lifecycle events.
 * Implementations receive callbacks for window creation, resize, rendering, and close events.
 */
public interface UlGfClient {

  /**
   * Called once the window is created, shown, and ready for rendering.
   * Initialize graphics resources here (e.g., Vulkan surface, swapchain).
   * Query initial sizes if needed.
   *
   * @param window GLFW window handle
   */
  void onWindowReady(MemorySegment window);

  /**
   * Called when the window size changes (screen coordinates).
   * Update logical aspects if necessary.
   *
   * @param window GLFW window handle
   * @param width  New window width
   * @param height New window height
   */
  void onWindowResize(MemorySegment window, int width, int height);

  /**
   * Called when the framebuffer size changes (pixel resolution, e.g., DPI).
   * Recreate Vulkan swapchains here.
   *
   * @param window GLFW window handle
   * @param width  New framebuffer width in pixels
   * @param height New framebuffer height in pixels
   */
  void onFramebufferResize(MemorySegment window, int width, int height);

  /**
   * Called per frame for updates and rendering.
   * Perform rendering, process input, etc.
   *
   * @param window GLFW window handle
   * @return true to continue; false to exit the main loop
   */
  boolean onRender(MemorySegment window);

  /**
   * Called on close request.
   * Clean up resources.
   *
   * @param window GLFW window handle
   * @return true to allow close; false to veto
   */
  boolean onWindowClose(MemorySegment window);

}
