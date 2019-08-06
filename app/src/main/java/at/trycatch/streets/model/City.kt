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

    companion object {

        @JvmStatic
        fun createFromCsv(line: String): City {
            val parts = line.split(";")

            return City().apply {
                version = parts[0].toInt()
                id = parts[1]
                displayName = parts[2]
                geoBounds = GeoBoundsBuilder().apply {
                    north = GeoLocation.fromString(parts[3])
                    east = GeoLocation.fromString(parts[4])
                    south = GeoLocation.fromString(parts[5])
                    west = GeoLocation.fromString(parts[6])
                }.build()
            }
        }

    }

    override fun toString(): String {
        return "City(id='$id', displayName='$displayName', version=$version, geoBounds=$geoBounds)"
    }


}
