package io.vacco.ujgl.gfx.context;

import io.vacco.ujgl.gfx.nativelib.UlNative;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

/**
 * Manual Panama FFI wrappers for RGFW functions.
 * Since RGFW is header-only with inline functions, we load them from libRGFW.so
 */
public class UlRgfwFunctions {

  private static final Linker LINKER = Linker.nativeLinker();
  private static final SymbolLookup LOOKUP;

  static {
    UlNative.load();
    LOOKUP = SymbolLookup.loaderLookup();
  }

  // Function handles
  private static final MethodHandle RGFW_createWindow;
  private static final MethodHandle RGFW_window_close;
  private static final MethodHandle RGFW_window_shouldClose;
  private static final MethodHandle RGFW_pollEvents;
  private static final MethodHandle RGFW_window_checkEvent;
  private static final MethodHandle RGFW_setWindowResizedCallback;
  private static final MethodHandle RGFW_setScaleUpdatedCallback;
  private static final MethodHandle RGFW_setWindowQuitCallback;
  private static final MethodHandle RGFW_setFocusCallback;
  private static final MethodHandle RGFW_useWayland;
  private static final MethodHandle RGFW_window_getSize;
  private static final MethodHandle RGFW_window_resize;
  private static final MethodHandle RGFW_window_createSurface_Vulkan;
  private static final MethodHandle RGFW_window_getScaleX;
  private static final MethodHandle RGFW_window_getScaleY;
  private static final MethodHandle RGFW_window_setViewportDestination;

  static {
    try {
      // RGFW_window* RGFW_createWindow(const char* name, i32 x, i32 y, u32 w, u32 h, u64 flags)
      RGFW_createWindow = LINKER.downcallHandle(
        LOOKUP.find("RGFW_createWindow").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG)
      );

      // void RGFW_window_close(RGFW_window* win)
      RGFW_window_close = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_close").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS)
      );

      // RGFW_bool RGFW_window_shouldClose(RGFW_window* win)
      RGFW_window_shouldClose = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_shouldClose").orElseThrow(),
        FunctionDescriptor.of(JAVA_BYTE, ADDRESS)
      );

      // void RGFW_pollEvents(void)
      RGFW_pollEvents = LINKER.downcallHandle(
        LOOKUP.find("RGFW_pollEvents").orElseThrow(),
        FunctionDescriptor.ofVoid()
      );

      // RGFW_bool RGFW_window_checkEvent(RGFW_window* win, RGFW_event* event)
      RGFW_window_checkEvent = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_checkEvent").orElseThrow(),
        FunctionDescriptor.of(JAVA_BYTE, ADDRESS, ADDRESS)
      );

      // RGFW_windowResizedfunc RGFW_setWindowResizedCallback(RGFW_windowResizedfunc func)
      RGFW_setWindowResizedCallback = LINKER.downcallHandle(
        LOOKUP.find("RGFW_setWindowResizedCallback").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, ADDRESS)
      );

      // RGFW_scaleUpdatedfunc RGFW_setScaleUpdatedCallback(RGFW_scaleUpdatedfunc func)
      RGFW_setScaleUpdatedCallback = LINKER.downcallHandle(
        LOOKUP.find("RGFW_setScaleUpdatedCallback").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, ADDRESS)
      );

      // RGFW_windowQuitfunc RGFW_setWindowQuitCallback(RGFW_windowQuitfunc func)
      RGFW_setWindowQuitCallback = LINKER.downcallHandle(
        LOOKUP.find("RGFW_setWindowQuitCallback").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, ADDRESS)
      );

      // RGFW_focusfunc RGFW_setFocusCallback(RGFW_focusfunc func)
      RGFW_setFocusCallback = LINKER.downcallHandle(
        LOOKUP.find("RGFW_setFocusCallback").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, ADDRESS)
      );

      // void RGFW_useWayland(RGFW_bool wayland)
      RGFW_useWayland = LINKER.downcallHandle(
        LOOKUP.find("RGFW_useWayland").orElseThrow(),
        FunctionDescriptor.ofVoid(JAVA_BYTE)
      );

      // RGFW_bool RGFW_window_getSize(RGFW_window* win, i32* w, i32* h)
      RGFW_window_getSize = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_getSize").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
      );

      // void RGFW_window_resize(RGFW_window* win, i32 w, i32 h)
      RGFW_window_resize = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_resize").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT)
      );

      // VkResult RGFW_window_createSurface_Vulkan(RGFW_window* win, VkInstance instance, VkSurfaceKHR* surface)
      RGFW_window_createSurface_Vulkan = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_createSurface_Vulkan").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS)
      );

      // float RGFW_window_getScaleX(RGFW_window* win)
      RGFW_window_getScaleX = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_getScaleX").orElseThrow(),
        FunctionDescriptor.of(JAVA_FLOAT, ADDRESS)
      );

      // float RGFW_window_getScaleY(RGFW_window* win)
      RGFW_window_getScaleY = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_getScaleY").orElseThrow(),
        FunctionDescriptor.of(JAVA_FLOAT, ADDRESS)
      );

      // void RGFW_window_setViewportDestination(RGFW_window* win, int logicalWidth, int logicalHeight)
      RGFW_window_setViewportDestination = LINKER.downcallHandle(
        LOOKUP.find("RGFW_window_setViewportDestination").orElseThrow(),
        FunctionDescriptor.ofVoid(ADDRESS, JAVA_INT, JAVA_INT)
      );

    } catch (Throwable e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static MemorySegment createWindow(String name, int x, int y, int w, int h, long flags, Arena arena) {
    try {
      var nameSegment = arena.allocateFrom(name);
      return (MemorySegment) RGFW_createWindow.invoke(nameSegment, x, y, w, h, flags);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to create RGFW window", e);
    }
  }

  public static void windowClose(MemorySegment window) {
    try {
      RGFW_window_close.invoke(window);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to close RGFW window", e);
    }
  }

  public static boolean windowShouldClose(MemorySegment window) {
    try {
      return ((byte) RGFW_window_shouldClose.invoke(window)) != 0;
    } catch (Throwable e) {
      throw new RuntimeException("Failed to check window should close", e);
    }
  }

  public static void pollEvents() {
    try {
      RGFW_pollEvents.invoke();
    } catch (Throwable e) {
      throw new RuntimeException("Failed to poll events", e);
    }
  }

  public static boolean windowCheckEvent(MemorySegment window, MemorySegment event) {
    try {
      return ((byte) RGFW_window_checkEvent.invoke(window, event)) != 0;
    } catch (Throwable e) {
      throw new RuntimeException("Failed to check event", e);
    }
  }

  public static void setWindowResizedCallback(MemorySegment callback) {
    try {
      RGFW_setWindowResizedCallback.invoke(callback);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set resize callback", e);
    }
  }

  public static void setScaleUpdatedCallback(MemorySegment callback) {
    try {
      RGFW_setScaleUpdatedCallback.invoke(callback);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set scale callback", e);
    }
  }

  public static void setWindowQuitCallback(MemorySegment callback) {
    try {
      RGFW_setWindowQuitCallback.invoke(callback);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set quit callback", e);
    }
  }

  public static void setFocusCallback(MemorySegment callback) {
    try {
      RGFW_setFocusCallback.invoke(callback);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set focus callback", e);
    }
  }

  public static void useWayland(boolean enable) {
    try {
      RGFW_useWayland.invoke((byte) (enable ? 1 : 0));
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set Wayland mode", e);
    }
  }

  // Window dimension accessors using RGFW_window_getSize
  public static void getWindowSize(MemorySegment window, int[] outWidth, int[] outHeight, Arena arena) {
    var wPtr = arena.allocate(JAVA_INT);
    var hPtr = arena.allocate(JAVA_INT);
    try {
      // Returns RGFW_bool (u8 / byte) - ignored
      RGFW_window_getSize.invoke(window, wPtr, hPtr);
      outWidth[0] = wPtr.get(JAVA_INT, 0);
      outHeight[0] = hPtr.get(JAVA_INT, 0);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to get window size", e);
    }
  }

  /**
   * Resizes a window programmatically.
   *
   * @param window The window handle
   * @param width  New window width
   * @param height New window height
   */
  public static void resizeWindow(MemorySegment window, int width, int height) {
    try {
      RGFW_window_resize.invoke(window, width, height);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to resize window", e);
    }
  }

  /**
   * Get HiDPI scale factors for a window.
   * On HiDPI displays (e.g., 275% scaling), this returns the actual scale (e.g., 2.74f).
   * Applications should multiply logical dimensions by scale to get physical pixel dimensions
   * for creating properly sized Vulkan swapchains.
   *
   * @param window    The window handle
   * @param outScaleX Output array for X scale factor (minimum length 1)
   * @param outScaleY Output array for Y scale factor (minimum length 1)
   */
  public static void getWindowScale(MemorySegment window, float[] outScaleX, float[] outScaleY) {
    try {
      outScaleX[0] = (float) RGFW_window_getScaleX.invoke(window);
      outScaleY[0] = (float) RGFW_window_getScaleY.invoke(window);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to get window scale", e);
    }
  }

  public static void setViewportDestination(MemorySegment window, int logicalWidth, int logicalHeight) {
    try {
      RGFW_window_setViewportDestination.invoke(window, logicalWidth, logicalHeight);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to set viewport destination", e);
    }
  }

  public static int createVulkanSurface(MemorySegment window, MemorySegment instance, MemorySegment pSurface) {
    try {
      return (int) RGFW_window_createSurface_Vulkan.invoke(window, instance, pSurface);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to create Vulkan surface", e);
    }
  }

}
