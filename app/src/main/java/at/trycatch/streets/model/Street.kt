package at.trycatch.streets.model

import androidx.room.Entity
import java.util.*

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Entity(primaryKeys = ["id", "cityId"])
class Street {

    var id: String = ""
    var cityId: String = ""
    var displayName: String = ""

    var totalGuesses = 0
    var totalCorrectGuesses = 0
    var consecutiveCorrectGuesses = 0
    var lastGuessedOn: Date? = null
    var flaggedForDeletion = false

    var version: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Street

        if (id != other.id) return false
        if (cityId != other.cityId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + cityId.hashCode()
        return result
    }

}