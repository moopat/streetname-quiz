package at.trycatch.streets.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Entity
open class District {

    @PrimaryKey
    var id: String = ""
    var cityId: String = ""
    var sequence: Int = 0
    var displayName: String = ""
    @Embedded
    var geoBounds: GeoBounds? = null
    var version: Int = 0

    override fun toString(): String {
        return "District(id='$id', cityId='$cityId', sequence=$sequence, displayName='$displayName', geoBounds=$geoBounds, version=$version)"
    }

    companion object {

        @JvmStatic
        fun createFromCsv(city: String, line: String): District {
            val parts = line.split(";")

            return District().apply {
                id = parts[1]
                cityId = city
                sequence = parts[3].toInt()
                displayName = parts[2]
                version = parts[0].toInt()
                geoBounds = GeoBoundsBuilder().apply {
                    north = GeoLocation.fromString(parts[4])
                    east = GeoLocation.fromString(parts[5])
                    south = GeoLocation.fromString(parts[6])
                    west = GeoLocation.fromString(parts[7])
                }.build()
            }
        }

    }

}
