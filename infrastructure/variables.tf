variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map
}

variable "location_db" {
  type    = string
  default = "UK South"
}

variable "deployment_namespace" {
  default = ""
}

variable "ilbIp" {}
