# Postgres 15 flexible servers
locals {
  db_host_name = "${var.product}-${var.component}-flexible-db-v15"
  db_name      = "notifications"
  pg_user      = "notifier"
}

module "postgresql" {
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }

  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = local.db_host_name
  product              = var.product
  component            = var.component
  location             = var.location_db
  env                  = var.env
  pgsql_admin_username = local.pg_user
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  common_tags   = var.common_tags
  business_area = "cft"
  pgsql_version = "15"
  subnet_suffix = "expanded"

  admin_user_object_id = var.jenkins_AAD_objectId
}

module "postgresql-staging" {
  count = var.env == "aat" ? 1 : 0
  providers = {
    azurerm.postgres_network = azurerm.postgres_network
  }
  source               = "git@github.com:hmcts/terraform-module-postgresql-flexible?ref=master"
  name                 = "${var.product}-${var.component}-flexible-v15-staging"
  product              = var.product
  component            = var.component
  location             = var.location_db
  env                  = "aat"
  pgsql_admin_username = local.pg_user
  pgsql_databases = [
    {
      name : local.db_name
    }
  ]
  common_tags   = var.common_tags
  business_area = "cft"
  pgsql_version = "15"
  subnet_suffix = "expanded"

  admin_user_object_id = var.jenkins_AAD_objectId
}
