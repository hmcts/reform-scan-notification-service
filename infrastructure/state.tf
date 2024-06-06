terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.106.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.47.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
