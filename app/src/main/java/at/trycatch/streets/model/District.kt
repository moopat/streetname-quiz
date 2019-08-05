package at.trycatch.streets.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Entity
class District {

    @PrimaryKey
    var id: String = ""
    var cityId: String = ""
    var sequence: Int = 0
    var displayName: String = ""
    @Embedded
    var geoBounds: GeoBounds? = null
    var version: Int = 0

}