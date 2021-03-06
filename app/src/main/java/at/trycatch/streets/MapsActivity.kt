package at.trycatch.streets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.trycatch.streets.activity.DistrictPickerActivity
import at.trycatch.streets.activity.StarterActivity
import at.trycatch.streets.data.Settings
import at.trycatch.streets.lifecycle.MapboxLifecycleObserver
import at.trycatch.streets.service.ImportService
import at.trycatch.streets.util.MessageUtil
import at.trycatch.streets.viewmodel.MapsViewModel
import at.trycatch.streets.widget.MapsPopupMenu
import com.cocoahero.android.geojson.Feature
import com.cocoahero.android.geojson.FeatureCollection
import com.cocoahero.android.geojson.GeoJSON
import com.cocoahero.android.geojson.LineString
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.firebase.analytics.FirebaseAnalytics
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.style.layers.Layer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kotlinx.android.synthetic.main.activity_maps.*
import org.jetbrains.anko.doAsync
import org.json.JSONException
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        const val RC_TERMS = 101
    }

    lateinit var model: MapsViewModel
    private var map: MapboxMap? = null
    private val handler = Handler()
    private val lines = mutableListOf<com.cocoahero.android.geojson.Geometry>()
    private var bounceAnimation: YoYo.YoYoString? = null
    private var selectionSource: GeoJsonSource? = null
    private var selectionLayer: Layer? = null

    // Updated by changes in the game state. Ad-hoc evaluation.
    private var mayClickMap = true
    private lateinit var mapComponent: MapboxLifecycleObserver

    private var receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            model.startNewRound()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Mapbox.getInstance(this, "pk.eyJ1IjoibW9vcGF0IiwiYSI6IlNVNU5xQVEifQ.73yuoRIlKsGZ9zVBjkjSZQ")

        setContentView(R.layout.activity_maps)

        if (!Settings(this).hasAcceptedLatestTerms()) {
            startActivityForResult(Intent(this, StarterActivity::class.java), RC_TERMS)
        }

        // Import Data
        ImportService.startImport(this)

        mapComponent = MapboxLifecycleObserver(mapView, savedInstanceState)
        lifecycle.addObserver(mapComponent)
        mapView.getMapAsync(this)

        model = ViewModelProviders.of(this).get(MapsViewModel::class.java)

        model.startNewRound()

        model.currentGameState.observe(this, Observer {
            if (it == null) return@Observer

            when (it) {
                MapsViewModel.STATE_GUESSING -> {
                    mayClickMap = true
                    stopBounceAnimation()

                    btnNext.setTextColor(ContextCompat.getColor(this, R.color.semiWhite))
                    btnNext.setText(R.string.button_skip)

                    refocusMap()
                }
                MapsViewModel.STATE_WON -> {
                    mayClickMap = false
                    startBounceAnimation()

                    btnNext.setText(R.string.button_next)
                    btnNext.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
                MapsViewModel.STATE_SURRENDERED -> {
                    mayClickMap = false
                    startBounceAnimation()

                    btnNext.setText(R.string.button_next)
                    btnNext.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        })

        model.currentObjective.observe(this, Observer {
            // We found a new objective.
            hideNotificationsNow()
            tvStreetName.text = it.displayName
        })

        model.currentlyLoading.observe(this, Observer {
            progressBar.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        model.streetLines.observe(this, Observer { lineStrings ->

            val featureCollection = FeatureCollection()
            lineStrings?.map { Feature(it) }?.forEach { featureCollection.addFeature(it) }
            selectionSource?.setGeoJson(featureCollection.toJSON().toString())

            lines.clear()
            featureCollection.features.forEach { lines.add(it.geometry) }
        })

        model.solvable.observe(this, Observer {
            if (it == true) confirm.show() else confirm.hide()
        })

        model.points.observe(this, Observer {
            tvPoints.text = it?.toString()
            it?.let { points ->
                when {
                    points < 0 -> tvPoints.setBackgroundResource(R.drawable.bg_red_rounded)
                    points >= 100 -> tvPoints.setBackgroundResource(R.drawable.bg_green_rounded)
                    else -> tvPoints.setBackgroundResource(R.drawable.bg_accent_rounded)
                }
            }
        })

        model.selectedDistrict.observe(this, Observer {
            tvTitle.text = it.displayName
            progress.max = it.totalStreets
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                progress.setProgress(it.solvedStreets, true)
            } else {
                progress.progress = it.solvedStreets
            }

            if (model.currentGameState.value == MapsViewModel.STATE_GUESSING) {
                refocusMap()
            }
        })

        confirm.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.STREET_SOLVED, null)
            if (model.solve()) {
                FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.ANSWERED_CORRECTLY, null)
                tvSuccess.setText(MessageUtil().getRandomSuccessMessage())
                rlSuccess.visibility = View.VISIBLE
                YoYo.with(Techniques.StandUp).duration(500).playOn(rlSuccess)
                scheduleToHideNotifications()
                zoomToCurrentSelection()
            } else {
                FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.ANSWERED_WRONGLY, null)
                rlError.visibility = View.VISIBLE
                tvErrorMessage.setText(MessageUtil().getRandomErrorMessage())
                tvErrorDetail.text = getString(R.string.solution_incorrect_detail, model.getSelectedStreetName())
                YoYo.with(Techniques.StandUp).duration(500).playOn(rlError)
                scheduleToHideNotifications()
            }
        }

        btnNext.setOnClickListener {
            if (mayClickMap) {
                model.subtractPoints()
                FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.STREET_SKIPPED, null)
            }
            model.startNewRound()
        }

        btnSolution.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.STREET_SOLUTION, null)
            model.solveRound {
                zoomToCurrentSelection()
            }
        }

        btnMore.setOnClickListener {
            MapsPopupMenu(this, it).show()
        }

        tvPoints.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_points_message)
                    .setPositiveButton(R.string.all_ok, null)
                    .show()
        }

        tvTitle.setOnClickListener {
            startActivity(Intent(this, DistrictPickerActivity::class.java))
        }

        registerReceiver(receiver, IntentFilter(Constants.Broadcasts.ACTION_UPDATE_DONE))
    }

    private fun refocusMap() {
        model.selectedDistrict.value?.let {
            it.geoBounds?.let { bound ->
                val bounds = LatLngBounds.Builder()
                        .include(LatLng(bound.east.latitude, bound.east.longitude))
                        .include(LatLng(bound.south.latitude, bound.south.longitude))
                        .include(LatLng(bound.west.latitude, bound.west.longitude))
                        .include(LatLng(bound.north.latitude, bound.north.longitude)).build()
                map?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 10))
                return
            }
        }
        map?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(47.0707, 15.4395), 11.0))
    }

    override fun onResume() {
        super.onResume()
        model.setDistrictId(Settings(this).getDistrict())
    }

    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            // May happen sometimes
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapComponent.handleLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapComponent.handleSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {

        map = mapboxMap

        map!!.setStyle("mapbox://styles/moopat/cjndg38970vq62rnu542m5fhf") { style ->
            val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(mutableListOf())

            selectionSource = GeoJsonSource("selection-source", featureCollection)
            style.addSource(selectionSource!!)

            selectionLayer = LineLayer("selection-layer", "selection-source")
            selectionLayer?.setProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineOpacity(1f),
                    PropertyFactory.lineColor(Color.parseColor("#ffffff"))
            )
            style.addLayer(selectionLayer!!)

            YoYo.with(Techniques.FadeOut).onEnd { rlLoading.visibility = View.GONE }.playOn(rlLoading)
        }

        map!!.uiSettings.isLogoEnabled = true
        map!!.uiSettings.isAttributionEnabled = true
        map!!.uiSettings.isRotateGesturesEnabled = false
        map!!.uiSettings.isCompassEnabled = false

        try {

            doAsync {
                model.featureCollection = GeoJSON.parse(resources.openRawResource(R.raw.graz_geodata)) as FeatureCollection
            }

            map!!.addOnMapClickListener { latLng ->
                if (!mayClickMap) {
                    startBounceAnimation(0)
                    return@addOnMapClickListener true
                }
                hideNotificationsNow()
                model.makeSelectionByCoordinates(latLng)
                return@addOnMapClickListener true
            }

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        refocusMap()
    }

    private fun startBounceAnimation() {
        startBounceAnimation(2000)
    }

    private fun startBounceAnimation(delay: Long) {
        stopBounceAnimation()
        bounceAnimation = YoYo.with(Techniques.Bounce).delay(delay).playOn(btnNext)
    }

    private fun stopBounceAnimation() {
        bounceAnimation?.stop()
    }

    private fun scheduleToHideNotifications() {
        handler.postDelayed({ hideNotificationsNow() }, 5000)
    }

    private fun hideNotificationsNow() {
        handler.removeCallbacksAndMessages(null)
        YoYo.with(Techniques.FadeOut).duration(500).onEnd { rlSuccess.visibility = View.INVISIBLE }.playOn(rlSuccess)
        YoYo.with(Techniques.FadeOut).duration(500).onEnd { rlError.visibility = View.INVISIBLE }.playOn(rlError)
    }

    private fun zoomToCurrentSelection() {
        val builder = LatLngBounds.Builder()
        lines.asSequence()
                .map { geo ->
                    if (geo is LineString) {
                        return@map geo.positions.map { LatLng(it.latitude, it.longitude) }
                    }
                    return@map null
                }
                .filterNotNull()
                .toList()
                .flatMap { it }
                .forEach {
                    builder.include(it)
                }
        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_TERMS && resultCode != RESULT_OK) {
            FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.TERMS_DECLINED, null)
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
