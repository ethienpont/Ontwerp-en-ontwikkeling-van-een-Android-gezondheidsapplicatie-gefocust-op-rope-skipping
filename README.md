# Locatie bestanden
## android_images
Gebruikte afbeeldingen in de android applicaties.

## data
Accelerometer data geordend in mappen volgens beweging en proefpersoon.
Validation_data en evaluation bevatten respectievelijk validatie en test data.
Gyro+acc+heart_data bevat metingen uitgevoerd met aanvulling van gyroscoop en hartslagsensor.
Trantitions bevat metingen waarbij twee verschillende sprongen na elkaar werden uitgevoerd.
Mistakes bevat metingen waarin een fout zich voordeed.

## data_collection_applications
Hierin zijn de smartwatch en smartphone app te vinden die gebruikt werden tijdens het data collectie proces.

## scriptie
Latex bestanden en de gebruikte afbeeldingen.

## models
Hierin zijn de TFLITE bestanden te vinden die de android applicatie gebruikt voor online bewegingsherkenning.

## notebooks
Deze map bevat alle notebook bestanden. Hierin werden alle experimenten uitgevoerd met betrekking tot bewegingsherkenning.

## WearableApp
Smartwatch applicatie

## healthRecommenderApp
Smartphone applicatie

# Gebruikershandleiding

1. Zorg ervoor dat smartphone en smartwatch verbonden zijn via bluetooth via de Wear OS app
2. 	Open android studio
	Connecteer de smartphone met een USB kabel
	Volg de instructies voor het enabelen van USB debugging (No devices - Troubleshoot device connections)
	Open Device file explorer (View - Tool Windows - Device File Explorer)
	In de Files folder (data/data/ugent.waves.healthrecommenderapp/Files) klik via de rechermuisknop op upload
	Navigeer naar de map models en selecteer model 1, 2, 3 en 4

	
	

