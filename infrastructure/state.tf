terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "=3.38.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.32.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
