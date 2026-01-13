package io.vacco.ujgl.gfx.context;

import io.vacco.ujgl.gfx.glfw.glfw3_h;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static j8spec.J8Spec.*;
import static org.junit.Assert.*;

/**
 * Tests for UlGfContext and UlGfClient integration.
 */
@RunWith(J8SpecRunner.class)
public class UlGfClientTest {

  static {
    it("should run headless task successfully", () -> {
      var executed = new AtomicBoolean(false);
      UlGfContext.run(() -> executed.set(true));
      assertTrue("Headless task should execute", executed.get());
    });

    it("should create window and call onWindowReady", () -> {
      var readyCalled = new AtomicBoolean(false);
      
      var client = new UlGfClient() {
        @Override
        public void onWindowReady(MemorySegment window, int framebufferWidth, int framebufferHeight) {
          assertNotNull("Window should not be null", window);
          assertNotEquals("Window should not be NULL segment", MemorySegment.NULL, window);
          assertTrue("Framebuffer width should be positive", framebufferWidth > 0);
          assertTrue("Framebuffer height should be positive", framebufferHeight > 0);
          readyCalled.set(true);
        }

        @Override
        public void onWindowResize(MemorySegment window, int width, int height) {}

        @Override
        public void onFramebufferResize(MemorySegment window, int width, int height) {}

        @Override
        public boolean onRender(MemorySegment window) {
          return false; // Exit immediately
        }

        @Override
        public boolean onWindowClose(MemorySegment window) {
          return true;
        }
      };

      UlGfContext.runWithWindow(640, 480, "Test Window", client);
      assertTrue("onWindowReady should be called", readyCalled.get());
    });

    it("should call onRender in a loop until false returned", () -> {
      var renderCount = new AtomicInteger(0);
      
      var client = new UlGfClient() {
        @Override
        public void onWindowReady(MemorySegment window, int framebufferWidth, int framebufferHeight) {}

        @Override
        public void onWindowResize(MemorySegment window, int width, int height) {}

        @Override
        public void onFramebufferResize(MemorySegment window, int width, int height) {}

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

      UlGfContext.runWithWindow(640, 480, "Render Test", client);
      assertEquals("onRender should be called 5 times", 5, renderCount.get());
    });

    it("should setup window resize callback handler", () -> {
      var resizeCalled = new AtomicBoolean(false);
      var resizeWidth = new AtomicInteger(0);
      var resizeHeight = new AtomicInteger(0);
      var renderCount = new AtomicInteger(0);
      
      var client = new UlGfClient() {
        @Override
        public void onWindowReady(MemorySegment window, int framebufferWidth, int framebufferHeight) {
          glfw3_h.glfwSetWindowSize(window, 800, 600);
        }

        @Override
        public void onWindowResize(MemorySegment window, int width, int height) {
          resizeCalled.set(true);
          resizeWidth.set(width);
          resizeHeight.set(height);
        }

        @Override
        public void onFramebufferResize(MemorySegment window, int width, int height) {}

        @Override
        public boolean onRender(MemorySegment window) {
          return renderCount.incrementAndGet() < 2;
        }

        @Override
        public boolean onWindowClose(MemorySegment window) {
          return true;
        }
      };

      UlGfContext.runWithWindow(640, 480, "Resize Test", client);
      if (resizeCalled.get()) {
        assertEquals("Resize width should be 800 when called", 800, resizeWidth.get());
        assertEquals("Resize height should be 600 when called", 600, resizeHeight.get());
      }
    });

    it("should exit loop when onRender returns false", () -> {
      var renderCount = new AtomicInteger(0);
      
      var client = new UlGfClient() {
        @Override
        public void onWindowReady(MemorySegment window, int framebufferWidth, int framebufferHeight) {
          System.out.println("window ready");
        }

        @Override
        public void onWindowResize(MemorySegment window, int width, int height) {
          System.out.println("window resize");
        }

        @Override
        public void onFramebufferResize(MemorySegment window, int width, int height) {
          System.out.println("windo framebuffer resize");
        }

        @Override
        public boolean onRender(MemorySegment window) {
          int count = renderCount.incrementAndGet();
          try {
            System.out.println("window render");
            Thread.sleep(1000);
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

      UlGfContext.runWithWindow(640, 480, "Render Exit Test", client);
      assertEquals("Loop should exit after onRender returns false", 6, renderCount.get());
    });

    it("should cleanup callbacks on window destroy", () -> {
      var client = new UlGfClient() {
        @Override
        public void onWindowReady(MemorySegment window, int framebufferWidth, int framebufferHeight) {}

        @Override
        public void onWindowResize(MemorySegment window, int width, int height) {}

        @Override
        public void onFramebufferResize(MemorySegment window, int width, int height) {}

        @Override
        public boolean onRender(MemorySegment window) {
          return false;
        }

        @Override
        public boolean onWindowClose(MemorySegment window) {
          return true;
        }
      };

      UlGfContext.runWithWindow(640, 480, "Cleanup Test", client);
    });
  }

}
