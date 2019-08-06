package at.trycatch.streets.model

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class CityWithProgress(val city: City) {

    var totalStreets = 0
    var solvedStreets = 0

    override fun toString(): String {
        return "CityWithProgress(city=$city, totalStreets=$totalStreets, solvedStreets=$solvedStreets)"
    }

}
