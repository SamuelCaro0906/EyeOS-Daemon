@echo off
setlocal
rem ============================================================
rem  COMPILAR_EXPORTAR.bat
rem  Compila EyeOS y genera las app-images con jpackage.
rem  Detecta JDK 25 y Maven solos; solo hay que tenerlos
rem  instalados (no hace falta editar este archivo).
rem ============================================================

rem --- 1) Localizar JDK 25 -----------------------------------
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok

for /d %%D in ("C:\Program Files\Eclipse Adoptium\jdk-25*") do set "JAVA_HOME=%%~fD"
if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok
for /d %%D in ("C:\Program Files\Java\jdk-25*") do set "JAVA_HOME=%%~fD"
if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok
for /d %%D in ("C:\Program Files\Microsoft\jdk-25*") do set "JAVA_HOME=%%~fD"
if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok
for /d %%D in ("%USERPROFILE%\.jdks\*25*") do set "JAVA_HOME=%%~fD"
if exist "%JAVA_HOME%\bin\java.exe" goto :java_ok

echo ERROR: no se encontro un JDK 25.
echo        Instalalo desde https://adoptium.net/ y vuelve a ejecutar.
pause
exit /b 1

:java_ok
set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- 2) Localizar Maven ------------------------------------
set "MVN=mvn.cmd"
where mvn.cmd >nul 2>&1 && goto :mvn_ok

set "MVN=%LOCALAPPDATA%\Programs\apache-maven\apache-maven-3.10.1\bin\mvn.cmd"
if exist "%MVN%" goto :mvn_ok

for /d %%D in ("%LOCALAPPDATA%\Programs\apache-maven\apache-maven-*") do set "MVN=%%~fD\bin\mvn.cmd"
if exist "%MVN%" goto :mvn_ok

echo ERROR: no se encontro Maven.
echo        Descarga el binario en https://maven.apache.org/download.cgi
echo        y anade su carpeta bin al PATH, o colocalo en:
echo        %LOCALAPPDATA%\Programs\apache-maven
pause
exit /b 1

:mvn_ok
echo   JAVA_HOME = %JAVA_HOME%
echo   Maven     = %MVN%
echo.

rem --- 3) Compilar y empaquetar ------------------------------
echo Compilando y generando app-images...
attrib -R "%~dp0target\dist\*.*" /S >nul 2>&1
call "%MVN%" -B -Pdist clean verify

if errorlevel 1 (
  echo.
  echo ERROR: la compilacion fallo.
  pause
  exit /b 1
)

echo.
echo Listo:
echo   %~dp0target\dist\EyeOS-Cliente\EyeOS-Cliente.exe
echo   %~dp0target\dist\EyeOS-Servidor\EyeOS-Servidor.exe
pause
