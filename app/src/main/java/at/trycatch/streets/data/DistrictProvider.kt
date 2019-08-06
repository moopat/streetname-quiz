package at.trycatch.streets.data

import android.content.Context
import androidx.lifecycle.LiveData
import at.trycatch.streets.data.room.AppDatabaseInstance
import at.trycatch.streets.model.City
import at.trycatch.streets.model.District
import at.trycatch.streets.model.DistrictWithProgress
import at.trycatch.streets.model.StreetToDistrict
import org.jetbrains.anko.doAsync

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DistrictProvider(context: Context) {

    private val db = AppDatabaseInstance.getInstance(context)

    fun addDistrict(district: District) {
        db.database.getDistrictDao().insert(district)
    }

    fun addStreetToDistrictMapping(streetToDistrict: StreetToDistrict) {
        db.database.getStreetToDistrictDao().insert(streetToDistrict)
    }

    fun removeAllDistricts(city: City) {
        db.database.getDistrictDao().deleteAll(city.id)
    }

    fun removeAllStreetToDistrictMappings(city: City) {
        val query = "${city.id}%"
        db.database.getStreetToDistrictDao().deleteAll(query)
    }

    fun getAllLive(cityId: String): LiveData<List<District>> {
        return db.database.getDistrictDao().findAllLive(cityId)
    }

    fun getAllDistrictsWithProgress(cityId: String, callback: (districts: List<DistrictWithProgress>) -> Unit) {
        doAsync {
            val result = mutableListOf<DistrictWithProgress>()
            val districts = db.database.getDistrictDao().findAll(cityId)
            districts.forEach {
                result.add(DistrictWithProgress(it).apply {
                    solvedStreets = db.database.getDistrictDao().getNumberOfSolvedStreets(cityId, it.id)
                    totalStreets = db.database.getDistrictDao().getTotalNumberOfStreets(cityId, it.id)
                })
            }
            callback.invoke(result)
        }
    }

}
