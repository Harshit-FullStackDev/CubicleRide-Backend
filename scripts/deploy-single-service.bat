@echo off
echo [INFO] CubicleRide - Step-by-Step Microservices Deployment
echo.

REM Set variables
set ACR_NAME=cubiclerideprodacr
set ACR_LOGIN_SERVER=cubiclerideprodacr.azurecr.io
set CONTAINERAPP_ENV=cubicleride-prod-cae
set MYSQL_FQDN=cubicleride-mysql-sep2025.mysql.database.azure.com

echo [INFO] Infrastructure Details:
echo   - Container Registry: %ACR_NAME%
echo   - ACR Login Server: %ACR_LOGIN_SERVER%
echo   - Container Apps Environment: %CONTAINERAPP_ENV%
echo   - MySQL Server: %MYSQL_FQDN%
echo.

REM Check which service to deploy
if "%1"=="" (
    echo Usage: deploy-single-service.bat [service-name]
    echo.
    echo Available services:
    echo   - eureka-server
    echo   - api-gateway
    echo   - auth-service
    echo   - employee-service
    echo   - ride-service
    echo   - admin-service
    echo   - all ^(deploys all services^)
    echo.
    echo Example: deploy-single-service.bat eureka-server
    goto :end
)

set SERVICE_NAME=%1

if "%SERVICE_NAME%"=="all" (
    echo [INFO] Deploying all services...
    call :deploy_service eureka-server
    timeout /t 30
    call :deploy_service api-gateway
    timeout /t 30
    call :deploy_service auth-service
    timeout /t 30
    call :deploy_service employee-service
    timeout /t 30
    call :deploy_service ride-service
    timeout /t 30
    call :deploy_service admin-service
    goto :show_status
) else (
    call :deploy_service %SERVICE_NAME%
    goto :show_status
)

:deploy_service
set CURRENT_SERVICE=%1
echo.
echo [INFO] ========================================
echo [INFO] Deploying %CURRENT_SERVICE%
echo [INFO] ========================================

echo [INFO] Building Docker image for %CURRENT_SERVICE%...
cd %CURRENT_SERVICE%
docker build -t %ACR_LOGIN_SERVER%/%CURRENT_SERVICE%:latest .
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build %CURRENT_SERVICE%
    cd ..
    goto :eof
)

echo [INFO] Pushing %CURRENT_SERVICE% to registry...
docker push %ACR_LOGIN_SERVER%/%CURRENT_SERVICE%:latest
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to push %CURRENT_SERVICE%
    cd ..
    goto :eof
)

echo [INFO] Deploying %CURRENT_SERVICE% to Container Apps...
cd ..

REM Deploy based on service type
if "%CURRENT_SERVICE%"=="eureka-server" (
    az containerapp create ^
      --name eureka-server ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/eureka-server:latest ^
      --target-port 8761 ^
      --ingress external ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 2
)

if "%CURRENT_SERVICE%"=="api-gateway" (
    az containerapp create ^
      --name api-gateway ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/api-gateway:latest ^
      --target-port 8080 ^
      --ingress external ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 3 ^
      --env-vars ^
        EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka ^
        SPRING_PROFILES_ACTIVE=prod
)

if "%CURRENT_SERVICE%"=="auth-service" (
    az containerapp create ^
      --name auth-service ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/auth-service:latest ^
      --target-port 8081 ^
      --ingress internal ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 2 ^
      --env-vars ^
        EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka ^
        SPRING_PROFILES_ACTIVE=prod ^
        SPRING_DATASOURCE_URL=jdbc:mysql://%MYSQL_FQDN%:3306/cubicleride ^
        SPRING_DATASOURCE_USERNAME=harshit ^
        SPRING_DATASOURCE_PASSWORD=@Manoj007 ^
        JWT_SECRET=qwertyuiopasdfghjklzxcvbnmqwertyuiop
)

if "%CURRENT_SERVICE%"=="employee-service" (
    az containerapp create ^
      --name employee-service ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/employee-service:latest ^
      --target-port 8082 ^
      --ingress internal ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 2 ^
      --env-vars ^
        EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka ^
        SPRING_PROFILES_ACTIVE=prod ^
        SPRING_DATASOURCE_URL=jdbc:mysql://%MYSQL_FQDN%:3306/cubicleride ^
        SPRING_DATASOURCE_USERNAME=harshit ^
        SPRING_DATASOURCE_PASSWORD=@Manoj007 ^
        SPRING_MAIL_USERNAME=harshiitsonii@gmail.com ^
        SPRING_MAIL_PASSWORD=ycjvnzlfukiarbgq
)

if "%CURRENT_SERVICE%"=="ride-service" (
    az containerapp create ^
      --name ride-service ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/ride-service:latest ^
      --target-port 8083 ^
      --ingress internal ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 2 ^
      --env-vars ^
        EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka ^
        SPRING_PROFILES_ACTIVE=prod ^
        SPRING_DATASOURCE_URL=jdbc:mysql://%MYSQL_FQDN%:3306/cubicleride ^
        SPRING_DATASOURCE_USERNAME=harshit ^
        SPRING_DATASOURCE_PASSWORD=@Manoj007
)

if "%CURRENT_SERVICE%"=="admin-service" (
    az containerapp create ^
      --name admin-service ^
      --resource-group rg-cubicleride ^
      --environment %CONTAINERAPP_ENV% ^
      --image %ACR_LOGIN_SERVER%/admin-service:latest ^
      --target-port 8084 ^
      --ingress internal ^
      --registry-server %ACR_LOGIN_SERVER% ^
      --cpu 0.5 ^
      --memory 1Gi ^
      --min-replicas 1 ^
      --max-replicas 2 ^
      --env-vars ^
        EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka ^
        SPRING_PROFILES_ACTIVE=prod ^
        SPRING_DATASOURCE_URL=jdbc:mysql://%MYSQL_FQDN%:3306/cubicleride ^
        SPRING_DATASOURCE_USERNAME=harshit ^
        SPRING_DATASOURCE_PASSWORD=@Manoj007
)

echo [SUCCESS] %CURRENT_SERVICE% deployed successfully!
goto :eof

:show_status
echo.
echo [INFO] Getting deployment status...
az containerapp list --resource-group rg-cubicleride --query "[].{Name:name, Status:properties.provisioningState, URL:properties.configuration.ingress.fqdn}" --output table

echo.
echo [INFO] Getting API Gateway URL...
for /f "tokens=*" %%i in ('az containerapp show --name api-gateway --resource-group rg-cubicleride --query "properties.configuration.ingress.fqdn" -o tsv 2^>nul') do set API_GATEWAY_URL=%%i

if not "%API_GATEWAY_URL%"=="" (
    echo [SUCCESS] Your CubicleRide Backend is accessible at:
    echo   API Gateway: https://%API_GATEWAY_URL%
    echo   Health Check: https://%API_GATEWAY_URL%/actuator/health
    echo   API Docs: https://%API_GATEWAY_URL%/swagger-ui.html
)

:end
pause
