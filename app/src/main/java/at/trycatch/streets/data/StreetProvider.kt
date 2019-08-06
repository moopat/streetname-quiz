package at.trycatch.streets.data

import android.content.Context
import android.util.Log
import at.trycatch.streets.data.room.AppDatabaseInstance
import at.trycatch.streets.model.City
import at.trycatch.streets.model.Street
import at.trycatch.streets.model.StreetCsvLine
import java.util.*

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class StreetProvider(private val context: Context) {

    private val db = AppDatabaseInstance.getInstance(context)
    private val random = Random()

    fun flagStreetsForDelection(city: City) {
        db.database.getStreetDao().flagStreetsAsDeletable(city.id)
    }

    fun removeFlaggedStreets(city: City) {
        db.database.getStreetDao().deleteFlagged(city.id)
    }

    fun insertOrUpdateStreet(city: City, line: StreetCsvLine) {
        val dao = db.database.getStreetDao()
        val street = dao.findStreet(line.streetId, city.id) ?: Street()
        street.flaggedForDeletion = false
        street.id = line.streetId
        street.displayName = line.streetName
        street.version = line.version
        street.cityId = city.id
        db.database.getStreetDao().insert(street)
    }

    fun getByIndex(cityId: String, districtId: String?, index: Int): Street? {
        if (districtId == null) {
            return db.database.getStreetDao().findStreetByIndex(cityId, index)
        } else {
            return db.database.getStreetDao().findStreetByIndex(cityId, districtId, index)
        }
    }

    fun getRandom(cityId: String, districtId: String?): Street? {
        if (districtId == null) {
            val data = db.database.getStreetDao().findAll(cityId)
            return data.shuffled(random).firstOrNull()
        } else {
            val data = db.database.getStreetDao().findAll(cityId, districtId)
            return data.shuffled(random).firstOrNull()
        }
    }

}
