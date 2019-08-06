package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.data.room.AppDatabaseInstance
import at.trycatch.streets.model.City

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class CityProvider(private val context: Context) {

    private val db = AppDatabaseInstance.getInstance(context)

    fun addCity(city: City) {
        db.database.getCityDao().insert(city)
    }

    fun removeAllCities() {
        db.database.getCityDao().deleteAll()
    }

}
