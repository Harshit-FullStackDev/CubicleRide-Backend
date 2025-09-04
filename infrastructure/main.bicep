@description('Location for all resources')
param location string = resourceGroup().location

@description('Environment name (e.g., dev, prod)')
param environmentName string = 'prod'

@description('Application name')
param applicationName string = 'cubicleride'

@description('MySQL administrator login')
@secure()
param mysqlAdminLogin string

@description('MySQL administrator password')
@secure()
param mysqlAdminPassword string

@description('JWT Secret for authentication')
@secure()
param jwtSecret string

@description('Mail username for SMTP')
@secure()
param mailUsername string

@description('Mail password for SMTP')
@secure()
param mailPassword string

// Variables
var resourcePrefix = '${applicationName}-${environmentName}'
var containerAppsEnvironmentName = '${resourcePrefix}-cae'
var logAnalyticsWorkspaceName = '${resourcePrefix}-logs'
var applicationInsightsName = '${resourcePrefix}-ai'
var containerRegistryName = replace('${resourcePrefix}acr', '-', '')
var keyVaultName = '${resourcePrefix}-kv'
var mysqlServerName = '${resourcePrefix}-mysql'
var eventHubNamespaceName = '${resourcePrefix}-eventhub'
var storageAccountName = replace('${resourcePrefix}storage', '-', '')

// Log Analytics Workspace
resource logAnalyticsWorkspace 'Microsoft.OperationalInsights/workspaces@2022-10-01' = {
  name: logAnalyticsWorkspaceName
  location: location
  properties: {
    sku: {
      name: 'PerGB2018'
    }
    retentionInDays: 30
  }
}

// Application Insights
resource applicationInsights 'Microsoft.Insights/components@2020-02-02' = {
  name: applicationInsightsName
  location: location
  kind: 'web'
  properties: {
    Application_Type: 'web'
    WorkspaceResourceId: logAnalyticsWorkspace.id
  }
}

// Container Registry
resource containerRegistry 'Microsoft.ContainerRegistry/registries@2023-07-01' = {
  name: containerRegistryName
  location: location
  sku: {
    name: 'Basic'
  }
  properties: {
    adminUserEnabled: true
  }
}

// Key Vault
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: keyVaultName
  location: location
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    tenantId: subscription().tenantId
    accessPolicies: []
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7
  }
}

// Store secrets in Key Vault
resource jwtSecretKV 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  name: 'jwt-secret'
  parent: keyVault
  properties: {
    value: jwtSecret
  }
}

resource mysqlPasswordKV 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  name: 'mysql-password'
  parent: keyVault
  properties: {
    value: mysqlAdminPassword
  }
}

resource mailUsernameKV 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  name: 'mail-username'
  parent: keyVault
  properties: {
    value: mailUsername
  }
}

resource mailPasswordKV 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  name: 'mail-password'
  parent: keyVault
  properties: {
    value: mailPassword
  }
}

// MySQL Flexible Server
resource mysqlServer 'Microsoft.DBforMySQL/flexibleServers@2023-06-30' = {
  name: mysqlServerName
  location: location
  sku: {
    name: 'Standard_B1ms'
    tier: 'Burstable'
  }
  properties: {
    administratorLogin: mysqlAdminLogin
    administratorLoginPassword: mysqlAdminPassword
    version: '8.0'
    storage: {
      storageSizeGB: 20
      iops: 360
      autoGrow: 'Enabled'
    }
    backup: {
      backupRetentionDays: 7
      geoRedundantBackup: 'Disabled'
    }
    highAvailability: {
      mode: 'Disabled'
    }
  }
}

// MySQL Databases
resource authDatabase 'Microsoft.DBforMySQL/flexibleServers/databases@2023-06-30' = {
  name: 'carpool_auth'
  parent: mysqlServer
  properties: {
    charset: 'utf8'
    collation: 'utf8_general_ci'
  }
}

resource employeeDatabase 'Microsoft.DBforMySQL/flexibleServers/databases@2023-06-30' = {
  name: 'carpool_employee'
  parent: mysqlServer
  properties: {
    charset: 'utf8'
    collation: 'utf8_general_ci'
  }
}

resource rideDatabase 'Microsoft.DBforMySQL/flexibleServers/databases@2023-06-30' = {
  name: 'carpool_ride'
  parent: mysqlServer
  properties: {
    charset: 'utf8'
    collation: 'utf8_general_ci'
  }
}

// MySQL Firewall Rule (Allow Azure Services)
resource mysqlFirewallRule 'Microsoft.DBforMySQL/flexibleServers/firewallRules@2023-06-30' = {
  name: 'AllowAzureServices'
  parent: mysqlServer
  properties: {
    startIpAddress: '0.0.0.0'
    endIpAddress: '0.0.0.0'
  }
}

// Event Hubs Namespace
resource eventHubNamespace 'Microsoft.EventHub/namespaces@2024-01-01' = {
  name: eventHubNamespaceName
  location: location
  sku: {
    name: 'Basic'
    tier: 'Basic'
  }
  properties: {
    kafkaEnabled: true
  }
}

// Event Hub for notifications
resource notificationEventHub 'Microsoft.EventHub/namespaces/eventhubs@2024-01-01' = {
  name: 'notifications'
  parent: eventHubNamespace
  properties: {
    messageRetentionInDays: 1
    partitionCount: 2
  }
}

// Storage Account
resource storageAccount 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: storageAccountName
  location: location
  sku: {
    name: 'Standard_LRS'
  }
  kind: 'StorageV2'
  properties: {
    supportsHttpsTrafficOnly: true
  }
}

// Container Apps Environment
resource containerAppsEnvironment 'Microsoft.App/managedEnvironments@2024-03-01' = {
  name: containerAppsEnvironmentName
  location: location
  properties: {
    appLogsConfiguration: {
      destination: 'log-analytics'
      logAnalyticsConfiguration: {
        customerId: logAnalyticsWorkspace.properties.customerId
        sharedKey: logAnalyticsWorkspace.listKeys().primarySharedKey
      }
    }
  }
}

// Outputs
output containerAppsEnvironmentId string = containerAppsEnvironment.id
output containerRegistryLoginServer string = containerRegistry.properties.loginServer
output keyVaultUri string = keyVault.properties.vaultUri
output mysqlServerFQDN string = mysqlServer.properties.fullyQualifiedDomainName
@description('Event Hub namespace name for connection string retrieval')
output eventHubNamespaceName string = eventHubNamespace.name
output logAnalyticsWorkspaceId string = logAnalyticsWorkspace.id
@description('Application Insights connection string for monitoring')
output applicationInsightsConnectionString string = applicationInsights.properties.ConnectionString
