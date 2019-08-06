package at.trycatch.streets.model

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class DistrictWithProgress(val district: District) {

    var totalStreets = 0
    var solvedStreets = 0

    override fun toString(): String {
        return "DistrictWithProgress(district=$district, totalStreets=$totalStreets, solvedStreets=$solvedStreets)"
    }

}
