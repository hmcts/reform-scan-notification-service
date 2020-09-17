variable "product" {}

variable "component" {}

variable "location" {
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "common_tags" {
  type = map(string)
}

variable "location_db" {
  default = "UK South"
}

variable "deployment_namespace" {
  default = ""
}

variable "ilbIp" {}
