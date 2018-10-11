package at.trycatch.streets.viewmodel

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.location.Location
import android.util.Log
import at.trycatch.streets.R
import com.cocoahero.android.geojson.Feature
import com.cocoahero.android.geojson.FeatureCollection
import com.cocoahero.android.geojson.LineString
import com.cocoahero.android.geojson.Position
import com.google.android.gms.maps.model.LatLng
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.io.BufferedReader
import java.io.InputStreamReader
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

    val currentGameState = MutableLiveData<Int>()
    val streetLines = MutableLiveData<MutableList<LineString>>()
    val currentObjective = MutableLiveData<String>()
    val currentlyLoading = MutableLiveData<Boolean>()
    val solvable = MutableLiveData<Boolean>()
    var featureCollection: FeatureCollection? = null
    var selectedFeature: Feature? = null
    var solved = false
    val streetNames = MutableLiveData<List<String>>()

    private val random = Random()
    private val streetLinesInternal = mutableListOf<LineString>()
    private val streetNamesInternal = mutableListOf<String>()

    init {
        currentGameState.postValue(STATE_GUESSING)
    }

    fun initializeStreets() {
        doAsync {
            val resource = getApplication<Application>().resources.openRawResource(R.raw.grazstreets)
            val bufferedReader = BufferedReader(InputStreamReader(resource))
            while (bufferedReader.ready()) {
                streetNamesInternal.add(bufferedReader.readLine())
            }
            streetNames.postValue(streetNamesInternal)
        }
    }

    fun startNewRound() {
        currentGameState.postValue(STATE_GUESSING)
        currentObjective.postValue(getRandomStreetName())
        solvable.postValue(false)
        currentlyLoading.postValue(false)
        streetLinesInternal.clear()
        streetLines.postValue(streetLinesInternal)
        solved = false
    }

    fun solveRound(callback: () -> Unit) {
        // If we haven't already won we need to surrender.
        if (currentGameState.value == STATE_GUESSING) {
            currentGameState.postValue(STATE_SURRENDERED)
        }

        currentlyLoading.postValue(true)
        solvable.postValue(false)

        // Make a new selection.
        streetLinesInternal.clear()
        streetLines.postValue(streetLinesInternal)

        doAsync {
            getFeaturesByName(currentObjective.value!!)
                    .map { it.geometry as LineString } // TODO: Might not be a LineString!
                    .forEach { lineString ->
                        streetLinesInternal.add(lineString)
                    }
            streetLines.postValue(streetLinesInternal)
            currentlyLoading.postValue(false)
            uiThread { callback.invoke() }
        }
    }

    private fun getRandomStreetName(): String? {
        return streetNamesInternal.shuffled(random).firstOrNull()
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
                    .map { it.geometry as LineString }
                    .forEach { lineString ->
                        streetLinesInternal.add(lineString)
                    }
            streetLines.postValue(streetLinesInternal)
            currentlyLoading.postValue(false)
            solvable.postValue(true)
        }
    }

    fun makeCorrectSelection(callback: () -> Unit) {
        currentlyLoading.postValue(true)
        solvable.postValue(false)

        streetLinesInternal.clear()
        streetLines.postValue(streetLinesInternal)

        doAsync {
            getFeaturesByName(currentObjective.value!!)
                    .map { it.geometry as LineString }
                    .forEach { lineString ->
                        streetLinesInternal.add(lineString)
                    }
            streetLines.postValue(streetLinesInternal)
            currentlyLoading.postValue(false)
            uiThread { callback.invoke() }
        }
    }

    fun getFeaturesByName(name: String): List<Feature> {
        return featureCollection!!.features
                .asSequence()
                .filter { it.properties.optString("name", "hugo") == name }
                .filter { it.geometry is LineString }
                .toList()
    }

    fun getSelectedStreetName(): String? {
        return selectedFeature?.properties?.optString("name")
    }

    fun solve(): Boolean {
        Log.d("MapsViewModel", "Selected " + selectedFeature!!.properties.getString("name") + ". Should have ${currentObjective.value}")
        solved = selectedFeature!!.properties.getString("name") == currentObjective.value
        if (solved) {
            currentGameState.postValue(STATE_WON)
            solvable.postValue(false)
        }
        return solved
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

}