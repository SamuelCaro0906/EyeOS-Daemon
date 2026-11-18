@echo off
setlocal
set "EXE=%~dp0target\dist\EyeOS-Cliente\EyeOS-Cliente.exe"
if not exist "%EXE%" (
  echo ERROR: falta el ejecutable.
  echo Ejecute primero COMPILAR_EXPORTAR.bat.
  pause
  exit /b 1
)
start "" "%EXE%"
