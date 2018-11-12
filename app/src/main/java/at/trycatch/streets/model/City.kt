package at.trycatch.streets.model

import android.arch.persistence.room.Embedded
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

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
    var geoBounds = GeoBounds()

}