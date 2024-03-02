module "reform-notifications-db" {
  count              = var.deploy_single_server_db
  source             = "git@github.com:hmcts/cnp-module-postgres?ref=master"
  product            = "${var.product}-${var.component}"
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
  count        = var.deploy_single_server_db
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-USER"
  value        = module.reform-notifications-db.user_name
}

resource "azurerm_key_vault_secret" "db_password" {
  count        = var.deploy_single_server_db
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-PASS"
  value        = module.reform-notifications-db.postgresql_password
}

resource "azurerm_key_vault_secret" "db_host" {
  count        = var.deploy_single_server_db
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-HOST"
  value        = module.reform-notifications-db.host_name
}

resource "azurerm_key_vault_secret" "db_port" {
  count        = var.deploy_single_server_db
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-PORT"
  value        = module.reform-notifications-db.postgresql_listen_port
}

resource "azurerm_key_vault_secret" "db_database" {
  count        = var.deploy_single_server_db
  key_vault_id = data.azurerm_key_vault.reform_scan_key_vault.id
  name         = "${var.component}-POSTGRES-DATABASE"
  value        = module.reform-notifications-db.postgresql_database
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