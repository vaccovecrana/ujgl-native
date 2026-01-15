package io.vacco.ujgl.gfx.context;

import java.lang.foreign.MemorySegment;

/**
 * Client interface for handling window lifecycle events (GLFW/RGFW).
 * Implementations receive callbacks for window creation, resize, rendering, and close events.
 */
public interface UlRgfwClient {

  /**
   * Called once the window is created and ready for rendering.
   * Initialize graphics resources here (e.g., Vulkan surface, swapchain).
   *
   * @param window Window handle
   * @param width  Window width in pixels
   * @param height Window height in pixels
   */
  void onWindowReady(MemorySegment window, int width, int height);

  /**
   * Called when the window size changes.
   * Recreate Vulkan swapchains here.
   *
   * @param window Window handle
   * @param width  New window width in pixels
   * @param height New window height in pixels
   */
  void onWindowResize(MemorySegment window, int width, int height);

  /**
   * Called when the content scale (DPI) changes.
   * Update UI scaling factors if needed.
   *
   * @param window Window handle
   * @param scaleX Content scale factor X
   * @param scaleY Content scale factor Y
   */
  default void onScaleUpdated(MemorySegment window, float scaleX, float scaleY) {
    // Optional - not all implementations need this
  }

  /**
   * Called when the window gains or loses focus.
   *
   * @param window  Window handle
   * @param inFocus true if window gained focus, false if lost focus
   */
  default void onFocusChanged(MemorySegment window, boolean inFocus) {
    // Optional - not all implementations need this
  }

  /**
   * Called per frame for updates and rendering.
   * Perform rendering, process input, etc.
   *
   * @param window Window handle
   * @return true to continue; false to exit the main loop
   */
  boolean onRender(MemorySegment window);

  /**
   * Called on close request.
   * Clean up resources.
   *
   * @param window Window handle
   * @return true to allow close; false to veto
   */
  boolean onWindowClose(MemorySegment window);

}
