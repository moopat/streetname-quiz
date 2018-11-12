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

    }

}