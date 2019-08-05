package at.trycatch.extractor.model

import at.trycatch.extractor.getNormalized

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class District(val cityId: String, val originalId: Int, val name: String) {

    val id = "$cityId-" + name.getNormalized()
    var sequence: Int = 0

    companion object {

        @JvmStatic
        fun createFromLine(city: String, line: String): District {
            // 1;Innere Stadt
            val segments = line.split(";")
            return District(city, segments[0].toInt(), segments[1]).apply { sequence = originalId }
        }

    }

    override fun toString(): String {
        return "District(cityId=$cityId, originalId=$originalId, name='$name', id='$id', sequence=$sequence)"
    }


}
