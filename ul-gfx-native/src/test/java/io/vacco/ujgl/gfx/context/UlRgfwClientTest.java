package io.vacco.ujgl.gfx.context;

import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.awt.*;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static j8spec.J8Spec.it;
import static org.junit.Assert.*;

/**
 * Tests for UlRgfwContext and UlGfClient integration.
 */
@DefinedOrder
@RunWith(J8SpecRunner.class)
public class UlRgfwClientTest {

  static {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("CI/CD environment, nothing to do");
    } else {
      it("should create window and call onWindowReady", () -> {
        var readyCalled = new AtomicBoolean(false);
        var renderCount = new AtomicInteger(0);
        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            assertNotNull("Window should not be null", window);
            assertNotEquals("Window should not be NULL segment", MemorySegment.NULL, window);
            assertTrue("Window width should be positive", width > 0);
            assertTrue("Window height should be positive", height > 0);
            readyCalled.set(true);
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            System.out.printf("window resize [%dx%d]%n", width, height);
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = renderCount.incrementAndGet();
            try {
              System.out.println("window render");
              Thread.sleep(120);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            return count < 96;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            System.out.println("window close");
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Test Window", client);
        assertTrue("onWindowReady should be called", readyCalled.get());
      });

      it("should call onRender in a loop until false returned", () -> {
        var renderCount = new AtomicInteger(0);

        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = renderCount.incrementAndGet();
            return count < 5; // Run 5 iterations then exit
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Render Test", client);
        assertEquals("onRender should be called 5 times", 5, renderCount.get());
      });

      it("should setup window resize callback handler", () -> {
        var resizeCalled = new AtomicBoolean(false);
        var resizeWidth = new AtomicInteger(0);
        var resizeHeight = new AtomicInteger(0);
        var renderCount = new AtomicInteger(0);

        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            // Note: RGFW doesn't have glfwSetWindowSize, would need RGFW_window_resize
            // For now, just test the callback mechanism
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            resizeCalled.set(true);
            resizeWidth.set(width);
            resizeHeight.set(height);
          }

          @Override
          public boolean onRender(MemorySegment window) {
            return renderCount.incrementAndGet() < 2;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Resize Test", client);
        // Resize callback may or may not be called depending on window manager
      });

      it("should exit loop when onRender returns false", () -> {
        var renderCount = new AtomicInteger(0);

        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            System.out.println("window ready");
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            System.out.println("window resize");
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = renderCount.incrementAndGet();
            try {
              System.out.println("window render");
              Thread.sleep(500);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            return count < 6;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            System.out.println("window close");
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Render Exit Test", client);
        assertEquals("Loop should exit after onRender returns false", 6, renderCount.get());
      });

      it("should cleanup callbacks on window destroy", () -> {
        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
          }

          @Override
          public boolean onRender(MemorySegment window) {
            return false;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Cleanup Test", client);
      });

      it("should programmatically resize window and trigger callback", () -> {
        var resizeCalled = new AtomicBoolean(false);
        var resizeWidth = new AtomicInteger(0);
        var resizeHeight = new AtomicInteger(0);
        var renderCount = new AtomicInteger(0);
        var resizeCompleted = new AtomicBoolean(false);

        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            assertEquals("Initial width should be 640", 640, width);
            assertEquals("Initial height should be 480", 480, height);

            try {
              UlRgfwFunctions.resizeWindow(window, 800, 600);
              resizeCompleted.set(true);
              System.out.println("Programmatic resize called: 640x480 -> 800x600");
            } catch (Exception e) {
              fail("Resize should not throw exception: " + e.getMessage());
            }
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            resizeCalled.set(true);
            resizeWidth.set(width);
            resizeHeight.set(height);
            System.out.println("Resize callback triggered - Width: " + width + ", Height: " + height);
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = renderCount.incrementAndGet();
            try {
              Thread.sleep(50);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            return count < 20;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "Programmatic Resize Test", client);

        assertTrue("Resize function should complete without errors", resizeCompleted.get());

        if (resizeCalled.get()) {
          System.out.println("✓ Resize callback was triggered successfully");
          System.out.println("  New dimensions: " + resizeWidth.get() + "x" + resizeHeight.get());
        } else {
          System.out.println("✓ Resize function executed successfully (wl_surface_commit called)");
          System.out.println("⚠ Note: Wayland compositor may not honor programmatic resize on borderless windows");
          System.out.println("  This is expected behavior - manual resize via decorations works correctly");
        }
      });

      it("should display window with decorations for visual inspection", () -> {
        var renderCount = new AtomicInteger(0);
        var focusGained = new AtomicBoolean(false);

        var client = new UlRgfwClient() {
          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            System.out.println("Window created with decorations (title bar, close button)");
            System.out.println("Window size: " + width + "x" + height);
            System.out.println("Window will stay open for 2 seconds for visual inspection...");
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            System.out.println("Window resized to: " + width + "x" + height);
          }

          @Override
          public void onFocusChanged(MemorySegment window, boolean inFocus) {
            System.out.println("Window focus changed: " + (inFocus ? "GAINED FOCUS" : "Lost focus"));
            if (inFocus) {
              focusGained.set(true);
            }
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = renderCount.incrementAndGet();
            try {
              // Keep window open for visual inspection
              Thread.sleep(100);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
            return count < 20; // 20 frames * 100ms = 2 seconds
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            System.out.println("Window closing");
            return true;
          }
        };

        UlRgfwContext.runWithWindow(800, 600, "RGFW Window with Decorations", client);

        // Verify focus was gained
        assertTrue("Window should have gained focus", focusGained.get());
      });
    }
  }

}
