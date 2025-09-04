@echo off
echo [INFO] Starting CubicleRide Microservices Deployment...

REM Get infrastructure details
echo [INFO] Getting infrastructure details...

echo [INFO] Getting Container Registry details...
for /f "tokens=*" %%i in ('az acr list --resource-group rg-cubicleride --query "[0].name" -o tsv') do set ACR_NAME=%%i
for /f "tokens=*" %%i in ('az acr list --resource-group rg-cubicleride --query "[0].loginServer" -o tsv') do set ACR_LOGIN_SERVER=%%i

echo [INFO] Getting Container Apps Environment...
for /f "tokens=*" %%i in ('az containerapp env list --resource-group rg-cubicleride --query "[0].name" -o tsv') do set CONTAINERAPP_ENV=%%i

echo [INFO] Getting MySQL server details...
for /f "tokens=*" %%i in ('az mysql flexible-server list --resource-group rg-cubicleride --query "[0].fullyQualifiedDomainName" -o tsv') do set MYSQL_FQDN=%%i

echo [SUCCESS] Infrastructure Details:
echo   - Container Registry: %ACR_NAME%
echo   - ACR Login Server: %ACR_LOGIN_SERVER%
echo   - Container Apps Environment: %CONTAINERAPP_ENV%
echo   - MySQL Server: %MYSQL_FQDN%

REM Check if infrastructure details are available
if "%ACR_NAME%"=="" (
    echo [ERROR] Container Registry not found. Please ensure Azure infrastructure is deployed.
    exit /b 1
)

if "%CONTAINERAPP_ENV%"=="" (
    echo [ERROR] Container Apps Environment not found. Please ensure Azure infrastructure is deployed.
    exit /b 1
)

echo.
echo [INFO] Logging into Container Registry...
az acr login --name %ACR_NAME%

echo.
echo [INFO] Building and pushing Docker images...

REM Build and push Eureka Server
echo [INFO] Building Eureka Server...
cd eureka-server
docker build -t %ACR_LOGIN_SERVER%/eureka-server:latest .
docker push %ACR_LOGIN_SERVER%/eureka-server:latest
cd ..

REM Build and push API Gateway
echo [INFO] Building API Gateway...
cd api-gateway
docker build -t %ACR_LOGIN_SERVER%/api-gateway:latest .
docker push %ACR_LOGIN_SERVER%/api-gateway:latest
cd ..

REM Build and push Auth Service
echo [INFO] Building Auth Service...
cd auth-service
docker build -t %ACR_LOGIN_SERVER%/auth-service:latest .
docker push %ACR_LOGIN_SERVER%/auth-service:latest
cd ..

REM Build and push Employee Service
echo [INFO] Building Employee Service...
cd employee-service
docker build -t %ACR_LOGIN_SERVER%/employee-service:latest .
docker push %ACR_LOGIN_SERVER%/employee-service:latest
cd ..

REM Build and push Ride Service
echo [INFO] Building Ride Service...
cd ride-service
docker build -t %ACR_LOGIN_SERVER%/ride-service:latest .
docker push %ACR_LOGIN_SERVER%/ride-service:latest
cd ..

REM Build and push Admin Service
echo [INFO] Building Admin Service...
cd admin-service
docker build -t %ACR_LOGIN_SERVER%/admin-service:latest .
docker push %ACR_LOGIN_SERVER%/admin-service:latest
cd ..

echo [SUCCESS] All Docker images built and pushed successfully!

echo.
echo [INFO] Deploying microservices to Container Apps...

REM Deploy Eureka Server
echo [INFO] Deploying Eureka Server...
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

REM Wait for Eureka to be ready
echo [INFO] Waiting for Eureka Server to be ready...
timeout /t 30

REM Deploy API Gateway
echo [INFO] Deploying API Gateway...
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

REM Deploy Auth Service
echo [INFO] Deploying Auth Service...
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

REM Deploy Employee Service
echo [INFO] Deploying Employee Service...
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

REM Deploy Ride Service
echo [INFO] Deploying Ride Service...
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

REM Deploy Admin Service
echo [INFO] Deploying Admin Service...
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

echo.
echo [SUCCESS] All microservices deployed successfully!

echo.
echo [INFO] Getting deployment status...
az containerapp list --resource-group rg-cubicleride --query "[].{Name:name, Status:properties.provisioningState, URL:properties.configuration.ingress.fqdn}" --output table

echo.
echo [INFO] Getting API Gateway URL...
for /f "tokens=*" %%i in ('az containerapp show --name api-gateway --resource-group rg-cubicleride --query "properties.configuration.ingress.fqdn" -o tsv') do set API_GATEWAY_URL=%%i

echo [SUCCESS] Deployment completed!
echo.
echo Your CubicleRide Backend is now accessible at:
echo   API Gateway: https://%API_GATEWAY_URL%
echo   Eureka Dashboard: Check the Container Apps in Azure Portal for Eureka URL
echo.
echo To test the deployment:
echo   Health Check: https://%API_GATEWAY_URL%/actuator/health
echo   API Documentation: https://%API_GATEWAY_URL%/swagger-ui.html
echo.
echo [INFO] You can monitor the applications in Azure Portal under Container Apps

pause
