package at.trycatch.streets.model

import androidx.room.Embedded

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GeoBounds(
        @Embedded(prefix = "north_") val north: GeoLocation,
        @Embedded(prefix = "east_") val east: GeoLocation,
        @Embedded(prefix = "south_") val south: GeoLocation,
        @Embedded(prefix = "west_") val west: GeoLocation) {

    override fun toString(): String {
        return "GeoBounds(north=$north, east=$east, south=$south, west=$west)"
    }
}

class GeoBoundsBuilder {

    var north: GeoLocation? = null
    var east: GeoLocation? = null
    var south: GeoLocation? = null
    var west: GeoLocation? = null

    fun build(): GeoBounds {
        return GeoBounds(north!!, east!!, south!!, west!!)
    }

}
