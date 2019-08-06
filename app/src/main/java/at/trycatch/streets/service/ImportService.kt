package at.trycatch.streets.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.util.Log
import at.trycatch.streets.Constants
import at.trycatch.streets.R
import at.trycatch.streets.data.CityProvider
import at.trycatch.streets.data.DistrictProvider
import at.trycatch.streets.data.Settings
import at.trycatch.streets.data.StreetProvider
import at.trycatch.streets.model.City
import at.trycatch.streets.model.District
import at.trycatch.streets.model.StreetCsvLine
import at.trycatch.streets.model.StreetToDistrict
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportService : IntentService("ImportService") {

    private lateinit var cityProvider: CityProvider
    private lateinit var districtProvider: DistrictProvider
    private lateinit var streetProvider: StreetProvider
    private lateinit var settings: Settings

    override fun onCreate() {
        super.onCreate()
        cityProvider = CityProvider(applicationContext)
        districtProvider = DistrictProvider(applicationContext)
        streetProvider = StreetProvider(applicationContext)
        settings = Settings(applicationContext)
    }

    override fun onHandleIntent(intent: Intent?) {
        handleImport()
    }

    private fun handleImport() {
        val timestamp = System.currentTimeMillis()
        Log.d(TAG, "Starting import.")

        getCities().forEach {
            handleCity(it)
        }

        Log.d(TAG, "Import took " + (System.currentTimeMillis() - timestamp) + " millis.")
    }

    private fun handleCity(city: City) {
        Log.d(TAG, "Handling `${city.displayName}`…")

        if (city.version <= settings.getInstalledVersion(city.id)) {
            Log.d(TAG, "Skipping update, latest version is installed.")
            return
        }

        settings.setInstalledVersion(city.id, city.version)

        // Add the city.
        cityProvider.addCity(city)

        // Wipe, then store districts.
        districtProvider.removeAllDistricts(city)
        val districts = getDistricts(city)
        districts.forEach {
            districtProvider.addDistrict(it)
        }

        // Wipe all city-district connections.
        districtProvider.removeAllStreetToDistrictMappings(city)

        // Flag streets as deletable
        streetProvider.flagStreetsForDelection(city)

        // Insert everything
        getStreetCsvLines().forEach {
            val streetToDistrict = StreetToDistrict().apply {
                streetId = it.streetId
                districtId = it.districtId
            }
            districtProvider.addStreetToDistrictMapping(streetToDistrict)
            streetProvider.insertOrUpdateStreet(city, it)
        }

        // Remove flagged streets
        streetProvider.removeFlaggedStreets(city)

        sendBroadcast(Intent(Constants.Broadcasts.ACTION_UPDATE_DONE))
    }

    private fun getStreetCsvLines(): List<StreetCsvLine> {
        val result = mutableListOf<StreetCsvLine>()
        val resource = resources.openRawResource(R.raw.graz_streets)
        val bufferedReader = BufferedReader(InputStreamReader(resource))
        while (bufferedReader.ready()) {
            result.add(StreetCsvLine.getFromCsv(bufferedReader.readLine()))
        }
        return result
    }

    private fun getCities(): List<City> {
        val result = mutableListOf<City>()
        val resource = resources.openRawResource(R.raw.cities)
        val bufferedReader = BufferedReader(InputStreamReader(resource))
        while (bufferedReader.ready()) {
            result.add(City.createFromCsv(bufferedReader.readLine()))
        }
        return result
    }

    private fun getDistricts(city: City): List<District> {
        val result = mutableListOf<District>()
        // TODO: Fetch the file name via reflection
        val resource = resources.openRawResource(R.raw.graz_districts)
        val bufferedReader = BufferedReader(InputStreamReader(resource))
        while (bufferedReader.ready()) {
            result.add(District.createFromCsv(city.id, bufferedReader.readLine()))
        }
        return result
    }

    companion object {

        private const val TAG = "ImportService"

        @JvmStatic
        fun startImport(context: Context) {
            try {
                context.startService(Intent(context, ImportService::class.java))
            } catch (e: IllegalStateException) {
                // App not in the foreground.
            }
        }

    }
}
