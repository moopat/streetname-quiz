package at.trycatch.streets.model

import com.mapbox.mapboxsdk.geometry.LatLng

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GeoLocation {

    var longitude: Double = 0.0
    var latitude: Double = 0.0

    companion object {

        @JvmStatic
        fun fromLatLng(latLng: LatLng): GeoLocation {
            val geoLocation = GeoLocation()
            geoLocation.latitude = latLng.latitude
            geoLocation.longitude = latLng.longitude
            return geoLocation
        }

        @JvmStatic
        fun fromString(line: String): GeoLocation {
            val split = line.split(",")
            return GeoLocation().apply {
                latitude = split[0].toDouble()
                longitude = split[1].toDouble()
            }
        }

    }

    override fun toString(): String {
        return "GeoLocation(longitude=$longitude, latitude=$latitude)"
    }


}