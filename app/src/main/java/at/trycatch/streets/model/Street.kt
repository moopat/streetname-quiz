package at.trycatch.streets.model

import android.arch.persistence.room.PrimaryKey

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class Street {

    @PrimaryKey
    var id: String = ""
    val key: String = ""
    var displayName: String = ""
    var version: Int = 0

}