package io.vacco.ujgl.gfx.context;

import io.vacco.ujgl.gfx.rgfw.RGFW_focusfunc;
import io.vacco.ujgl.gfx.rgfw.RGFW_scaleUpdatedfunc;
import io.vacco.ujgl.gfx.rgfw.RGFW_windowQuitfunc;
import io.vacco.ujgl.gfx.rgfw.RGFW_windowResizedfunc;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

import static io.vacco.ujgl.gfx.context.UlRgfwFunctions.*;

/**
 * RGFW context manager.
 * Handles window creation, event callbacks, and main loop with native HiDPI support.
 */
public class UlRgfwContext {

  // RGFW window flags (from RGFW.h)
  private static final long RGFW_windowCenter = 1L << 6;
  private static final long RGFW_windowFocus = 1L << 15;  // Request focus on creation

  /**
   * Runs an RGFW windowed application with full lifecycle callbacks.
   * Automatically enables Wayland with native HiDPI support.
   * Creates a window with decorations (title bar, close button, etc.) and requests focus.
   *
   * @param width  Window width
   * @param height Window height
   * @param title  Window title
   * @param client Client implementing lifecycle callbacks
   */
  public static void runWithWindow(int width, int height, String title, UlRgfwClient client) {
    // Enable Wayland for native HiDPI support
    useWayland(true);

    try (var arena = Arena.ofConfined()) {
      // Setup callbacks BEFORE creating window
      // (RGFW may fire scale callback during window creation)
      var resizeCallback = RGFW_windowResizedfunc.allocate(
        client::onWindowResize,
        arena
      );
      setWindowResizedCallback(resizeCallback);

      var scaleCallback = RGFW_scaleUpdatedfunc.allocate(
        client::onScaleUpdated,
        arena
      );
      setScaleUpdatedCallback(scaleCallback);

      var quitCallback = RGFW_windowQuitfunc.allocate(
        client::onWindowClose,
        arena
      );
      setWindowQuitCallback(quitCallback);

      var focusCallback = RGFW_focusfunc.allocate(
        (win, inFocus) -> client.onFocusChanged(win, inFocus != 0),
        arena
      );
      setFocusCallback(focusCallback);

      // Now create window - callbacks are ready to receive events
      // Combine flags: RGFW_windowCenter | RGFW_windowFocus
      var window = createWindow(
        title, 0, 0, width, height,
        RGFW_windowCenter | RGFW_windowFocus, arena
      );

      if (window == null || window.equals(MemorySegment.NULL)) {
        throw new RuntimeException("Failed to create RGFW window");
      }

      try {
        // Get window dimensions and notify client
        int[] w = new int[1];
        int[] h = new int[1];
        getWindowSize(window, w, h, arena);
        client.onWindowReady(window, w[0], h[0]);

        // Main loop
        while (!windowShouldClose(window)) {
          pollEvents();
          if (!client.onRender(window)) {
            break;
          }
        }
      } finally {
        windowClose(window);
      }
    }
  }

}
