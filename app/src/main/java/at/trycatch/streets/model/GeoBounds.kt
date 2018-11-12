package at.trycatch.streets.model

import android.arch.persistence.room.Embedded

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class GeoBounds {

    @Embedded
    var north: GeoLocation? = null

    @Embedded
    var east: GeoLocation? = null

    @Embedded
    var south: GeoLocation? = null

    @Embedded
    var west: GeoLocation? = null

}