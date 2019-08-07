package at.trycatch.streets.model

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class PickedDistrict(val cityId: String) {

    var districtId: String? = null
    var displayName: String = ""
    var geoBounds: GeoBounds? = null

    var totalStreets = 0
    var solvedStreets = 0

    var isCity = false

    override fun toString(): String {
        return "PickedDistrict(cityId='$cityId', districtId=$districtId, displayName='$displayName', geoBounds=$geoBounds, totalStreets=$totalStreets, solvedStreets=$solvedStreets, isCity=$isCity)"
    }
    
}
