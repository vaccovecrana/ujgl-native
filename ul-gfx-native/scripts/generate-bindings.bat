@echo off
REM Script to generate JNI bindings using jextract (Windows)
REM Usage: scripts\generate-bindings.bat

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROJECT_DIR=%SCRIPT_DIR%..
cd /d "%PROJECT_DIR%"
set OUTPUT_BASE=%PROJECT_DIR%\src\main\java\io\vacco\ujgl\gfx

echo Generating JNI bindings with jextract...

REM Check if jextract is available
where jextract >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: jextract not found in PATH
    echo Please ensure jextract is installed and available in your PATH
    exit /b 1
)

REM Create output directories
if not exist "%OUTPUT_BASE%\glfw" mkdir "%OUTPUT_BASE%\glfw"
if not exist "%OUTPUT_BASE%\vulkan" mkdir "%OUTPUT_BASE%\vulkan"

REM Find GLFW headers
set GLFW_HEADER=
set GLFW_INCLUDE=
if exist "%PROJECT_DIR%\native\glfw\include\GLFW\glfw3.h" (
    set GLFW_HEADER=%PROJECT_DIR%\native\glfw\include\GLFW\glfw3.h
    set GLFW_INCLUDE=%PROJECT_DIR%\native\glfw\include
) else if exist "C:\Program Files\GLFW\include\GLFW\glfw3.h" (
    set GLFW_HEADER=C:\Program Files\GLFW\include\GLFW\glfw3.h
    set GLFW_INCLUDE=C:\Program Files\GLFW\include
) else (
    echo Error: GLFW headers not found
    echo Please ensure GLFW is installed or download GLFW source to native\glfw\
    exit /b 1
)

REM Find Vulkan headers
set VULKAN_INCLUDE=
if exist "C:\VulkanSDK\Include\vulkan" (
    set VULKAN_INCLUDE=C:\VulkanSDK\Include\vulkan
) else if exist "C:\Program Files\Vulkan SDK\*\Include\vulkan" (
    for /d %%i in ("C:\Program Files\Vulkan SDK\*") do (
        if exist "%%i\Include\vulkan" (
            set VULKAN_INCLUDE=%%i\Include\vulkan
            goto :vulkan_found
        )
    )
    :vulkan_found
) else (
    echo Error: Vulkan headers not found
    echo Please install Vulkan SDK
    exit /b 1
)

echo GLFW header: %GLFW_HEADER%
echo Vulkan include: %VULKAN_INCLUDE%

REM Generate GLFW bindings
echo.
echo Generating GLFW bindings...
jextract -l :glfw ^
    --output "%OUTPUT_BASE%\glfw" ^
    -I "%GLFW_INCLUDE%" ^
    -t io.vacco.ujgl.gfx.glfw ^
    -C "-DGLFW_INCLUDE_VULKAN" ^
    "%GLFW_HEADER%"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to generate GLFW bindings
    exit /b 1
)
echo GLFW bindings generated successfully

REM Generate Vulkan bindings
echo.
echo Generating Vulkan bindings...
jextract -l :vulkan ^
    --output "%OUTPUT_BASE%\vulkan" ^
    -I "%VULKAN_INCLUDE%" ^
    -t io.vacco.ujgl.gfx.vulkan ^
    "%VULKAN_INCLUDE%\vulkan.h" "%VULKAN_INCLUDE%\vulkan_core.h"

if %ERRORLEVEL% NEQ 0 (
    echo Failed to generate Vulkan bindings
    exit /b 1
)
echo Vulkan bindings generated successfully

echo.
echo All bindings generated successfully!
echo Output directory: %OUTPUT_BASE%




