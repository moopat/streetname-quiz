package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.model.Street
import org.jetbrains.anko.doAsync

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GameServiceLayer(context: Context) {

    private val streetProvider = StreetProvider(context)

    fun getRandomStreet(cityId: String, districtId: String?, callback: (street: Street?) -> Unit) {
        doAsync {
            callback.invoke(streetProvider.getRandom(cityId, districtId))
        }
    }

    fun getSequentialStreet(cityId: String, districtId: String?, index: Int, callback: (street: Street?) -> Unit) {
        doAsync {
            callback.invoke(streetProvider.getByIndex(cityId, districtId, index))
        }
    }

    fun updateStreet(street: Street) {
        doAsync {
            streetProvider.updateStreet(street)
        }
    }

}
