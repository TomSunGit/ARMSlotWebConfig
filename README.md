# ARMSlotWebConfig
To prevent the copying of settings from the production app, just add an empty siteConfig object in the slot properties. e.g.
 
        {
          "apiVersion": "2015-08-01",
          "type": "slots",
          "name": "maintenance",
          "location": "[resourceGroup().location]",
          "dependsOn": [
            "[resourceId('Microsoft.Web/Sites/', variables('webSiteName'))]"
          ],
          "properties": {
            "siteConfig": { }
          }
        }
