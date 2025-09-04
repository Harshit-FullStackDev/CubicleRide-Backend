@echo off
REM Azure Deployment Script for CubicleRide Backend (Windows)
REM This script deploys the infrastructure and applications to Azure

setlocal EnableDelayedExpansion

REM Configuration
set RESOURCE_GROUP=rg-cubicleride
set LOCATION=eastus
set APPLICATION_NAME=cubicleride
set ENVIRONMENT_NAME=prod

echo [INFO] Starting CubicleRide Backend deployment to Azure...
echo.

REM Check prerequisites
echo [INFO] Checking prerequisites...
where az >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Azure CLI is not installed. Please install it first.
    exit /b 1
)

where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed. Please install it first.
    exit /b 1
)

where docker >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not installed. Please install it first.
    exit /b 1
)

echo [SUCCESS] All prerequisites are available
echo.

REM Azure login
echo [INFO] Checking Azure login status...
az account show >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [INFO] Please login to Azure...
    az login
)

REM Set subscription
for /f "tokens=*" %%i in ('az account show --query id -o tsv') do set SUBSCRIPTION_ID=%%i
az account set --subscription "%SUBSCRIPTION_ID%"
echo [SUCCESS] Using Azure subscription: %SUBSCRIPTION_ID%
echo.

REM Create resource group
echo [INFO] Creating resource group: %RESOURCE_GROUP% in %LOCATION%...
az group show --name "%RESOURCE_GROUP%" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    az group create --name "%RESOURCE_GROUP%" --location "%LOCATION%"
    echo [SUCCESS] Resource group created: %RESOURCE_GROUP%
) else (
    echo [WARNING] Resource group %RESOURCE_GROUP% already exists
)
echo.

REM Build applications
echo [INFO] Building Java applications...
mvn clean package -DskipTests -B
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to build Java applications
    exit /b 1
)
echo [SUCCESS] Java applications built successfully
echo.

REM Get sensitive inputs
if not defined MYSQL_ADMIN_LOGIN (
    set /p MYSQL_ADMIN_LOGIN=Enter MySQL admin username: 
)

if not defined MYSQL_ADMIN_PASSWORD (
    set /p MYSQL_ADMIN_PASSWORD=Enter MySQL admin password: 
)

if not defined JWT_SECRET (
    set /p JWT_SECRET=Enter JWT secret (minimum 32 characters): 
)

if not defined MAIL_USERNAME (
    set /p MAIL_USERNAME=Enter email username for SMTP: 
)

if not defined MAIL_PASSWORD (
    set /p MAIL_PASSWORD=Enter email password for SMTP: 
)

echo.
echo [INFO] Deploying Azure infrastructure...
az deployment group create ^
    --resource-group "%RESOURCE_GROUP%" ^
    --template-file infrastructure/main.bicep ^
    --parameters ^
        applicationName="%APPLICATION_NAME%" ^
        environmentName="%ENVIRONMENT_NAME%" ^
        mysqlAdminLogin="%MYSQL_ADMIN_LOGIN%" ^
        mysqlAdminPassword="%MYSQL_ADMIN_PASSWORD%" ^
        jwtSecret="%JWT_SECRET%" ^
        mailUsername="%MAIL_USERNAME%" ^
        mailPassword="%MAIL_PASSWORD%" ^
    --query "properties.outputs" -o json > deployment-outputs.json

if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to deploy infrastructure
    exit /b 1
)

echo [SUCCESS] Infrastructure deployed successfully
echo.

REM Extract outputs (simplified for Windows batch)
echo [INFO] Please check deployment-outputs.json for infrastructure details
echo.

REM Build and push Docker images
echo [INFO] Building and pushing Docker images...
for /f "tokens=*" %%i in ('az acr credential show --name "%APPLICATION_NAME%%ENVIRONMENT_NAME%acr" --query username -o tsv') do set ACR_USERNAME=%%i
for /f "tokens=*" %%i in ('az acr credential show --name "%APPLICATION_NAME%%ENVIRONMENT_NAME%acr" --query passwords[0].value -o tsv') do set ACR_PASSWORD=%%i
for /f "tokens=*" %%i in ('az acr show --name "%APPLICATION_NAME%%ENVIRONMENT_NAME%acr" --query loginServer -o tsv') do set CONTAINER_REGISTRY_LOGIN_SERVER=%%i

echo %ACR_PASSWORD% | docker login "%CONTAINER_REGISTRY_LOGIN_SERVER%" --username "%ACR_USERNAME%" --password-stdin

REM Build each service
for %%S in (eureka-server api-gateway auth-service employee-service ride-service admin-service) do (
    echo [INFO] Building and pushing %%S...
    cd %%S
    docker build -t "%CONTAINER_REGISTRY_LOGIN_SERVER%/cubicleride/%%S:latest" .
    docker push "%CONTAINER_REGISTRY_LOGIN_SERVER%/cubicleride/%%S:latest"
    cd ..
    echo [SUCCESS] %%S image pushed successfully
)

echo.
echo [SUCCESS] Deployment completed! Check the Azure portal for your resources.
echo [INFO] API Gateway will be available at: https://api-gateway.%LOCATION%.azurecontainerapps.io
echo.

pause
