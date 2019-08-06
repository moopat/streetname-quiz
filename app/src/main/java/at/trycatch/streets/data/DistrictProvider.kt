package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.data.room.AppDatabaseInstance
import at.trycatch.streets.model.City
import at.trycatch.streets.model.District
import at.trycatch.streets.model.StreetToDistrict

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DistrictProvider(private val context: Context) {

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

}
