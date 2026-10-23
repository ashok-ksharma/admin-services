@echo off
REM run-local-stack.bat - local stack for admin-service on Windows (cmd / PowerShell).
REM Same commands as run-local-stack.sh: dependencies in Docker (deps\), the app on the host.
REM
REM Usage (from anywhere):
REM   run-local-stack.bat              same as up
REM   run-local-stack.bat up           start the Docker dependencies, wait until healthy, load sample test data on a fresh DB
REM   run-local-stack.bat down         stop and remove the containers; the DB and keystore volumes are kept
REM   run-local-stack.bat wipe         down + delete the DB and keystore volumes, then up (fresh DB, new keys, sample test data)
REM   run-local-stack.bat status       container status
REM   run-local-stack.bat logs [svc]   follow logs (default: keymanager)
REM   run-local-stack.bat tokens       regenerate the WireMock IAM tokens and reload WireMock
REM   run-local-stack.bat build        Maven package admin-service (tests skipped)
REM   run-local-stack.bat run [port]   run the built jar against the stack (default port 8086; 8096 next to an IDE run)
REM   run-local-stack.bat smoke [port] check dependencies and the app (default port 8086)
REM   run-local-stack.bat help

setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "COMPOSE_FILE=%SCRIPT_DIR%\deps\docker-compose.yml"
for %%I in ("%SCRIPT_DIR%\..") do set "MAVEN_PARENT=%%~fI"

set "CONFIG_URI=http://localhost:51200/config"
set "WIREMOCK_URL=http://localhost:8083"
set "KEYMANAGER_URL=http://localhost:8089/v1/keymanager"
set "SMOKE_USER=globaladmin"

set "CMD=%~1"
if "%CMD%"=="" set "CMD=up"
set "ARG=%~2"
REM All arguments after the command, for "logs" (the .sh passes them all on).
set "REST="
:collect_args
shift
if "%~1"=="" goto :args_done
set "REST=%REST% %1"
goto :collect_args
:args_done

if /i "%CMD%"=="up"     goto :cmd_up
if /i "%CMD%"=="down"   goto :cmd_down
if /i "%CMD%"=="wipe"   goto :cmd_wipe
if /i "%CMD%"=="status" goto :cmd_status
if /i "%CMD%"=="logs"   goto :cmd_logs
if /i "%CMD%"=="tokens" goto :cmd_tokens
if /i "%CMD%"=="build"  goto :cmd_build
if /i "%CMD%"=="run"    goto :cmd_run
if /i "%CMD%"=="smoke"  goto :cmd_smoke
if /i "%CMD%"=="help"   goto :cmd_help
if /i "%CMD%"=="-h"     goto :cmd_help
if /i "%CMD%"=="--help" goto :cmd_help
call :help_text
echo [ERROR] unknown command: %CMD%
exit /b 1

:cmd_up
call :check_docker || exit /b 1
echo [INFO] Starting dependencies (postgres, config-server, wiremock, keymanager)...
docker compose -f "%COMPOSE_FILE%" up -d --wait || exit /b 1
REM Sample test data (deps\mosip-data) is loaded once, on a fresh database; later changes survive "up".
set "ZONES="
for /f %%Z in ('docker compose -f "%COMPOSE_FILE%" exec -T postgres psql -U postgres -d mosip_master -tAc "select count(*) from master.zone"') do set "ZONES=%%Z"
if "%ZONES%"=="0" (
  echo [INFO] Loading sample test data ^(deps\mosip-data^) into mosip_master...
  docker compose -f "%COMPOSE_FILE%" --profile seed run --rm masterdata-loader || exit /b 1
)
docker compose -f "%COMPOSE_FILE%" ps
echo [INFO] Ready. Start admin-service from the IDE with:
echo [INFO]   -Dspring.profiles.active=default,local -Dspring.cloud.config.uri=%CONFIG_URI%
exit /b 0

:cmd_down
call :check_docker || exit /b 1
docker compose -f "%COMPOSE_FILE%" down
exit /b %ERRORLEVEL%

:cmd_wipe
call :check_docker || exit /b 1
REM The keystore holds the keys that mosip_keymgr points to; both volumes go in the same step.
echo [INFO] Removing the containers, the DB volume and the keystore volume...
docker compose -f "%COMPOSE_FILE%" down -v || exit /b 1
goto :cmd_up

:cmd_status
call :check_docker || exit /b 1
docker compose -f "%COMPOSE_FILE%" ps
exit /b %ERRORLEVEL%

:cmd_logs
call :check_docker || exit /b 1
set "SVC=%REST%"
if "%SVC%"=="" set "SVC=keymanager"
docker compose -f "%COMPOSE_FILE%" logs -f %SVC%
exit /b %ERRORLEVEL%

:cmd_tokens
call :need_cmd java || exit /b 1
pushd "%SCRIPT_DIR%"
java tools\jwt\GenerateTokens.java
set "RC=%ERRORLEVEL%"
popd
if not "%RC%"=="0" exit /b %RC%
curl -fs "%WIREMOCK_URL%/__admin/health" >nul 2>&1
if not errorlevel 1 (
  curl -fs -X POST "%WIREMOCK_URL%/__admin/mappings/reset" >nul
  echo [INFO] WireMock reloaded the mappings
)
exit /b 0

:cmd_build
call :need_cmd mvn || exit /b 1
pushd "%MAVEN_PARENT%"
call mvn -pl admin-service -am package -DskipTests -Dgpg.skip=true
set "RC=%ERRORLEVEL%"
popd
exit /b %RC%

:cmd_run
call :need_cmd java || exit /b 1
set "PORT=%ARG%"
if "%PORT%"=="" set "PORT=8086"
set "JAR="
for %%F in ("%MAVEN_PARENT%\admin-service\target\admin-service-*.jar") do (
  echo %%~nxF | findstr /i /c:"-sources" /c:"-javadoc" /c:".original" >nul || set "JAR=%%~fF"
)
if not defined JAR (
  echo [ERROR] admin-service jar not found; run: %~nx0 build
  exit /b 1
)
curl -fs "%CONFIG_URI%/admin/default,local" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] config server not reachable; run: %~nx0 up
  exit /b 1
)
echo [INFO] Running %JAR% on port %PORT% (Ctrl+C to stop)
java --add-modules=ALL-SYSTEM --add-opens=java.base/java.lang=ALL-UNNAMED ^
  -Dspring.profiles.active=default,local ^
  -Dspring.cloud.config.uri=%CONFIG_URI% ^
  -Dserver.port=%PORT% ^
  -jar "%JAR%"
exit /b %ERRORLEVEL%

:cmd_smoke
set "PORT=%ARG%"
if "%PORT%"=="" set "PORT=8086"
set "APP=http://localhost:%PORT%"
set "SMOKE_FAILED=0"
echo [INFO] Dependencies
call :check "config-server admin/default,local" 200 "%CONFIG_URI%/admin/default,local"
call :check "wiremock health" 200 "%WIREMOCK_URL%/__admin/health"
call :check "keymanager health" 200 "%KEYMANAGER_URL%/actuator/health"
call :check "keymanager KERNEL:SIGN certificate" 200 "%KEYMANAGER_URL%/getCertificate?applicationId=KERNEL&referenceId=SIGN"

set "TOKEN="
set "LOGIN_BODY=%TEMP%\admin-local-login.json"
> "%LOGIN_BODY%" echo {"request":{"userName":"%SMOKE_USER%"}}
for /f "tokens=1,2" %%A in ('curl -s -D - -o nul -X POST "%WIREMOCK_URL%/v1/authmanager/authenticate/internal/useridPwd" -H "Content-Type: application/json" --data-binary "@%LOGIN_BODY%"') do (
  if /i "%%A"=="authorization:" set "TOKEN=%%B"
)
del /q "%LOGIN_BODY%" >nul 2>&1
if not defined TOKEN (
  echo   [FAIL] login %SMOKE_USER% on WireMock
  set "SMOKE_FAILED=1"
)

echo [INFO] admin-service on port %PORT%
call :check "health" 200 "%APP%/v1/admin/actuator/health"
call :check "masterdata languages (token)" 200 "%APP%/v1/masterdata/languages" "Authorization=!TOKEN!"
call :check "masterdata languages (no token)" 401 "%APP%/v1/masterdata/languages"
call :check "admin roles (token)" 200 "%APP%/v1/admin/roles" "Authorization=!TOKEN!"
REM Same text as the .sh looks for: "code":"MOR". findstr regex: each . stands for a quote, which cmd cannot pass inside a quoted argument.
set "ZONE_TEXT="code":"MOR""
call :check_contains "sample test data: zone hierarchy (token)" ZONE_TEXT "code.:.MOR" "%APP%/v1/masterdata/zones/hierarchy/eng" "Authorization=!TOKEN!"
call :check "admin keymanager proxy KERNEL:SIGN (token)" 200 "%APP%/v1/admin/keymanager/getCertificate?applicationId=KERNEL&referenceId=SIGN" "Authorization=!TOKEN!"

if not "%SMOKE_FAILED%"=="0" (
  echo [ERROR] smoke checks failed
  exit /b 1
)
echo [INFO] All smoke checks passed
exit /b 0

:cmd_help
call :help_text
exit /b 0

REM ---------------------------------------------------------------------------
REM check "name" expected-code "url" ["cookie"]
:check
set "CODE="
set "CHECK_NAME=%~1"
set "CHECK_EXPECTED=%~2"
if "%~4"=="" (
  for /f %%C in ('curl -s -o nul -w "%%{http_code}" "%~3"') do set "CODE=%%C"
) else (
  for /f %%C in ('curl -s -o nul -w "%%{http_code}" -b "%~4" "%~3"') do set "CODE=%%C"
)
REM Delayed expansion: check names contain parentheses, which would end the if block.
REM Pad the name to 45 characters, like printf %-45s in the .sh.
set "PADDED=!CHECK_NAME!                                             "
if "!CODE!"=="!CHECK_EXPECTED!" (
  echo   [ OK ] !PADDED:~0,45! !CODE!
) else (
  echo   [FAIL] !PADDED:~0,45! !CODE! ^(expected !CHECK_EXPECTED!^)
  set "SMOKE_FAILED=1"
)
exit /b 0

REM check_contains "name" TEXT_VAR "findstr-regex" "url" "cookie"
REM TEXT_VAR names a variable holding the text to show (it may contain quotes); the regex matches it.
:check_contains
set "CHECK_NAME=%~1"
set "CHECK_SHOWN=!%~2!"
set "FOUND="
for /f "delims=" %%L in ('curl -s -b "%~5" "%~4" ^| findstr /r /c:"%~3"') do set "FOUND=1"
set "PADDED=!CHECK_NAME!                                             "
if defined FOUND (
  echo   [ OK ] !PADDED:~0,45! !CHECK_SHOWN!
) else (
  echo   [FAIL] !PADDED:~0,45! missing !CHECK_SHOWN!
  set "SMOKE_FAILED=1"
)
exit /b 0

REM need_cmd tool: fail if the tool is not on PATH
:need_cmd
where %1 >nul 2>&1 || (echo [ERROR] '%1' not found on PATH & exit /b 1)
exit /b 0

:check_docker
where docker >nul 2>&1 || (echo [ERROR] 'docker' not found on PATH & exit /b 1)
docker info >nul 2>&1 || (echo [ERROR] Docker is not running & exit /b 1)
exit /b 0

:help_text
for /f "usebackq tokens=* delims=" %%L in (`findstr /b /c:"REM " "%~f0"`) do (
  set "LINE=%%L"
  echo(!LINE:~4!
  if "!LINE!"=="REM   run-local-stack.bat help" exit /b 0
)
exit /b 0
