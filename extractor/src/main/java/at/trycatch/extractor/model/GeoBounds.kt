package at.trycatch.extractor.model

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GeoBounds(val north: GeoLocation, val east: GeoLocation, val south: GeoLocation, val west: GeoLocation) {

    class Builder {

        var north: GeoLocation? = null
        var east: GeoLocation? = null
        var south: GeoLocation? = null
        var west: GeoLocation? = null

        fun include(location: GeoLocation) {

            // Latitude (North - South)
            val latitude = location.latitude
            if (north == null || north!!.latitude < latitude) {
                north = location
            }
            if (south == null || south!!.latitude > latitude) {
                south = location
            }

            // Longitude (West - East)
            val longitude = location.longitude
            if (west == null || west!!.longitude > longitude) {
                west = location
            }
            if (east == null || east!!.longitude < longitude) {
                east = location
            }

        }

        fun build(): GeoBounds {
            return GeoBounds(north!!, east!!, south!!, west!!)
        }

    }

}

class GeoLocation(val latitude: Double, val longitude: Double) {

    override fun toString(): String {
        return "$latitude,$longitude"
    }

}