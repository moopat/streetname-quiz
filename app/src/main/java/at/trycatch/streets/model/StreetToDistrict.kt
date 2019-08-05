package at.trycatch.streets.model

import androidx.room.Entity

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Entity(primaryKeys = ["streetId", "districtId"])
class StreetToDistrict {

    var streetId = ""
    var districtId = ""

}
