@echo off
REM Script para iniciar el sistema de transferencias interbancarias en Windows
REM Autor: Sistema Bancario
REM Fecha: 2026-02-23

setlocal enabledelayedexpansion

echo.
echo ========================================================
echo    Sistema de Transferencias Interbancarias
echo    PostgreSQL (Nacional) + MySQL (Internacional)
echo ========================================================
echo.

REM Verificar si Docker esta instalado
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta instalado
    pause
    exit /b 1
)

REM Verificar si Docker Compose esta disponible
docker compose version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose no esta disponible
    pause
    exit /b 1
)

echo [INFO] Deteniendo contenedores previos...
docker compose down >nul 2>&1

echo [INFO] Iniciando servicios Docker...
docker compose up -d --build

if errorlevel 1 (
    echo [ERROR] Fallo al iniciar los servicios
    pause
    exit /b 1
)

echo [INFO] Esperando a que los servicios esten listos...
echo.

REM Esperar a que PostgreSQL este listo
echo    Banco Nacional (PostgreSQL):
set /a count=0
:wait_postgres
docker exec banco_nacional_db pg_isready -U admin -d banco_nacional >nul 2>&1
if errorlevel 1 (
    set /a count+=1
    if !count! gtr 30 (
        echo [ERROR] Timeout esperando PostgreSQL
        goto continue_startup
    )
    timeout /t 1 /nobreak >nul
    goto wait_postgres
)
echo       [OK] Listo

REM Esperar a que MySQL este listo
echo    Banco Internacional (MySQL):
set /a count=0
:wait_mysql
docker exec banco_internacional_db mysqladmin ping -h localhost -uroot -padmin >nul 2>&1
if errorlevel 1 (
    set /a count+=1
    if !count! gtr 30 (
        echo [ERROR] Timeout esperando MySQL
        goto continue_startup
    )
    timeout /t 1 /nobreak >nul
    goto wait_mysql
)
echo       [OK] Listo

REM Esperar a que Spring Boot este listo
echo    Backend Spring Boot:
set /a count=0
:wait_springboot
curl -s http://localhost:8080/api/cuentas/nacional/BN-001 >nul 2>&1
if errorlevel 1 (
    set /a count+=1
    if !count! gtr 60 (
        echo [WARNING] Timeout esperando Spring Boot
        echo       El backend puede tardar unos segundos mas...
        goto continue_startup
    )
    timeout /t 1 /nobreak >nul
    goto wait_springboot
)
echo       [OK] Listo

:continue_startup
echo.
echo [OK] Servicios corriendo!
echo.

echo ========================================================
echo  Estado de los contenedores:
echo ========================================================
docker compose ps

echo.
echo [INFO] Abriendo interfaz web en el navegador...
echo.

REM Obtener la ruta absoluta del index.html
set "INDEX_PATH=%CD%\index.html"

REM Abrir el navegador predeterminado
start "" "%INDEX_PATH%"

echo.
echo ========================================================
echo  URLs disponibles:
echo ========================================================
echo    Interfaz Web: file:///%INDEX_PATH%
echo    API Backend:  http://localhost:8080/api
echo    PostgreSQL:   localhost:5432 (admin/admin)
echo    MySQL:        localhost:3306 (root/admin)
echo.
echo ========================================================
echo  Endpoints API:
echo ========================================================
echo    GET  /api/cuentas/nacional/{cuenta}
echo    GET  /api/cuentas/internacional/{cuenta}
echo    POST /api/transferencias/nacional-a-internacional
echo    POST /api/transferencias/internacional-a-nacional
echo.
echo ========================================================
echo  Comandos utiles:
echo ========================================================
echo    Ver logs:     docker compose logs -f backend
echo    Detener todo: docker compose down
echo    Reiniciar:    docker compose restart
echo.
echo [OK] Sistema listo para usar!
echo.
echo Presiona cualquier tecla para cerrar esta ventana...
pause >nul
