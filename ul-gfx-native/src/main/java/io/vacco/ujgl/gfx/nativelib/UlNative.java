package io.vacco.ujgl.gfx.nativelib;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

/**
 * Loads native libraries from JAR resources and extracts them to a temporary location.
 * Supports platform-specific library loading (Linux, macOS, Windows).
 */
public class UlNative {

  private static final String LIBRARY_NAME = "RGFW";
  private static boolean loaded = false;

  /**
   * Loads the native library for the current platform.
   * The library is extracted from JAR resources if needed.
   *
   * @throws UnsatisfiedLinkError if the library cannot be loaded
   */
  public static synchronized void load() {
    if (loaded) {
      return;
    }

    var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    var arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

    var platform = detectPlatform(os, arch);
    var libraryFileName = getLibraryFileName(platform);

    try {
      // Try to load from system library path first
      System.loadLibrary(LIBRARY_NAME);
      loaded = true;
      return;
    } catch (UnsatisfiedLinkError e) {
      // System library not found, try extracting from JAR
    }

    var extractedPath = extractLibrary(platform, libraryFileName);
    var libraryPath = extractedPath.toAbsolutePath().toString();
    System.load(libraryPath);

    // Also try to load Vulkan loader if available (for Vulkan bindings)
    // Vulkan is provided by the system, so we try to load it
    // This makes Vulkan symbols available to loaderLookup()
    try {
      if (os.contains("linux")) {
        // Try libvulkan.so.1 first (common on Linux)
        try {
          System.load("/usr/lib/x86_64-linux-gnu/libvulkan.so.1");
        } catch (UnsatisfiedLinkError e) {
          // Fallback to loadLibrary which will search system paths
          System.loadLibrary("vulkan");
        }
      } else if (os.contains("mac") || os.contains("darwin")) {
        System.loadLibrary("vulkan");
      } else if (os.contains("windows")) {
        System.loadLibrary("vulkan-1");
      }
    } catch (UnsatisfiedLinkError e) {
      // Vulkan not available - that's okay, it will be loaded by the system when needed
      // The bindings will use defaultLookup() which should find system libraries
    }

    loaded = true;
  }

  private static String detectPlatform(String os, String arch) {
    if (os.contains("linux")) {
      if (arch.equals("amd64") || arch.equals("x86_64")) {
        return "linux-x64";
      }
    } else if (os.contains("mac") || os.contains("darwin")) {
      if (arch.equals("x86_64") || arch.equals("aarch64")) {
        return "macos-x64";
      }
    } else if (os.contains("windows")) {
      if (arch.equals("amd64") || arch.equals("x86_64")) {
        return "windows-x64";
      }
    }

    throw new UnsupportedOperationException(
      "Unsupported platform: " + os + " " + arch
    );
  }

  private static String getLibraryFileName(String platform) {
    if (platform.startsWith("linux")) {
      return "lib" + LIBRARY_NAME + ".so";
    } else if (platform.startsWith("macos")) {
      return LIBRARY_NAME + ".dylib";
    } else if (platform.startsWith("windows")) {
      return LIBRARY_NAME + ".dll";
    }
    throw new UnsupportedOperationException("Unsupported platform: " + platform);
  }

  private static Path extractLibrary(String platform, String libraryFileName) {
    var resourcePath = "/natives/" + platform + "/" + libraryFileName;

    try (var is = UlNative.class.getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new UnsatisfiedLinkError(
          "Native library not found in JAR: " + resourcePath
        );
      }

      var tempDir = Files.createTempDirectory("ujgl-native-");
      var libraryPath = tempDir.resolve(libraryFileName);
      Files.copy(is, libraryPath, StandardCopyOption.REPLACE_EXISTING);

      if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
        libraryPath.toFile().setExecutable(true);
      }

      return libraryPath;
    } catch (IOException e) {
      throw new UnsatisfiedLinkError(
        "Failed to extract native library: " + e.getMessage()
      );
    }
  }
}
