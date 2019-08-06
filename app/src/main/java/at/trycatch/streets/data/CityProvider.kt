package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.data.room.AppDatabaseInstance
import at.trycatch.streets.model.City
import at.trycatch.streets.model.CityWithProgress
import org.jetbrains.anko.doAsync

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

    fun getCityWithProgress(cityId: String, callback: (city: CityWithProgress?) -> Unit) {
        doAsync {
            val result = db.database.getCityDao().findById(cityId)
            result?.apply {
                callback.invoke(CityWithProgress(this).apply {
                    solvedStreets = db.database.getDistrictDao().getNumberOfSolvedStreets(cityId)
                    totalStreets = db.database.getDistrictDao().getTotalNumberOfStreets(cityId)
                })
            }
        }
    }

}
