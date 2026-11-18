@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.2"
set "MVN=mvn"
where mvn >nul 2>&1
if errorlevel 1 set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo ERROR: no se encontro JDK 25 en %JAVA_HOME%
  pause
  exit /b 1
)

if not exist "%MVN%" (
  echo ERROR: no se encontro Maven en %MVN%
  pause
  exit /b 1
)

echo Compilando y generando app-images...
attrib -R "%~dp0target\dist\*.*" /S >nul 2>&1
call "%MVN%" -B -Pdist clean verify

if errorlevel 1 (
  echo ERROR: la compilacion fallo.
  pause
  exit /b 1
)

echo.
echo Listo:
echo   %~dp0target\dist\EyeOS-Cliente\EyeOS-Cliente.exe
echo   %~dp0target\dist\EyeOS-Servidor\EyeOS-Servidor.exe
pause
