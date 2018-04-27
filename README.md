# ARMSlotWebConfig
To prevent the copying of settings from the production app, just add an empty **siteConfig** object in the slot properties. e.g.
 
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


SO link: https://stackoverflow.com/questions/50024342/create-azure-web-app-slot-from-arm-template-without-copying-original-web-app-con/50034319?noredirect=1#comment87109053_50034319
