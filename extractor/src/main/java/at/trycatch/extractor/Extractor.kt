package at.trycatch.extractor

import at.trycatch.extractor.model.District
import at.trycatch.extractor.model.Street
import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.Feature
import org.geojson.FeatureCollection
import org.geojson.Point
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.streams.toList


class Extractor(private val cityId: String) {

    companion object {

        private const val VERSION = 1

    }

    val outputDir = File("output")

    init {
        outputDir.mkdirs()
    }

    @Throws(Exception::class)
    fun start() {

        val streets = getStreetNames()
        println("Loaded ${streets.size} official street names.")
        println("")

        val officialNormalizedStreets = streets.map { it.key }
        val missingStreets = mutableListOf<String>().apply { addAll(officialNormalizedStreets) }

        // Load geojson
        val features = getFeatures()
        val eligibleFeatures = features.features.stream()
                .filter { feature -> feature.properties.containsKey("name") }
                .filter { feature -> !feature.properties.containsKey("addr:housenumber") }
                .filter { feature -> feature.geometry !is Point }
                .filter { feature -> officialNormalizedStreets.contains((feature.properties["name"] as String).getNormalized()) }
                .collect(Collectors.toList<Feature>())

        // Set normalized names.
        eligibleFeatures.forEach { it.setProperty("skey", it.getProperty<String>("name").getNormalized()) }

        // Only set the required properties.
        eligibleFeatures.forEach {
            val properties = HashMap<String, Any>()
            properties["skey"] = it.getProperty<Any>("skey")
            properties["name"] = it.getProperty<Any>("name")
            it.properties = properties
        }

        println("Found " + eligibleFeatures.size + " eligible features that will be included in the app.")

        // Remove all existing streets from missing streets and
        val count = AtomicInteger()
        eligibleFeatures.stream()
                .map { feature -> feature.properties["name"] }
                .map { (it as String).getNormalized() }
                .distinct()
                .forEach {
                    missingStreets.remove(it)
                    count.getAndIncrement()
                }

        println("Found " + count.get() + " matching streets in the open dataset.")
        println("Found " + missingStreets.size + " missing streets.\n")

        missingStreets.forEach { println("  $it") }

        // TODO: Remove blacklisted streets

        // Write feature collection
        val newFeatureCollection = FeatureCollection()
        newFeatureCollection.addAll(eligibleFeatures)
        ObjectMapper().writeValue(File(outputDir.path + "/$cityId-geodata.json"), newFeatureCollection)

        // Find the bounds of Graz
        val north = "47.116536,15.424495"
        val south = "47.023936,15.438228"
        val east = "47.067154,15.488289"
        val west = "47.06084,15.377052"

        // Indices

        // Write the city index document.
        writeCitiesDocument(north, south, east, west)

        // Write the district index document.
        val districts = getDistricts()
        writeDistrictsDocument(districts)

        // Write the street index document.
        val usableStreets = streets.filterNot { missingStreets.contains(it.key) }.toList()
        writeStreetIndex(usableStreets, districts)

    }

    private fun getStreetNames(): List<Street> {

        val result = mutableListOf<Street>()
        var didReadFirstLine = false // The first line needs to be skipped.

        val url = javaClass.classLoader.getResource("$cityId/streetnames.csv")
        Files.lines(Paths.get(url!!.toURI()), Charset.forName("ISO-8859-1")).forEach {
            if (didReadFirstLine) {
                if (it != null && it.length > 5) Street.createFromLine(it)?.let { result.add(it) }
            }
            didReadFirstLine = true
        }

        return result
    }

    private fun getDistricts(): List<District> {
        val url = javaClass.classLoader.getResource("$cityId/districts.csv")
        return Files.lines(Paths.get(url!!.toURI()), Charset.forName("UTF-8")).map { District.createFromLine(cityId, it) }.toList()
    }

    private fun getFeatures(): FeatureCollection {
        val stream = javaClass.classLoader.getResourceAsStream("$cityId/raw.geojson")
        return ObjectMapper().readValue(stream, FeatureCollection::class.java)
    }

    @Throws(IOException::class)
    private fun writeCitiesDocument(north: String, south: String, east: String, west: String) {
        val builder = StringBuilder()
        builder.append("graz;").append("Graz;").append("1;")
                .append(north).append(";")
                .append(south).append(";")
                .append(east).append(";")
                .append(west).append(";")

        val writer = FileWriter(outputDir.path + "/cities.csv")
        writer.write(builder.toString() + "\n")
        writer.close()
    }

    @Throws(IOException::class)
    private fun writeDistrictsDocument(districts: List<District>) {
        val builder = StringBuilder()

        districts.forEach {
            builder.append(VERSION).append(";")
                    .append(it.id).append(";")
                    .append(it.name).append(";")
                    .append(it.sequence).append(";")
                    .append("\n")
        }

        val writer = FileWriter(outputDir.path + "/$cityId-districts.csv")
        writer.write(builder.toString())
        writer.close()
    }

    private fun writeStreetIndex(streets: List<Street>, districts: List<District>) {
        val builder = StringBuilder()

        streets.forEach { street ->
            street.districts.forEach { districtId ->
                val district = districts.filter { it.originalId == districtId }.first()

                builder.append(VERSION).append(";")
                        .append(street.key).append(";")
                        .append(street.name).append(";")
                        .append(district.id).append(";")
                        .append("\n")
            }
        }

        val writer = FileWriter(outputDir.path + "/$cityId-streets.csv")
        writer.write(builder.toString())
        writer.close()
    }

}
