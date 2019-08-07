package at.trycatch.streets.viewmodel

import android.app.Application
import android.location.Location
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import at.trycatch.streets.data.CityProvider
import at.trycatch.streets.data.DistrictProvider
import at.trycatch.streets.data.GameServiceLayer
import at.trycatch.streets.data.Settings
import at.trycatch.streets.model.PickedDistrict
import at.trycatch.streets.model.Street
import com.cocoahero.android.geojson.*
import com.mapbox.mapboxsdk.geometry.LatLng
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.*

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class MapsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val STATE_GUESSING = 0
        const val STATE_WON = 1
        const val STATE_SURRENDERED = 2
    }

    val cityId = "graz"
    private var districtId: String? = null

    val gameService: GameServiceLayer = GameServiceLayer(application)
    val districtProvider = DistrictProvider(application)
    val cityProvider = CityProvider(application)

    var testMode = false
    var currentIndex = -1

    val selectedDistrict = MutableLiveData<PickedDistrict>()
    val points = MutableLiveData<Long>()
    val currentGameState = MutableLiveData<Int>()
    val streetLines = MutableLiveData<MutableList<LineString>>()
    val currentObjective = MutableLiveData<Street>()
    val currentlyLoading = MutableLiveData<Boolean>()
    val solvable = MutableLiveData<Boolean>()
    var featureCollection: FeatureCollection? = null
    var selectedFeature: Feature? = null
    var solved = false

    private var didGetPointSubtraction = false

    private val streetLinesInternal = mutableListOf<LineString>()
    private val settings = Settings(application)

    init {
        selectedDistrict.postValue(PickedDistrict("graz").apply {
            isCity = true
            displayName = "Graz"
        })
        currentGameState.postValue(STATE_GUESSING)
        points.postValue(settings.getPoints())
        districtId = settings.getDistrict()
        updateDistrictInfo()
    }

    private fun updateDistrictInfo() {
        if (districtId != null) {
            districtProvider.getAllDistrictsWithProgress(cityId) {
                val district = it.filter { districtId == it.district.id }.first()
                selectedDistrict.postValue(PickedDistrict(district.district.cityId).apply {
                    isCity = false
                    geoBounds = district.district.geoBounds
                    displayName = district.district.displayName
                    districtId = district.district.id
                    totalStreets = district.totalStreets
                    solvedStreets = district.solvedStreets
                })
            }
        } else {
            cityProvider.getCityWithProgress(cityId) {
                it?.let {
                    selectedDistrict.postValue(PickedDistrict(it.city.id).apply {
                        isCity = true
                        geoBounds = it.city.geoBounds
                        displayName = it.city.displayName
                        districtId = it.city.id
                        totalStreets = it.totalStreets
                        solvedStreets = it.solvedStreets
                    })
                }
            }
        }
    }

    fun setDistrictId(districtId: String?) {
        if (districtId != this.districtId || currentObjective.value == null) {
            this.districtId = districtId
            startNewRound()
        }
        updateDistrictInfo()
    }

    fun startNewRound() {
        if (testMode) {
            currentIndex++
            gameService.getSequentialStreet(cityId, districtId, currentIndex) {
                if (it == null && currentIndex > 0) {
                    currentIndex = -1
                    startNewRound()
                } else {
                    handleNewRound(it)
                }
            }
        } else {
            gameService.getRandomStreet(cityId, districtId) {
                handleNewRound(it)
            }
        }

    }

    private fun handleNewRound(street: Street?) {
        street?.let {
            didGetPointSubtraction = false
            currentGameState.postValue(STATE_GUESSING)
            currentObjective.postValue(it)
            solvable.postValue(false)
            currentlyLoading.postValue(false)
            streetLinesInternal.clear()
            streetLines.postValue(streetLinesInternal)
            solved = false
        }
    }

    fun subtractPoints() {
        if (didGetPointSubtraction) return
        settings.subtractPoints(3)
        didGetPointSubtraction = true
        points.postValue(settings.getPoints())
    }

    fun awardPoints() {
        settings.addPoints(10)
        points.postValue(settings.getPoints())
    }

    fun solveRound(callback: () -> Unit) {
        // If we haven't already won we need to surrender.
        if (currentGameState.value == STATE_GUESSING) {
            currentGameState.postValue(STATE_SURRENDERED)
            updateCurrentStreet(false)
            subtractPoints()
        }

        currentlyLoading.postValue(true)
        solvable.postValue(false)

        // Make a new selection.
        streetLinesInternal.clear()
        streetLines.postValue(streetLinesInternal)

        doAsync {
            getFeaturesByName(currentObjective.value!!.displayName)
                    .asSequence()
                    .map {
                        val lineStrings = mutableListOf<LineString>()
                        when {
                            it.geometry is LineString -> lineStrings.add(it.geometry as LineString)
                            it.geometry is MultiPolygon -> {
                                val poly = it.geometry as MultiPolygon
                                lineStrings.addAll(poly.toLineStrings())
                            }
                        }
                        lineStrings
                    }
                    .toList()
                    .flatMap { it }
                    .forEach { lineString ->
                        streetLinesInternal.add(lineString)
                    }

            if (streetLinesInternal.isEmpty()) {
                getFeaturesByName(currentObjective.value!!.displayName)
                        .asSequence()
                        .filter { it.geometry is MultiPolygon }
                        .map { it.geometry as MultiPolygon }
                        .forEach { multiPolygon ->
                            multiPolygon.polygons
                                    .flatMap { it.rings }
                                    .map {
                                        val lineString = LineString()
                                        lineString.positions = it.positions
                                        lineString
                                    }
                                    .forEach { streetLinesInternal.add(it) }
                        }
            }

            streetLines.postValue(streetLinesInternal)
            currentlyLoading.postValue(false)
            uiThread { callback.invoke() }
        }
    }

    private fun getClosestFeature(latLng: LatLng): Feature {
        var shortestDistance = 10000000
        var closestElement: Feature? = null
        featureCollection!!.features.asSequence().filter { it.geometry is LineString }.forEach {
            val geo = it.geometry as LineString
            val distance = geo.getDistance(latLng)
            if (distance < shortestDistance) {
                shortestDistance = distance
                closestElement = it
            }
        }

        featureCollection!!.features.asSequence().filter { it.geometry is MultiPolygon }.forEach { feature ->
            val geo = feature.geometry as MultiPolygon
            geo.toLineStrings().forEach {
                val distance = it.getDistance(latLng)
                if (distance < shortestDistance) {
                    shortestDistance = distance
                    closestElement = feature
                }
            }
        }
        return closestElement!!
    }

    fun makeSelectionByCoordinates(latLng: LatLng) {
        currentlyLoading.postValue(true)
        solvable.postValue(false)

        streetLinesInternal.clear()
        streetLines.postValue(streetLinesInternal)

        doAsync {
            selectedFeature = getClosestFeature(latLng)

            getFeaturesByName(selectedFeature!!.properties.getString("name"))
                    .asSequence()
                    .map {
                        val lineStrings = mutableListOf<LineString>()
                        when {
                            it.geometry is LineString -> lineStrings.add(it.geometry as LineString)
                            it.geometry is MultiPolygon -> {
                                val poly = it.geometry as MultiPolygon
                                lineStrings.addAll(poly.toLineStrings())
                            }
                        }
                        lineStrings
                    }
                    .toList()
                    .flatMap { it }
                    .forEach { lineString ->
                        streetLinesInternal.add(lineString)
                    }

            streetLines.postValue(streetLinesInternal)
            currentlyLoading.postValue(false)
            solvable.postValue(true)
        }
    }

    private fun getFeaturesByName(name: String): List<Feature> {
        val normalizedName = name.normalize()
        return featureCollection!!.features
                .asSequence()
                .filter { it.properties.getString("skey") == normalizedName }
                .filter { it.geometry is LineString || it.geometry is MultiPolygon }
                .toList()
    }

    fun getSelectedStreetName(): String? {
        return selectedFeature?.properties?.optString("name")
    }

    fun solve(): Boolean {
        Log.d("MapsViewModel", "Selected " + selectedFeature!!.properties.getString("name") + ". Should have ${currentObjective.value}")
        solved = selectedFeature!!.properties.getString("skey") == currentObjective.value?.id
        if (solved) {
            updateCurrentStreet(true)
            awardPoints()
            currentGameState.postValue(STATE_WON)
            solvable.postValue(false)
        } else {
            subtractPoints()
            updateCurrentStreet(false)
        }
        return solved
    }

    private fun updateCurrentStreet(won: Boolean) {
        currentObjective.value?.let {
            if (won) {
                it.consecutiveCorrectGuesses++
                it.totalCorrectGuesses++
            } else {
                it.consecutiveCorrectGuesses = 0
            }

            it.lastGuessedOn = Date()
            it.totalGuesses++

            gameService.updateStreet(it)
        }

        Handler().postDelayed({ updateDistrictInfo() }, 100)
    }

    fun LineString.getDistance(position: Position) = getDistance(LatLng(position.latitude, position.longitude))

    fun LineString.getDistance(latLng: LatLng) = positions.asSequence().map {
        val location = Location("manual")
        location.latitude = latLng.latitude
        location.longitude = latLng.longitude
        val otherLocation = Location("manual")
        otherLocation.latitude = it.latitude
        otherLocation.longitude = it.longitude
        location.distanceTo(otherLocation).toInt()
    }.min()!!

    private fun String.normalize(): String {
        return this.toLowerCase()
                .replace("st.", "sankt")
                .replace("prof.", "professor")
                .replace(".", "")
                .replace(" ", "")
                .replace("-", "")
    }

    private fun MultiPolygon.toLineStrings(): MutableList<LineString> {
        return polygons
                .flatMap { it.rings }
                .asSequence()
                .map {
                    val lineString = LineString()
                    lineString.positions = it.positions
                    lineString
                }
                .toCollection(mutableListOf())
    }

}