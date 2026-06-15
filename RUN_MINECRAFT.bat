@echo off
setlocal EnableExtensions

set "SCRIPT_DIR=%~dp0"
set "LAUNCH_ARGS=%*"

pushd "%SCRIPT_DIR%" || exit /b 1

if not exist "run\logs" mkdir "run\logs" >NUL 2>&1
set "LAUNCHER_LOG=run\logs\launcher-latest.log"
for %%I in ("%CD%\%LAUNCHER_LOG%") do set "LAUNCHER_LOG_ABS=%%~fI"

> "%LAUNCHER_LOG%" (
    echo Witchery launcher preflight
    echo Timestamp: %DATE% %TIME%
    echo Repository: %CD%
    echo Script: %~f0
    echo Args: %LAUNCH_ARGS%
    echo.
)

if not exist "gradlew.bat" (
    set "EXIT_CODE=1"
    >> "%LAUNCHER_LOG%" echo ERROR: Missing Gradle wrapper script: gradlew.bat
    echo ERROR: Missing Gradle wrapper script: gradlew.bat
    goto fail
)
>> "%LAUNCHER_LOG%" echo OK: Gradle wrapper script found at gradlew.bat

if not exist "gradle\wrapper\gradle-wrapper.jar" (
    set "EXIT_CODE=1"
    >> "%LAUNCHER_LOG%" echo ERROR: Missing Gradle wrapper jar: gradle\wrapper\gradle-wrapper.jar
    >> "%LAUNCHER_LOG%" echo This file is tracked in the repository. If it is missing, the download/extract is incomplete or antivirus removed the jar.
    echo ERROR: Missing Gradle wrapper jar: gradle\wrapper\gradle-wrapper.jar
    echo.
    echo This means the repo copy is incomplete, or antivirus/security software removed the jar.
    echo Fix: re-download or git clone the full repository, then make sure this file exists:
    echo   gradle\wrapper\gradle-wrapper.jar
    echo.
    goto fail
)
>> "%LAUNCHER_LOG%" echo OK: Gradle wrapper jar found at gradle\wrapper\gradle-wrapper.jar

if not exist "gradle\wrapper\gradle-wrapper.properties" (
    set "EXIT_CODE=1"
    >> "%LAUNCHER_LOG%" echo ERROR: Missing Gradle wrapper properties: gradle\wrapper\gradle-wrapper.properties
    echo ERROR: Missing Gradle wrapper properties: gradle\wrapper\gradle-wrapper.properties
    goto fail
)
>> "%LAUNCHER_LOG%" echo OK: Gradle wrapper properties found at gradle\wrapper\gradle-wrapper.properties

if not exist "build.gradle" (
    set "EXIT_CODE=1"
    >> "%LAUNCHER_LOG%" echo ERROR: Missing Gradle build file: build.gradle
    echo ERROR: Missing Gradle build file: build.gradle
    goto fail
)
>> "%LAUNCHER_LOG%" echo OK: Gradle build file found at build.gradle

if not exist "settings.gradle" (
    set "EXIT_CODE=1"
    >> "%LAUNCHER_LOG%" echo ERROR: Missing Gradle settings file: settings.gradle
    echo ERROR: Missing Gradle settings file: settings.gradle
    goto fail
)
>> "%LAUNCHER_LOG%" echo OK: Gradle settings file found at settings.gradle

if defined JAVA_HOME (
    >> "%LAUNCHER_LOG%" echo JAVA_HOME=%JAVA_HOME%
    if not exist "%JAVA_HOME%\bin\java.exe" (
        set "EXIT_CODE=1"
        >> "%LAUNCHER_LOG%" echo ERROR: JAVA_HOME does not contain bin\java.exe.
        echo ERROR: JAVA_HOME is set, but "%JAVA_HOME%\bin\java.exe" does not exist.
        goto fail
    )
    >> "%LAUNCHER_LOG%" echo Java executable: %JAVA_HOME%\bin\java.exe
    "%JAVA_HOME%\bin\java.exe" -version >> "%LAUNCHER_LOG%" 2>&1
    "%JAVA_HOME%\bin\java.exe" --list-modules | findstr /B /C:"jdk.compiler@" >NUL 2>&1
    if errorlevel 1 (
        set "EXIT_CODE=1"
        >> "%LAUNCHER_LOG%" echo ERROR: JAVA_HOME points to a Java runtime without the jdk.compiler module.
        echo ERROR: Java 21 JDK is required, but JAVA_HOME does not provide the Java compiler.
        echo Install JDK 21, not a JRE and not JDK 25/26, then set JAVA_HOME to the JDK folder.
        echo Example: C:\Program Files\Eclipse Adoptium\jdk-21...
        goto fail
    )
    >> "%LAUNCHER_LOG%" echo OK: jdk.compiler module found.
) else (
    where java >NUL 2>&1
    if errorlevel 1 (
        set "EXIT_CODE=1"
        >> "%LAUNCHER_LOG%" echo ERROR: Java was not found in PATH and JAVA_HOME is not set.
        echo ERROR: Java was not found. Install Java 21 or set JAVA_HOME.
        goto fail
    )
    >> "%LAUNCHER_LOG%" echo Java executable from PATH:
    where java >> "%LAUNCHER_LOG%" 2>&1
    java -version >> "%LAUNCHER_LOG%" 2>&1
    java --list-modules | findstr /B /C:"jdk.compiler@" >NUL 2>&1
    if errorlevel 1 (
        set "EXIT_CODE=1"
        >> "%LAUNCHER_LOG%" echo ERROR: Java on PATH does not provide the jdk.compiler module.
        echo ERROR: Java 21 JDK is required, but the first java.exe on PATH is a JRE.
        echo Install JDK 21, not a JRE and not JDK 25/26, then set JAVA_HOME to the JDK folder.
        echo Example: C:\Program Files\Eclipse Adoptium\jdk-21...
        goto fail
    )
    >> "%LAUNCHER_LOG%" echo OK: jdk.compiler module found.
)

set "RAM_GB="
for /f %%R in ('powershell -NoProfile -Command "try { [int][math]::Round((Get-CimInstance Win32_ComputerSystem).TotalPhysicalMemory / 1GB) } catch { '' }" 2^>NUL') do set "RAM_GB=%%R"
if defined RAM_GB (
    >> "%LAUNCHER_LOG%" echo Physical memory: %RAM_GB% GB
    if %RAM_GB% LSS 8 (
        echo WARNING: This PC appears to have %RAM_GB% GB RAM. The dev client requests up to 6 GB.
        >> "%LAUNCHER_LOG%" echo WARNING: Low RAM for the configured -Xmx6G dev client.
    )
)

if /I "%~1"=="--diagnose" goto diagnose

echo.
echo Launcher diagnostics: %LAUNCHER_LOG_ABS%
echo.

call "%SCRIPT_DIR%gradlew.bat" runClient %*
set "EXIT_CODE=%ERRORLEVEL%"
>> "%LAUNCHER_LOG%" echo.
>> "%LAUNCHER_LOG%" echo runClient exit code: %EXIT_CODE%
if "%EXIT_CODE%"=="0" goto done
goto fail

:diagnose
echo.
echo Running setup diagnosis. This does not launch Minecraft.
echo Writing results to: %LAUNCHER_LOG_ABS%
echo.
>> "%LAUNCHER_LOG%" echo.
>> "%LAUNCHER_LOG%" echo Running Gradle wrapper version check...
call "%SCRIPT_DIR%gradlew.bat" --version >> "%LAUNCHER_LOG%" 2>&1
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" goto fail

>> "%LAUNCHER_LOG%" echo.
>> "%LAUNCHER_LOG%" echo Running client setup check...
call "%SCRIPT_DIR%gradlew.bat" prepareClientRun --stacktrace >> "%LAUNCHER_LOG%" 2>&1
set "EXIT_CODE=%ERRORLEVEL%"
if not "%EXIT_CODE%"=="0" goto fail

>> "%LAUNCHER_LOG%" echo.
>> "%LAUNCHER_LOG%" echo Diagnosis completed successfully.
echo Diagnosis completed successfully.
goto done

:fail
echo.
echo Minecraft failed to start or setup failed with code %EXIT_CODE%.
echo Launcher/preflight log: %LAUNCHER_LOG_ABS%
echo Minecraft logs: run\logs\latest.log, run\logs\debug.log, or run\crash-reports
echo.
echo For support, run:
echo   RUN_MINECRAFT.bat --diagnose
echo   RUN_MINECRAFT.bat --stacktrace
echo.
pause
goto done

:done
popd
exit /b %EXIT_CODE%
