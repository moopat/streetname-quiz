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

}