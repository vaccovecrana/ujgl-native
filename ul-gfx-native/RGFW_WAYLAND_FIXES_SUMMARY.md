# RGFW Wayland Fixes Summary

## Overview
This document summarizes the fixes applied to RGFW for proper Wayland support, including libdecor decorations, programmatic window resizing, and multi-window stability.

## Fixes Applied

### 1. **Programmatic Window Resize Fix** (Wayland)
**Problem**: `RGFW_window_resize()` on Wayland was calling `xdg_surface_set_window_geometry()` but not committing the changes to the compositor.

**Solution**: Added `wl_surface_commit(win->src.surface);` after setting window geometry.

**Location**: `RGFW.h` line ~9026
```c
void RGFW_FUNC(RGFW_window_resize) (RGFW_window* win, i32 w, i32 h) {
	RGFW_ASSERT(win != NULL);
	win->w = w;
	win->h = h;
	if (_RGFW->compositor) {
		xdg_surface_set_window_geometry(win->src.xdg_surface, 0, 0, win->w, win->h);
		wl_surface_commit(win->src.surface); /* Commit changes to make compositor apply them */
		#ifdef RGFW_OPENGL
		if (win->src.ctx.egl)
			wl_egl_window_resize(win->src.ctx.egl->eglWindow, (i32)w, (i32)h, 0, 0);
		#endif
	}
}
```

**Note**: The compositor may still not honor programmatic resize on borderless windows (this is expected Wayland behavior).

---

### 2. **Multi-Window GTK Crash Fix**
**Problem**: When libdecor is used and GTK initialization fails (common in embedded terminals like Cursor), creating multiple windows sequentially caused a crash:
```
GLib-GObject-CRITICAL: cannot register existing type 'GdkDisplayManager'
```

**Root Cause**: `libdecor_decorate()` internally calls GTK init for each window. After GTK init fails once, calling it again crashes because GTK types are already registered.

**Solution**: 
- Set `decorInitFailed = true` after the first window successfully uses libdecor
- Subsequent windows skip libdecor and use borderless mode (manual xdg-shell)
- Use a static `libdecor* staticDecorContext` that persists across `RGFW_init`/`RGFW_deinit` cycles
- Don't free `libdecor` context in `RGFW_deinitPlatform_Wayland` to avoid GTK re-initialization

**Location**: `RGFW.h` lines ~8700-8850

**Behavior**:
- **Window 1**: Attempts libdecor → gets decorations (if GTK works) or borderless (if GTK fails)
- **Windows 2+**: Skips libdecor → always borderless mode

---

### 3. **Non-Blocking Event Polling Fix**
**Problem**: `RGFW_pollEvents()` was using `wl_display_dispatch()`, which is a blocking call. Applications would hang indefinitely waiting for events (especially noticeable in non-interactive terminals).

**Solution**: 
- Changed `wl_display_dispatch()` to `wl_display_dispatch_pending()`
- Added `poll()` with 0 timeout to check for new events without blocking
- Added `libdecor_dispatch(_RGFW->decorContext, 0);` to process libdecor events

**Location**: `RGFW.h` lines ~7600-7650

---

### 4. **libdecor Integration Fix**
**Problem**: RGFW was manually creating `xdg_surface`, and then libdecor was attempting to create its own, leading to:
```
xdg_wm_base::get_xdg_surface already requested
```

**Solution**: Refactored window creation to let libdecor manage xdg-shell surfaces when `xdg-decoration` protocol is not available:
- If `_RGFW->decoration_manager` exists → use server-side decorations
- If NULL → initialize libdecor and use `libdecor_frame_get_xdg_surface()` / `libdecor_frame_get_xdg_toplevel()`

---

### 5. **Double-Free Fix on Window Close**
**Problem**: When using libdecor, `xdg_surface` and `xdg_toplevel` are owned by libdecor. After `libdecor_frame_unref()`, the code was attempting to destroy these surfaces again.

**Solution**: Set `win->src.xdg_surface = NULL;` and `win->src.xdg_toplevel = NULL;` after `libdecor_frame_unref()`.

---

## Java Bindings Status

### ✅ Completed
- FFI bindings for `RGFW_window_resize` implemented in `UlRgfwFunctions.java`
- Test coverage in `UlRgfwClientTest.java`
- Multi-window stability verified (all 7 tests pass)

### Programmatic Resize Behavior
The `resizeWindow()` function works correctly (calls the fixed C code with `wl_surface_commit`), but the resize callback **may not be triggered** on Wayland. This is expected behavior:
- Wayland compositors may not honor programmatic resize requests on borderless windows
- Manual resize via window decorations (when available) works correctly
- The function executes without errors and the C fix is confirmed present

---

## Testing
All C and Java tests pass:
- ✅ C: `libdecor_test` (single window with decorations)
- ✅ C: `libdecor_multi_test` (3 windows sequentially)
- ✅ Java: All 7 `UlRgfwClientTest` tests pass

---

## Upstream Status
All fixes have been committed to the user's GitHub fork:
- Repository: `https://github.com/jjzazuet/RGFW`
- Branch: `main`
- Latest commit: `9eab465` - "Fix GTK re-initialization crash in multi-window Wayland scenarios"

---

## Known Limitations
1. **Borderless Windows After First**: Due to GTK re-initialization issues, only the first window attempts libdecor. Subsequent windows are borderless.
2. **Programmatic Resize**: Wayland compositors may not honor programmatic resize on borderless windows (expected Wayland behavior).
3. **GTK Plugin Issues**: If `libdecor-gtk.so` fails to initialize (no GTK in environment), falls back to borderless mode.

---

## Recommendations for Upstream PR
The current fixes are functional but have trade-offs. For an upstream PR to RGFW, consider:
1. Properly reset GTK state between windows (if possible)
2. Detect libdecor plugin availability before attempting initialization
3. Provide a `RGFW_windowForceBorderless` flag for explicit control
4. Document Wayland-specific behaviors clearly in the API docs
