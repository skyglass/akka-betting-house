#
# Creates a resource group for Event Booking in your Azure account.
#
resource "azurerm_resource_group" "bettinghouse" {
  name     = var.app_name
  location = var.location
}
