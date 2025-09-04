@description('Container Apps Environment ID')
param containerAppsEnvironmentId string

@description('Container Registry Login Server')
param containerRegistryLoginServer string

@description('Container Registry Username')
@secure()
param containerRegistryUsername string

@description('Container Registry Password')
@secure()
param containerRegistryPassword string

@description('Application name')
param applicationName string = 'cubicleride'

@description('Environment name')
param environmentName string = 'prod'

@description('MySQL Server FQDN')
param mysqlServerFQDN string

@description('MySQL Username')
param mysqlUsername string

@description('Event Hub Connection String')
@secure()
param eventHubConnectionString string

@description('JWT Secret')
@secure()
param jwtSecret string

@description('Mail Username')
@secure()
param mailUsername string

@description('Mail Password')
@secure()
param mailPassword string

@description('MySQL Password')
@secure()
param mysqlPassword string

@description('Image tag')
param imageTag string = 'latest'

var resourcePrefix = '${applicationName}-${environmentName}'

// Eureka Server
resource eurekaServer 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'eureka-server'
  location: resourceGroup().location
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: false
        targetPort: 8761
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'eureka-server'
          image: '${containerRegistryLoginServer}/cubicleride/eureka-server:${imageTag}'
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8761'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8761
              }
              initialDelaySeconds: 60
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8761
              }
              initialDelaySeconds: 30
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 2
      }
    }
  }
}

// API Gateway
resource apiGateway 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'api-gateway'
  location: resourceGroup().location
  dependsOn: [
    eurekaServer
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: true
        targetPort: 8080
        allowInsecure: false
        traffic: [
          {
            weight: 100
            latestRevision: true
          }
        ]
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'api-gateway'
          image: '${containerRegistryLoginServer}/cubicleride/api-gateway:${imageTag}'
          resources: {
            cpu: json('0.5')
            memory: '1.0Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8080'
            }
            {
              name: 'EUREKA_DEFAULT_ZONE'
              value: 'http://eureka-server:8761/eureka'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8080
              }
              initialDelaySeconds: 60
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8080
              }
              initialDelaySeconds: 30
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 5
        rules: [
          {
            name: 'http-scaling-rule'
            http: {
              metadata: {
                concurrentRequests: '30'
              }
            }
          }
        ]
      }
    }
  }
}

// Auth Service
resource authService 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'auth-service'
  location: resourceGroup().location
  dependsOn: [
    eurekaServer
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: false
        targetPort: 8081
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
        {
          name: 'mysql-password'
          value: mysqlPassword
        }
        {
          name: 'mail-username'
          value: mailUsername
        }
        {
          name: 'mail-password'
          value: mailPassword
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'auth-service'
          image: '${containerRegistryLoginServer}/cubicleride/auth-service:${imageTag}'
          resources: {
            cpu: json('0.5')
            memory: '1.0Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8081'
            }
            {
              name: 'EUREKA_DEFAULT_ZONE'
              value: 'http://eureka-server:8761/eureka'
            }
            {
              name: 'DATASOURCE_URL'
              value: 'jdbc:mysql://${mysqlServerFQDN}:3306/carpool_auth?useSSL=true&requireSSL=false&serverTimezone=UTC'
            }
            {
              name: 'DATASOURCE_USERNAME'
              value: mysqlUsername
            }
            {
              name: 'DATASOURCE_PASSWORD'
              secretRef: 'mysql-password'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
            {
              name: 'MAIL_USERNAME'
              secretRef: 'mail-username'
            }
            {
              name: 'MAIL_PASSWORD'
              secretRef: 'mail-password'
            }
            {
              name: 'JPA_DDL_AUTO'
              value: 'update'
            }
            {
              name: 'JPA_SHOW_SQL'
              value: 'false'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8081
              }
              initialDelaySeconds: 90
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8081
              }
              initialDelaySeconds: 45
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 3
      }
    }
  }
}

// Employee Service
resource employeeService 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'employee-service'
  location: resourceGroup().location
  dependsOn: [
    eurekaServer
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: false
        targetPort: 8082
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
        {
          name: 'mysql-password'
          value: mysqlPassword
        }
        {
          name: 'eventhub-connection-string'
          value: eventHubConnectionString
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'employee-service'
          image: '${containerRegistryLoginServer}/cubicleride/employee-service:${imageTag}'
          resources: {
            cpu: json('0.5')
            memory: '1.0Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8082'
            }
            {
              name: 'EUREKA_DEFAULT_ZONE'
              value: 'http://eureka-server:8761/eureka'
            }
            {
              name: 'DATASOURCE_URL'
              value: 'jdbc:mysql://${mysqlServerFQDN}:3306/carpool_employee?useSSL=true&requireSSL=false&serverTimezone=UTC'
            }
            {
              name: 'DATASOURCE_USERNAME'
              value: mysqlUsername
            }
            {
              name: 'DATASOURCE_PASSWORD'
              secretRef: 'mysql-password'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
            {
              name: 'KAFKA_BOOTSTRAP_SERVERS'
              value: '${split(eventHubConnectionString, ';')[0]}.servicebus.windows.net:9093'
            }
            {
              name: 'JPA_DDL_AUTO'
              value: 'update'
            }
            {
              name: 'JPA_SHOW_SQL'
              value: 'false'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8082
              }
              initialDelaySeconds: 90
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8082
              }
              initialDelaySeconds: 45
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 3
      }
    }
  }
}

// Ride Service
resource rideService 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'ride-service'
  location: resourceGroup().location
  dependsOn: [
    eurekaServer
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: false
        targetPort: 8083
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
        {
          name: 'mysql-password'
          value: mysqlPassword
        }
        {
          name: 'eventhub-connection-string'
          value: eventHubConnectionString
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'ride-service'
          image: '${containerRegistryLoginServer}/cubicleride/ride-service:${imageTag}'
          resources: {
            cpu: json('0.5')
            memory: '1.0Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8083'
            }
            {
              name: 'EUREKA_DEFAULT_ZONE'
              value: 'http://eureka-server:8761/eureka'
            }
            {
              name: 'DATASOURCE_URL'
              value: 'jdbc:mysql://${mysqlServerFQDN}:3306/carpool_ride?useSSL=true&requireSSL=false&serverTimezone=UTC'
            }
            {
              name: 'DATASOURCE_USERNAME'
              value: mysqlUsername
            }
            {
              name: 'DATASOURCE_PASSWORD'
              secretRef: 'mysql-password'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
            {
              name: 'KAFKA_BOOTSTRAP_SERVERS'
              value: '${split(eventHubConnectionString, ';')[0]}.servicebus.windows.net:9093'
            }
            {
              name: 'JPA_DDL_AUTO'
              value: 'update'
            }
            {
              name: 'JPA_SHOW_SQL'
              value: 'false'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8083
              }
              initialDelaySeconds: 90
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8083
              }
              initialDelaySeconds: 45
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 1
        maxReplicas: 4
      }
    }
  }
}

// Admin Service
resource adminService 'Microsoft.App/containerApps@2024-03-01' = {
  name: 'admin-service'
  location: resourceGroup().location
  dependsOn: [
    eurekaServer
  ]
  properties: {
    managedEnvironmentId: containerAppsEnvironmentId
    configuration: {
      activeRevisionsMode: 'Single'
      ingress: {
        external: false
        targetPort: 8084
        allowInsecure: false
      }
      registries: [
        {
          server: containerRegistryLoginServer
          username: containerRegistryUsername
          passwordSecretRef: 'registry-password'
        }
      ]
      secrets: [
        {
          name: 'registry-password'
          value: containerRegistryPassword
        }
        {
          name: 'jwt-secret'
          value: jwtSecret
        }
      ]
    }
    template: {
      containers: [
        {
          name: 'admin-service'
          image: '${containerRegistryLoginServer}/cubicleride/admin-service:${imageTag}'
          resources: {
            cpu: json('0.25')
            memory: '0.5Gi'
          }
          env: [
            {
              name: 'SPRING_PROFILES_ACTIVE'
              value: 'prod'
            }
            {
              name: 'SERVER_PORT'
              value: '8084'
            }
            {
              name: 'EUREKA_DEFAULT_ZONE'
              value: 'http://eureka-server:8761/eureka'
            }
            {
              name: 'JWT_SECRET'
              secretRef: 'jwt-secret'
            }
          ]
          probes: [
            {
              type: 'Liveness'
              httpGet: {
                path: '/actuator/health/liveness'
                port: 8084
              }
              initialDelaySeconds: 60
              periodSeconds: 30
            }
            {
              type: 'Readiness'
              httpGet: {
                path: '/actuator/health/readiness'
                port: 8084
              }
              initialDelaySeconds: 30
              periodSeconds: 10
            }
          ]
        }
      ]
      scale: {
        minReplicas: 0
        maxReplicas: 2
      }
    }
  }
}

// Outputs
output apiGatewayUrl string = 'https://${apiGateway.properties.configuration.ingress.fqdn}'
output eurekaServerUrl string = 'http://eureka-server.internal.${containerAppsEnvironment.properties.defaultDomain}'

var containerAppsEnvironment = reference(containerAppsEnvironmentId, '2024-03-01')
