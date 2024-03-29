module "reform-notifications-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = var.product
  component          = var.component
  name               = "${var.product}-${var.component}"
  location           = var.location_db
  env                = var.env
  database_name      = "notifications"
  postgresql_user    = "notifier"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

module "reform-notifications-staging-db" {
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.component}-staging"
  location           = var.location_db
  env                = var.env
  database_name      = "notifications"
  postgresql_user    = "notifier"
  postgresql_version = "11"
  sku_name           = "GP_Gen5_2"
  sku_tier           = "GeneralPurpose"
  common_tags        = var.common_tags
  subscription       = var.subscription
}

data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "reform-scan-${var.env}"
  resource_group_name = "reform-scan-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

# region DB secrets
# names have to be in such format as library hardcodes them for migration url build

resource "azurerm_key_vault_secret" "db_user" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-USER"
  value        = module.reform-notifications-db.user_name
}

resource "azurerm_key_vault_secret" "db_password" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.reform-notifications-db.postgresql_password
}

resource "azurerm_key_vault_secret" "db_host" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.reform-notifications-db.host_name
}

resource "azurerm_key_vault_secret" "db_port" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.reform-notifications-db.postgresql_listen_port
}

resource "azurerm_key_vault_secret" "db_database" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.reform-notifications-db.postgresql_database
}

# endregion

# region staging DB secrets

resource "azurerm_key_vault_secret" "staging_db_user" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-staging-postgres-user"
  value        = module.reform-notifications-staging-db.user_name
}

resource "azurerm_key_vault_secret" "staging_db_password" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-staging-postgres-pass"
  value        = module.reform-notifications-staging-db.postgresql_password
}

resource "azurerm_key_vault_secret" "staging_db_host" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-staging-postgres-host"
  value        = module.reform-notifications-staging-db.host_name
}

resource "azurerm_key_vault_secret" "staging_db_port" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-staging-postgres-port"
  value        = module.reform-notifications-staging-db.postgresql_listen_port
}

resource "azurerm_key_vault_secret" "staging_db_database" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-staging-postgres-database"
  value        = module.reform-notifications-staging-db.postgresql_database
}

# endregion

# region Copy secrets from BulkScan

data "azurerm_key_vault_secret" "s2s_secret" {
  key_vault_id = data.azurerm_key_vault.s2s_key_vault.id
  name         = "microservicekey-reform-scan-notification-tests"
}

resource "azurerm_key_vault_secret" "test_s2s_secret" {
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "test-s2s-secret"
  value        = data.azurerm_key_vault_secret.s2s_secret.value
}

# endregion

# Create secrets for Launch darkly - values manually populated
data "azurerm_key_vault_secret" "launch_darkly_sdk_key" {
  name         = "launch-darkly-sdk-key"
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}

data "azurerm_key_vault_secret" "launch_darkly_offline_mode" {
  name         = "launch-darkly-offline-mode"
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
}
