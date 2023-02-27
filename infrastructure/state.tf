terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "3.44.1"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.35.0"
    }
    random = {
      source = "hashicorp/random"
    }
  }
}
