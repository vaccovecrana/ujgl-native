package io.vacco.ujgl.gfx.context;

import io.vacco.ujgl.gfx.glfw.GLFWframebuffersizefun;
import io.vacco.ujgl.gfx.glfw.GLFWwindowclosefun;
import io.vacco.ujgl.gfx.glfw.GLFWwindowsizefun;
import io.vacco.ujgl.gfx.glfw.glfw3_h;
import io.vacco.ujgl.gfx.nativelib.UlNative;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * GLFW context manager.
 * Handles GLFW initialization, optional window creation, event callbacks, and cleanup.
 * Ensures proper resource management for both headless and windowed modes.
 */
public class UlGfContext {

  static {
    UlNative.load();
  }

  /**
   * Resizes a GLFW window, triggering window and framebuffer resize callbacks.
   * Use this to test window-driven swapchain recreation.
   *
   * @param window The GLFW window handle
   * @param width  New window width
   * @param height New window height
   */
  public static void resizeWindow(MemorySegment window, int width, int height) {
    glfw3_h.glfwSetWindowSize(window, width, height);
  }

  /**
   * Runs a task in a headless GLFW context (no window).
   *
   * @param task The task to run
   */
  public static void run(Runnable task) {
    if (glfw3_h.glfwInit() == 0) {
      throw new RuntimeException("Failed to initialize GLFW");
    }
    try {
      task.run();
    } finally {
      glfw3_h.glfwTerminate();
    }
  }

  /**
   * Runs a GLFW windowed application with full lifecycle callbacks.
   * Window is created visible (lightweightvk approach) to allow compositor to apply DPI scaling.
   * Manages the main loop, polling events and calling onRender until exit.
   * <p />
   * Handles HiDPI displays by creating visible window immediately and querying framebuffer size
   * after compositor has applied scaling (following lightweightvk pattern).
   *
   * @param width  Window width (may be scaled by HiDPI)
   * @param height Window height (may be scaled by HiDPI)
   * @param title  Window title
   * @param client Client implementing lifecycle callbacks
   */
  public static void runWithWindow(int width, int height, String title, UlGfClient client) {
    if (glfw3_h.glfwInit() == 0) {
      throw new RuntimeException("Failed to initialize GLFW");
    }
    
    try (var arena = Arena.ofConfined()) {
      // Configure window for Vulkan (no OpenGL context)
      glfw3_h.glfwWindowHint(glfw3_h.GLFW_CLIENT_API(), glfw3_h.GLFW_NO_API());
      glfw3_h.glfwWindowHint(glfw3_h.GLFW_SCALE_TO_MONITOR(), glfw3_h.GLFW_FALSE());

      var titleSeg = arena.allocateFrom(title, StandardCharsets.UTF_8);
      var window = glfw3_h.glfwCreateWindow(width, height, titleSeg, MemorySegment.NULL, MemorySegment.NULL);
      var closed = new boolean[1];
      
      if (window == null || window.equals(MemorySegment.NULL)) {
        throw new RuntimeException("Failed to create GLFW window");
      }

      try {
        // Setup callbacks BEFORE querying size (lightweightvk order)
        var windowSizeCallback = GLFWwindowsizefun.allocate(client::onWindowResize, arena);
        glfw3_h.glfwSetWindowSizeCallback(window, windowSizeCallback);
        var framebufferSizeCallback = GLFWframebuffersizefun.allocate(client::onFramebufferResize, arena);
        glfw3_h.glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);
        var closeCallback = GLFWwindowclosefun.allocate(
          (win) -> {
            closed[0] = client.onWindowClose(win);
            if (!closed[0]) {
              glfw3_h.glfwSetWindowShouldClose(win, glfw3_h.GLFW_FALSE());
            }
          }, arena
        );
        glfw3_h.glfwSetWindowCloseCallback(window, closeCallback);

        // Query actual framebuffer size (lightweightvk approach)
        // Window is already visible, so compositor has applied scaling
        try (var sizeArena = Arena.ofConfined()) {
          var pWidth = sizeArena.allocate(ValueLayout.JAVA_INT);
          var pHeight = sizeArena.allocate(ValueLayout.JAVA_INT);
          glfw3_h.glfwGetFramebufferSize(window, pWidth, pHeight);
          int fbWidth = pWidth.get(ValueLayout.JAVA_INT, 0);
          int fbHeight = pHeight.get(ValueLayout.JAVA_INT, 0);
          client.onWindowReady(window, fbWidth, fbHeight);
        }

        while (glfw3_h.glfwWindowShouldClose(window) == 0 && !closed[0]) {
          glfw3_h.glfwPollEvents();
          if (!client.onRender(window)) {
            break;
          }
        }
      } finally {
        clearCallbacks(window);
        glfw3_h.glfwDestroyWindow(window);
      }
    } finally {
      glfw3_h.glfwTerminate();
    }
  }

  private static void clearCallbacks(MemorySegment window) {
    glfw3_h.glfwSetWindowSizeCallback(window, MemorySegment.NULL);
    glfw3_h.glfwSetFramebufferSizeCallback(window, MemorySegment.NULL);
    glfw3_h.glfwSetWindowCloseCallback(window, MemorySegment.NULL);
  }

}
