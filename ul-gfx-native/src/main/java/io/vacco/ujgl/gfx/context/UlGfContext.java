package io.vacco.ujgl.gfx.context;

import io.vacco.ujgl.gfx.glfw.GLFWframebuffersizefun;
import io.vacco.ujgl.gfx.glfw.GLFWwindowclosefun;
import io.vacco.ujgl.gfx.glfw.GLFWwindowsizefun;
import io.vacco.ujgl.gfx.glfw.glfw3_h;
import io.vacco.ujgl.gfx.nativelib.UlNative;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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
   * The window is initially hidden and shown after setup.
   * Manages the main loop, polling events and calling onRender until exit.
   *
   * @param width  Window width
   * @param height Window height
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
      glfw3_h.glfwWindowHint(glfw3_h.GLFW_VISIBLE(), glfw3_h.GLFW_FALSE());
      glfw3_h.glfwWindowHint(glfw3_h.GLFW_SCALE_TO_MONITOR(), glfw3_h.GLFW_FALSE());

      var titleSeg = arena.allocateFrom(title, StandardCharsets.UTF_8);
      var window = glfw3_h.glfwCreateWindow(width, height, titleSeg, MemorySegment.NULL, MemorySegment.NULL);
      
      if (window == null || window.equals(MemorySegment.NULL)) {
        throw new RuntimeException("Failed to create GLFW window");
      }

      try {
        setupCallbacks(window, client, arena);
        
        glfw3_h.glfwShowWindow(window);
        client.onWindowReady(window);
        
        mainLoop(window, client);
        
      } finally {
        clearCallbacks(window);
        glfw3_h.glfwDestroyWindow(window);
      }
    } finally {
      glfw3_h.glfwTerminate();
    }
  }

  private static void setupCallbacks(MemorySegment window, UlGfClient client, Arena arena) {
    var windowSizeCallback = GLFWwindowsizefun.allocate(
      (win, w, h) -> client.onWindowResize(win, w, h), arena
    );
    glfw3_h.glfwSetWindowSizeCallback(window, windowSizeCallback);

    var framebufferSizeCallback = GLFWframebuffersizefun.allocate(
      (win, w, h) -> client.onFramebufferResize(win, w, h), arena
    );
    glfw3_h.glfwSetFramebufferSizeCallback(window, framebufferSizeCallback);

    var closeCallback = GLFWwindowclosefun.allocate(
      (win) -> {
        if (!client.onWindowClose(win)) {
          glfw3_h.glfwSetWindowShouldClose(win, glfw3_h.GLFW_FALSE());
        }
      }, arena
    );
    glfw3_h.glfwSetWindowCloseCallback(window, closeCallback);
  }

  private static void clearCallbacks(MemorySegment window) {
    glfw3_h.glfwSetWindowSizeCallback(window, MemorySegment.NULL);
    glfw3_h.glfwSetFramebufferSizeCallback(window, MemorySegment.NULL);
    glfw3_h.glfwSetWindowCloseCallback(window, MemorySegment.NULL);
  }

  private static void mainLoop(MemorySegment window, UlGfClient client) {
    while (glfw3_h.glfwWindowShouldClose(window) == 0) {
      glfw3_h.glfwPollEvents();
      if (!client.onRender(window)) {
        break;
      }
    }
  }

}
