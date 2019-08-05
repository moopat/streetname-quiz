package at.trycatch.streets.model

import androidx.room.Embedded

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GeoBounds(
        @Embedded val north: GeoLocation,
        @Embedded val south: GeoLocation,
        @Embedded val east: GeoLocation,
        @Embedded val west: GeoLocation)
