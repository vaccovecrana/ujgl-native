package io.vacco.ujgl.gfx.context;

import j8spec.annotation.DefinedOrder;
import j8spec.junit.J8SpecRunner;
import org.junit.runner.RunWith;

import java.awt.*;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static j8spec.J8Spec.*;
import static org.junit.Assert.assertTrue;

@DefinedOrder
@RunWith(J8SpecRunner.class)
public class UlRgfwHiDpiTest {

  static {
    if (GraphicsEnvironment.isHeadless()) {
      System.out.println("CI/CD environment, nothing to do");
    } else {
      it("Should report hidpi scale for window", () -> {
        var scaleReported = new AtomicBoolean(false);
        var frameCount = new AtomicInteger(0);

        var client = new UlRgfwClient() {
          private float reportedScaleX = 0f;
          private float reportedScaleY = 0f;

          @Override
          public void onWindowReady(MemorySegment window, int width, int height) {
            System.out.println("Window ready: " + width + "x" + height);
            // Query scale using accessor functions
            var scaleX = new float[1];
            var scaleY = new float[1];
            UlRgfwFunctions.getWindowScale(window, scaleX, scaleY);
            System.out.println("Initial scale (from accessors): " + scaleX[0] + "x x " + scaleY[0] + "x");
            // On HiDPI displays, scale should be > 1.0
            // On normal displays, scale should be 1.0
            assertTrue("Scale X should be positive", scaleX[0] > 0);
            assertTrue("Scale Y should be positive", scaleY[0] > 0);
          }

          @Override
          public void onScaleUpdated(MemorySegment window, float scaleX, float scaleY) {
            System.out.println("🔍 Scale update callback: " + scaleX + "x x " + scaleY + "x");
            reportedScaleX = scaleX;
            reportedScaleY = scaleY;
            scaleReported.set(true);
            // Verify scale values are reasonable
            assertTrue("Scale X should be between 0.5 and 5.0", scaleX >= 0.5f && scaleX <= 5.0f);
            assertTrue("Scale Y should be between 0.5 and 5.0", scaleY >= 0.5f && scaleY <= 5.0f);
          }

          @Override
          public void onWindowResize(MemorySegment window, int width, int height) {
            System.out.println("Window resized: " + width + "x" + height);
          }

          @Override
          public boolean onRender(MemorySegment window) {
            int count = frameCount.incrementAndGet();
            if (count >= 60) { // Run for ~1 second at 60fps
              return false;
            }
            try {
              Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            return true;
          }

          @Override
          public boolean onWindowClose(MemorySegment window) {
            System.out.println("Window closing");
            System.out.println("Final reported scale: " + reportedScaleX + "x x " + reportedScaleY + "x");
            return true;
          }
        };

        UlRgfwContext.runWithWindow(640, 480, "HiDPI Scale Test", client);
        // Verify the scale callback was fired at least once
        assertTrue("Scale callback should have been called", scaleReported.get());
        System.out.println("✅ HiDPI scale test passed");
      });
    }
  }

}
