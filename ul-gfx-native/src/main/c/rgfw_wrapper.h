#ifndef RGFW_WRAPPER_H
#define RGFW_WRAPPER_H

#define RGFW_VULKAN
#define RGFW_WAYLAND
#include "RGFW.h"

// Expose key RGFW types and functions for jextract
typedef struct RGFW_window RGFW_window;
typedef struct RGFW_event RGFW_event;
typedef struct RGFW_monitor RGFW_monitor;

// Window management
RGFW_window* RGFW_createWindow(const char* name, i32 x, i32 y, u32 w, u32 h, u64 flags);
void RGFW_window_close(RGFW_window* win);
RGFW_bool RGFW_window_shouldClose(RGFW_window* win);
void RGFW_window_setShouldClose(RGFW_window* win, RGFW_bool shouldClose);

// Event handling
RGFW_bool RGFW_window_checkEvent(RGFW_window* win, RGFW_event* event);
void RGFW_pollEvents(void);

// Vulkan
VkResult RGFW_window_createSurface_Vulkan(RGFW_window* win, VkInstance instance, VkSurfaceKHR* surface);

// Callbacks
typedef void (*RGFW_windowResizedfunc)(RGFW_window* win, i32 w, i32 h);
typedef void (*RGFW_scaleUpdatedfunc)(RGFW_window* win, float scaleX, float scaleY);
typedef void (*RGFW_windowQuitfunc)(RGFW_window* win);

RGFW_windowResizedfunc RGFW_setWindowResizedCallback(RGFW_windowResizedfunc func);
RGFW_scaleUpdatedfunc RGFW_setScaleUpdatedCallback(RGFW_scaleUpdatedfunc func);
RGFW_windowQuitfunc RGFW_setWindowQuitCallback(RGFW_windowQuitfunc func);

// Wayland control
void RGFW_useWayland(RGFW_bool wayland);
RGFW_bool RGFW_usingWayland(void);

#endif // RGFW_WRAPPER_H
