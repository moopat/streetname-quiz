package at.trycatch.extractor

import at.trycatch.extractor.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.geojson.Feature
import org.geojson.FeatureCollection
import org.geojson.MultiPolygon
import org.geojson.Point
import org.tukaani.xz.XZInputStream
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import javax.imageio.ImageIO
import kotlin.streams.toList


class Extractor(private val cityId: String) {

    companion object {

        private const val VERSION = 1

    }

    val rawOutputDir: File
    val assetOutputDir: File
    val cacheDir = File("build/tmp")

    init {
        val outputDir = File("output")
        // TODO: Wipe output dir.

        rawOutputDir = File(outputDir, "raw").apply { mkdirs() }
        assetOutputDir = File(outputDir, "assets").apply { mkdirs() }
    }

    @Throws(Exception::class)
    fun start() {

        val streets = getStreetNames()
        println("Loaded ${streets.size} official street names.")
        println("")

        val officialNormalizedStreets = streets.map { it.key }
        val missingStreets = mutableListOf<String>().apply { addAll(officialNormalizedStreets) }

        // Get street features
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
        ObjectMapper().writeValue(File(rawOutputDir.path + "/$cityId" + "_geodata.json"), newFeatureCollection)

        // Load bounds
        val districts = getDistricts()
        val boundaries = getBoundaryFeatures().filter { it.properties.containsKey("boundary") }
                .filter { it.properties["name"] != null }
                .filter { districts.map { dist -> dist.name.getNormalized() }.contains((it.properties["name"] as String).getNormalized()) || it.properties["name"] == "Graz" }
                .filter { it.properties["boundary"] == "administrative" }

        // Find the bounds of Graz
        val city = City(cityId, "Graz")
        val rawCityBound = boundaries.first { it.properties["name"] == "Graz" }
        val cityBoundPoly = rawCityBound.geometry as MultiPolygon
        val cityBounds = GeoBounds.Builder()
        cityBoundPoly.coordinates.flatten().flatten().map { GeoLocation(it.latitude, it.longitude) }.forEach { cityBounds.include(it) }
        city.geoBounds = cityBounds.build()

        // Indices

        // Write the city index document.
        writeCitiesDocument(city)

        // Enrich districts with bounds.
        districts.forEach { district ->
            val rawBound = boundaries.first { district.name.getNormalized() == (it.properties["name"] as String).getNormalized() }
            val boundPoly = rawBound.geometry as MultiPolygon
            val bounds = GeoBounds.Builder()
            boundPoly.coordinates.flatten().flatten().map { GeoLocation(it.latitude, it.longitude) }.forEach { bounds.include(it) }
            district.geoBounds = bounds.build()
        }

        // Write the district index document.
        writeDistrictsDocument(districts)

        // Write the street index document.
        val usableStreets = streets.filterNot { missingStreets.contains(it.key) }.toList()
        writeStreetIndex(usableStreets, districts)

        // Generate the district previews.
        generateDistrictPreviews(districts)
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

    private fun getBoundaryFeatures(): FeatureCollection {
        val path = unpackFile("$cityId/boundaries.geojson.xz")
        val stream = FileInputStream(path)
        return ObjectMapper().readValue(stream, FeatureCollection::class.java)
    }

    private fun unpackFile(path: String): String {
        val stream = javaClass.classLoader.getResourceAsStream(path)
        val bufferedStream = BufferedInputStream(stream!!)
        val outPath = cacheDir.apply { mkdirs() }.path + "/unpacked-" + Random().nextInt()
        val out = FileOutputStream(outPath)
        val xzIn = XZInputStream(bufferedStream)

        xzIn.copyTo(out)

        out.close()
        xzIn.close()

        return outPath
    }

    @Throws(IOException::class)
    private fun writeCitiesDocument(city: City) {
        val builder = StringBuilder()
        builder.append(VERSION).append(";")
        builder.append(city.id).append(";")
        builder.append(city.name).append(";")
        builder.append(city.geoBounds!!.north).append(";")
        builder.append(city.geoBounds!!.east).append(";")
        builder.append(city.geoBounds!!.south).append(";")
        builder.append(city.geoBounds!!.west).append(";")

        val writer = FileWriter(rawOutputDir.path + "/cities.csv")
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
                    .append(it.geoBounds!!.north).append(";")
                    .append(it.geoBounds!!.east).append(";")
                    .append(it.geoBounds!!.south).append(";")
                    .append(it.geoBounds!!.west).append(";")
                    .append("\n")
        }

        val writer = FileWriter(rawOutputDir.path + "/$cityId" + "_districts.csv")
        writer.write(builder.toString())
        writer.close()
    }

    private fun generateDistrictPreviews(districts: List<District>) {
        // https://api.mapbox.com/styles/v1/moopat/cjndg38970vq62rnu542m5fhf/static/-122.4241,37.78,14.25/700x409?access_token=pk.eyJ1IjoibW9vcGF0IiwiYSI6IlNVNU5xQVEifQ.73yuoRIlKsGZ9zVBjkjSZQ&logo=false&attribution=false

        val style = "cjndg38970vq62rnu542m5fhf"
        val user = "moopat"
        val key = "pk.eyJ1IjoibW9vcGF0IiwiYSI6IlNVNU5xQVEifQ.73yuoRIlKsGZ9zVBjkjSZQ"
        val zoom = 13.0f
        val dimens = "700x409"

        districts.forEach { district ->
            val avgLatitude = (district.geoBounds!!.north.latitude + district.geoBounds!!.south.latitude) / 2
            val avgLongitude = (district.geoBounds!!.west.longitude + district.geoBounds!!.east.longitude) / 2

            val url = "https://api.mapbox.com/styles/v1/$user/$style/static/$avgLongitude,$avgLatitude,$zoom/$dimens?access_token=$key&logo=false&attribution=false"

            println(district.name + ": " + url)

            val inStream = URL(url).openStream()
            val pngFile = File(File(cacheDir, "thumbs").apply { mkdirs() }, "${district.id}.png")
            Files.copy(inStream, Paths.get(pngFile.toURI()), StandardCopyOption.REPLACE_EXISTING)
            val jpgFile = File(assetOutputDir, "${district.id}.jpg")

            convertPngToJpg(pngFile, jpgFile)

            // TODO: Copy file to Android project
        }

    }

    private fun convertPngToJpg(png: File, jpg: File) {
        val img = ImageIO.read(png)
        ImageIO.write(img, "jpg", jpg)
    }

    private fun writeStreetIndex(streets: List<Street>, districts: List<District>) {
        val builder = StringBuilder()

        streets.forEach { street ->
            street.districts.forEach { districtId ->
                val district = districts.first { it.originalId == districtId }

                builder.append(VERSION).append(";")
                        .append(street.key).append(";")
                        .append(street.name).append(";")
                        .append(district.id).append(";")
                        .append("\n")
            }
        }

        val writer = FileWriter(rawOutputDir.path + "/$cityId" + "_streets.csv")
        writer.write(builder.toString())
        writer.close()
    }

}
