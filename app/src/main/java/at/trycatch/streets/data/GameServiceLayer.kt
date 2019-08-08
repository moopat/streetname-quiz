package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.model.Street
import org.jetbrains.anko.doAsync
import java.util.*

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GameServiceLayer(context: Context) {

    companion object {
        private const val CORRECT_ANSWER_RATIO = 0.1
    }

    private val random = Random()
    private val streetProvider = StreetProvider(context)

    fun getRandomStreet(previousStreet: Street?, cityId: String, districtId: String?, callback: (street: Street?) -> Unit) {
        doAsync {
            val incorrectlyAnswered = streetProvider.getAllUnanswered(cityId, districtId).toMutableList()
            val correctlyAnswered = streetProvider.getAllCorrectlyAnswered(cityId, districtId).toMutableList()

            // Remove the previous street
            previousStreet?.run {
                incorrectlyAnswered.remove(this);
                correctlyAnswered.remove(this)
            }

            val useCorrectAnswer = random.nextFloat() <= CORRECT_ANSWER_RATIO

            val street = if (useCorrectAnswer && correctlyAnswered.size > 0) {
                correctlyAnswered.shuffled(random).first()
            } else {
                incorrectlyAnswered.shuffled(random).first()
            }

            callback.invoke(street)
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
