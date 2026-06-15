@echo off
setlocal

pushd "%~dp0" || exit /b 1

call "%~dp0gradlew.bat" runClient %*
set "EXIT_CODE=%ERRORLEVEL%"

popd

if not "%EXIT_CODE%"=="0" (
    echo.
    echo Minecraft failed to start or exited with code %EXIT_CODE%.
    echo Check run\logs\latest.log, run\logs\debug.log, or run\crash-reports.
    echo You can also run RUN_MINECRAFT.bat --stacktrace for more Gradle detail.
    echo.
    pause
)

exit /b %EXIT_CODE%
