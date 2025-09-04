# 🚗 CubicleRide Backend

A microservices-based carpooling application built with Spring Boot and deployed on Azure Container Apps.

## 🏗️ Architecture

- **API Gateway** (8080) - Request routing and authentication
- **Eureka Server** (8761) - Service discovery
- **Auth Service** (8081) - User authentication and JWT management
- **Employee Service** (8082) - Employee management and notifications
- **Ride Service** (8083) - Ride booking and real-time tracking
- **Admin Service** (8084) - Administrative operations

## 🚀 Quick Start

### Prerequisites
- Java 17
- Maven 3.6+
- Docker Desktop
- Azure CLI (for deployment)

### Local Development
```bash
# Build all services
mvn clean package

# Run services (in order)
cd eureka-server && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd auth-service && mvn spring-boot:run &
cd employee-service && mvn spring-boot:run &
cd ride-service && mvn spring-boot:run &
cd admin-service && mvn spring-boot:run &
```

### Azure Deployment
```bash
# Deploy infrastructure
az deployment group create --resource-group rg-cubicleride --template-file infrastructure/main.bicep

# Deploy applications
./scripts/deploy.sh  # Linux/Mac
scripts\deploy.bat   # Windows
```

## 🔧 Configuration

Environment variables (see `.env.example`):
- `JWT_SECRET` - JWT signing key
- `DATASOURCE_URL` - MySQL connection string
- `DATASOURCE_PASSWORD` - Database password
- `MAIL_USERNAME` / `MAIL_PASSWORD` - SMTP credentials
- `KAFKA_BOOTSTRAP_SERVERS` - Event Hub endpoint

## 🌐 API Endpoints

### Authentication
- `POST /auth/register` - User registration
- `POST /auth/verify-otp` - Email verification
- `POST /auth/login` - User login

### Employee Management
- `GET /employee/profile` - Get user profile
- `PUT /employee/profile` - Update profile
- `POST /employee/vehicle` - Add vehicle

### Ride Management
- `POST /ride/create` - Create ride
- `GET /ride/search` - Search available rides
- `POST /ride/book` - Book a ride

## 🔒 Security Features

- JWT-based authentication
- Role-based access control (ADMIN/EMPLOYEE)
- Email verification with OTP
- Secure password hashing
- API Gateway request filtering

## 📊 Tech Stack

- **Backend**: Spring Boot, Spring Cloud Gateway, Spring Security
- **Database**: MySQL with Spring Data JPA
- **Messaging**: Kafka (Azure Event Hubs)
- **Service Discovery**: Netflix Eureka
- **Containerization**: Docker
- **Cloud**: Azure Container Apps, Azure Container Registry
- **Monitoring**: Spring Boot Actuator

## 📝 License

This project is licensed under the MIT License.
