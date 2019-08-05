package at.trycatch.extractor.model

import at.trycatch.extractor.getNormalized

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class Street(val name: String) {

    val key = name.getNormalized()
    val districts = mutableListOf<Int>()

    companion object {

        @JvmStatic
        fun createFromLine(line: String): Street? {

            // 5009;Kalvarienbrücke;3,4;
            val splitString = line.split(";")
            val street = splitString[1]
            val districts =
                    try {
                        splitString[2].split(",").map { it.trim().toInt() }
                    } catch (e: NumberFormatException) {
                        println("Skipping a street because the district is invalid: $line")
                        return null
                    }

            return Street(street).apply { this.districts.addAll(districts) }
        }

    }

    override fun toString(): String {
        return "Street(name='$name', key='$key', districts=$districts)"
    }

}
