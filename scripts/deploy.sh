#!/bin/bash

# Azure Deployment Script for CubicleRide Backend
# This script deploys the infrastructure and applications to Azure

set -e

# Configuration
RESOURCE_GROUP="rg-cubicleride"
LOCATION="eastus"
SUBSCRIPTION_ID="${AZURE_SUBSCRIPTION_ID:-$(az account show --query id -o tsv)}"
APPLICATION_NAME="cubicleride"
ENVIRONMENT_NAME="prod"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

echo_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

echo_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

echo_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if required tools are installed
check_prerequisites() {
    echo_info "Checking prerequisites..."
    
    if ! command -v az &> /dev/null; then
        echo_error "Azure CLI is not installed. Please install it first."
        exit 1
    fi
    
    if ! command -v mvn &> /dev/null; then
        echo_error "Maven is not installed. Please install it first."
        exit 1
    fi
    
    if ! command -v docker &> /dev/null; then
        echo_error "Docker is not installed. Please install it first."
        exit 1
    fi
    
    echo_success "All prerequisites are available"
}

# Login to Azure and set subscription
azure_login() {
    echo_info "Logging into Azure..."
    
    # Check if already logged in
    if ! az account show &> /dev/null; then
        az login
    fi
    
    # Set the subscription
    az account set --subscription "$SUBSCRIPTION_ID"
    echo_success "Using Azure subscription: $SUBSCRIPTION_ID"
}

# Create resource group
create_resource_group() {
    echo_info "Creating resource group: $RESOURCE_GROUP in $LOCATION..."
    
    if az group show --name "$RESOURCE_GROUP" &> /dev/null; then
        echo_warning "Resource group $RESOURCE_GROUP already exists"
    else
        az group create --name "$RESOURCE_GROUP" --location "$LOCATION"
        echo_success "Resource group created: $RESOURCE_GROUP"
    fi
}

# Build Java applications
build_applications() {
    echo_info "Building Java applications..."
    
    # Clean and build all modules
    mvn clean package -DskipTests -B
    
    if [ $? -eq 0 ]; then
        echo_success "Java applications built successfully"
    else
        echo_error "Failed to build Java applications"
        exit 1
    fi
}

# Deploy infrastructure
deploy_infrastructure() {
    echo_info "Deploying Azure infrastructure..."
    
    # Prompt for sensitive values if not set as environment variables
    if [ -z "$MYSQL_ADMIN_LOGIN" ]; then
        read -p "Enter MySQL admin username: " MYSQL_ADMIN_LOGIN
    fi
    
    if [ -z "$MYSQL_ADMIN_PASSWORD" ]; then
        read -s -p "Enter MySQL admin password: " MYSQL_ADMIN_PASSWORD
        echo
    fi
    
    if [ -z "$JWT_SECRET" ]; then
        read -s -p "Enter JWT secret (minimum 32 characters): " JWT_SECRET
        echo
    fi
    
    if [ -z "$MAIL_USERNAME" ]; then
        read -p "Enter email username for SMTP: " MAIL_USERNAME
    fi
    
    if [ -z "$MAIL_PASSWORD" ]; then
        read -s -p "Enter email password for SMTP: " MAIL_PASSWORD
        echo
    fi
    
    # Deploy main infrastructure
    DEPLOYMENT_OUTPUT=$(az deployment group create \
        --resource-group "$RESOURCE_GROUP" \
        --template-file infrastructure/main.bicep \
        --parameters \
            applicationName="$APPLICATION_NAME" \
            environmentName="$ENVIRONMENT_NAME" \
            mysqlAdminLogin="$MYSQL_ADMIN_LOGIN" \
            mysqlAdminPassword="$MYSQL_ADMIN_PASSWORD" \
            jwtSecret="$JWT_SECRET" \
            mailUsername="$MAIL_USERNAME" \
            mailPassword="$MAIL_PASSWORD" \
        --query 'properties.outputs' -o json)
    
    if [ $? -eq 0 ]; then
        echo_success "Infrastructure deployed successfully"
        
        # Extract outputs
        export CONTAINER_APPS_ENVIRONMENT_ID=$(echo "$DEPLOYMENT_OUTPUT" | jq -r '.containerAppsEnvironmentId.value')
        export CONTAINER_REGISTRY_LOGIN_SERVER=$(echo "$DEPLOYMENT_OUTPUT" | jq -r '.containerRegistryLoginServer.value')
        export KEY_VAULT_URI=$(echo "$DEPLOYMENT_OUTPUT" | jq -r '.keyVaultUri.value')
        export MYSQL_SERVER_FQDN=$(echo "$DEPLOYMENT_OUTPUT" | jq -r '.mysqlServerFQDN.value')
        
        # Get Event Hub connection string separately for security
        EVENT_HUB_NAMESPACE_NAME=$(echo "$DEPLOYMENT_OUTPUT" | jq -r '.eventHubNamespaceName.value')
        export EVENT_HUB_CONNECTION_STRING=$(az eventhubs namespace authorization-rule keys list \
            --resource-group "$RESOURCE_GROUP" \
            --namespace-name "$EVENT_HUB_NAMESPACE_NAME" \
            --name RootManageSharedAccessKey \
            --query primaryConnectionString -o tsv)
        
        echo_info "Infrastructure outputs saved to environment variables"
    else
        echo_error "Failed to deploy infrastructure"
        exit 1
    fi
}

# Build and push Docker images
build_and_push_images() {
    echo_info "Building and pushing Docker images..."
    
    # Get ACR credentials
    ACR_USERNAME=$(az acr credential show --name "${APPLICATION_NAME}${ENVIRONMENT_NAME}acr" --query username -o tsv)
    ACR_PASSWORD=$(az acr credential show --name "${APPLICATION_NAME}${ENVIRONMENT_NAME}acr" --query passwords[0].value -o tsv)
    
    # Login to ACR
    echo "$ACR_PASSWORD" | docker login "$CONTAINER_REGISTRY_LOGIN_SERVER" --username "$ACR_USERNAME" --password-stdin
    
    # Build and push each service
    SERVICES=("eureka-server" "api-gateway" "auth-service" "employee-service" "ride-service" "admin-service")
    
    for SERVICE in "${SERVICES[@]}"; do
        echo_info "Building and pushing $SERVICE..."
        
        cd "$SERVICE"
        
        # Build Docker image
        docker build -t "$CONTAINER_REGISTRY_LOGIN_SERVER/cubicleride/$SERVICE:latest" .
        docker build -t "$CONTAINER_REGISTRY_LOGIN_SERVER/cubicleride/$SERVICE:$(git rev-parse --short HEAD)" .
        
        # Push images
        docker push "$CONTAINER_REGISTRY_LOGIN_SERVER/cubicleride/$SERVICE:latest"
        docker push "$CONTAINER_REGISTRY_LOGIN_SERVER/cubicleride/$SERVICE:$(git rev-parse --short HEAD)"
        
        cd ..
        
        echo_success "$SERVICE image pushed successfully"
    done
}

# Deploy container apps
deploy_container_apps() {
    echo_info "Deploying container applications..."
    
    az deployment group create \
        --resource-group "$RESOURCE_GROUP" \
        --template-file infrastructure/container-apps.bicep \
        --parameters \
            containerAppsEnvironmentId="$CONTAINER_APPS_ENVIRONMENT_ID" \
            containerRegistryLoginServer="$CONTAINER_REGISTRY_LOGIN_SERVER" \
            containerRegistryUsername="$ACR_USERNAME" \
            containerRegistryPassword="$ACR_PASSWORD" \
            applicationName="$APPLICATION_NAME" \
            environmentName="$ENVIRONMENT_NAME" \
            mysqlServerFQDN="$MYSQL_SERVER_FQDN" \
            mysqlUsername="$MYSQL_ADMIN_LOGIN" \
            eventHubConnectionString="$EVENT_HUB_CONNECTION_STRING" \
            jwtSecret="$JWT_SECRET" \
            mailUsername="$MAIL_USERNAME" \
            mailPassword="$MAIL_PASSWORD" \
            mysqlPassword="$MYSQL_ADMIN_PASSWORD" \
            imageTag="latest"
    
    if [ $? -eq 0 ]; then
        echo_success "Container apps deployed successfully"
    else
        echo_error "Failed to deploy container apps"
        exit 1
    fi
}

# Get deployment information
get_deployment_info() {
    echo_info "Getting deployment information..."
    
    # Get API Gateway URL
    API_GATEWAY_URL=$(az containerapp show --name api-gateway --resource-group "$RESOURCE_GROUP" --query properties.configuration.ingress.fqdn -o tsv)
    
    echo
    echo_success "🚀 Deployment completed successfully!"
    echo
    echo -e "${GREEN}Application URLs:${NC}"
    echo -e "  API Gateway: https://$API_GATEWAY_URL"
    echo -e "  Eureka Server: Internal only (eureka-server:8761)"
    echo
    echo -e "${BLUE}Useful Commands:${NC}"
    echo -e "  View logs: az containerapp logs show --name <service-name> --resource-group $RESOURCE_GROUP --follow"
    echo -e "  Scale app: az containerapp update --name <service-name> --resource-group $RESOURCE_GROUP --min-replicas 0 --max-replicas 5"
    echo -e "  Get status: az containerapp list --resource-group $RESOURCE_GROUP --output table"
    echo
}

# Main execution
main() {
    echo_info "Starting CubicleRide Backend deployment to Azure..."
    echo
    
    check_prerequisites
    azure_login
    create_resource_group
    build_applications
    deploy_infrastructure
    build_and_push_images
    deploy_container_apps
    get_deployment_info
    
    echo_success "🎉 All done! Your CubicleRide Backend is now running on Azure Container Apps!"
}

# Execute main function
main "$@"
