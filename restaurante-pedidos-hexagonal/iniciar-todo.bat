@echo off
setlocal
rem Levanta los seis procesos del sistema, cada uno en su propia ventana.
rem Uso:  iniciar-todo.bat            (compila solo si faltan los .jar)
rem       iniciar-todo.bat compilar   (vuelve a compilar antes de arrancar)
rem Para detener todo, cierra las ventanas que se abrieron.

cd /d "%~dp0"

rem Se usa el timeout de Windows con su ruta completa: si el .bat se lanza desde Git Bash,
rem el "timeout" que se encuentra primero es el de Linux y no entiende /t.
set "ESPERA=%SystemRoot%\System32\timeout.exe"

if /i "%~1"=="compilar" goto compilar
if not exist "gateway\target\gateway-0.1.0.jar" goto compilar
goto arrancar

:compilar
echo Compilando con Maven (sin pruebas)...
call mvn -q -DskipTests package
if errorlevel 1 (
    echo No se pudo compilar. Revisa que Maven y el JDK 17 esten instalados.
    pause
    exit /b 1
)

:arrancar
rem 1. El bus va primero: los demas se suscriben a el al arrancar.
start "Bus de eventos - 8090" cmd /k java -jar bus-eventos\target\bus-eventos-0.1.0.jar
"%ESPERA%" /t 2 /nobreak >nul

rem 2. Menu, Cocina y Caja. Cocina y Caja tienen que estar suscritas antes de que
rem    Ordenes publique la orden de ejemplo; si no, no se enteran de ella.
start "Menu - 8081" cmd /k java -jar servicio-menu\target\servicio-menu-0.1.0.jar --demo
start "Cocina - 8083" cmd /k java -jar servicio-cocina\target\servicio-cocina-0.1.0.jar
start "Caja - 8084" cmd /k java -jar servicio-caja\target\servicio-caja-0.1.0.jar
"%ESPERA%" /t 4 /nobreak >nul

rem 3. Ordenes (necesita al Menu arriba para cargar la orden de ejemplo) y el gateway.
start "Ordenes - 8082" cmd /k java -jar monolito\target\monolito-0.1.0.jar --demo
start "Gateway - 8080" cmd /k java -jar gateway\target\gateway-0.1.0.jar
"%ESPERA%" /t 3 /nobreak >nul

start "" http://localhost:8080
echo Listo. La interfaz esta en http://localhost:8080
endlocal