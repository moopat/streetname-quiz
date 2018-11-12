package at.trycatch.streets.model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.PrimaryKey

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class District {

    @PrimaryKey
    var id: String = ""
    var sequence: Int = 0
    var displayName: String = ""
    @Embedded
    var geoBounds = GeoBounds()
    var version: Int = 0

}