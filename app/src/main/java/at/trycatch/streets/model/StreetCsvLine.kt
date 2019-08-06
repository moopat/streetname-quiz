package at.trycatch.streets.model

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class StreetCsvLine(
        val version: Int,
        val streetId: String,
        val streetName: String,
        val districtId: String) {

    companion object {

        @JvmStatic
        fun getFromCsv(line: String): StreetCsvLine {
            val segments = line.split(";")
            return StreetCsvLine(
                    segments[0].toInt(),
                    segments[1],
                    segments[2],
                    segments[3]
            )
        }
    }

    override fun toString(): String {
        return "StreetCsvLine(version=$version, streetId='$streetId', streetName='$streetName', districtId='$districtId')"
    }

}
