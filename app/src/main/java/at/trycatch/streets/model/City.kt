package at.trycatch.streets.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Entity
class City {

    @PrimaryKey
    var id: String = ""
    var displayName: String = ""
    var version: Int = 0
    @Embedded
    var geoBounds: GeoBounds? = null

}
