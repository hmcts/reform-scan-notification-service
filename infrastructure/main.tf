provider "azurerm" {
  version = "=1.42.0"
}

locals {
  is_preview  = "${(var.env == "preview" || var.env == "spreview")}"
  local_env   = "${local.is_preview ? "aat" : var.env}"

  s2s_rg  = "rpe-service-auth-provider-${local.local_env}"
  s2s_url = "http://${local.s2s_rg}.service.core-compute-${local.local_env}.internal"

  core_app_settings = {
    S2S_URL = "${local.s2s_url}"
  }
}

module "reform-notifications-db" {
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
