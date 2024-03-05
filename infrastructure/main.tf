data "azurerm_key_vault" "reform_scan_key_vault" {
  name                = "reform-scan-${var.env}"
  resource_group_name = "reform-scan-${var.env}"
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = "s2s-${var.env}"
  resource_group_name = "rpe-service-auth-provider-${var.env}"
}

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