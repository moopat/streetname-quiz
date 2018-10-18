package at.trycatch.streets

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import at.trycatch.streets.activity.StarterActivity
import at.trycatch.streets.data.Settings
import at.trycatch.streets.lifecycle.MapboxLifecycleObserver
import at.trycatch.streets.viewmodel.MapsViewModel
import at.trycatch.streets.widget.MapsPopupMenu
import com.cocoahero.android.geojson.FeatureCollection
import com.cocoahero.android.geojson.GeoJSON
import com.cocoahero.android.geojson.LineString
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.firebase.analytics.FirebaseAnalytics
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Polyline
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONException
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val RC_TERMS = 101
    }

    private lateinit var model: MapsViewModel
    private var map: MapboxMap? = null
    private val handler = Handler()
    private val lines = mutableListOf<Polyline>()
    private var bounceAnimation: YoYo.YoYoString? = null

    // Updated by changes in the game state. Ad-hoc evaluation.
    private var mayClickMap = true
    private lateinit var mapComponent: MapboxLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (!Settings(this).hasAcceptedLatestTerms()) {
            startActivityForResult(Intent(this, StarterActivity::class.java), RC_TERMS)
        }

        Mapbox.getInstance(this, "pk.eyJ1IjoibW9vcGF0IiwiYSI6IlNVNU5xQVEifQ.73yuoRIlKsGZ9zVBjkjSZQ")
        mapComponent = MapboxLifecycleObserver(mapView, savedInstanceState)
        lifecycle.addObserver(mapComponent)
        mapView.getMapAsync(this)

        model = ViewModelProviders.of(this).get(MapsViewModel::class.java)

        model.streetNames.observe(this, Observer {
            if (it != null) model.startNewRound()
        })

        model.initializeStreets()

        model.currentGameState.observe(this, Observer {
            if (it == null) return@Observer

            when (it) {
                MapsViewModel.STATE_GUESSING -> {
                    mayClickMap = true
                    stopBounceAnimation()

                    btnNext.setTextColor(ContextCompat.getColor(this, R.color.semiWhite))
                    btnNext.setText(R.string.button_skip)
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

        model.currentObjective.observe(this, android.arch.lifecycle.Observer {
            // We found a new objective.
            hideNotificationsNow()
            tvStreetName.text = it
        })

        model.currentlyLoading.observe(this, Observer {
            progressBar.visibility = if (it == true) View.VISIBLE else View.GONE
        })

        model.streetLines.observe(this, Observer { lineStrings ->
            lines.forEach { it.remove() }
            lines.clear()
            lineStrings?.forEach {
                lines.add(it.toPolyline(map!!))
            }
        })

        model.solvable.observe(this, Observer {
            if (it == true) confirm.show() else confirm.hide()
        })

        model.points.observe(this, Observer {
            tvPoints.text = it?.toString()
            if (it != null && it < 0) {
                tvPoints.setBackgroundResource(R.drawable.bg_red_rounded)
            } else {
                tvPoints.setBackgroundResource(R.drawable.bg_accent_rounded)
            }
        })

        confirm.setOnClickListener {
            FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.STREET_SOLVED, null)
            if (model.solve()) {
                FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.ANSWERED_CORRECTLY, null)
                rlSuccess.visibility = View.VISIBLE
                YoYo.with(Techniques.StandUp).duration(500).playOn(rlSuccess)
                scheduleToHideNotifications()
                zoomToCurrentSelection()
            } else {
                FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.ANSWERED_WRONGLY, null)
                rlError.visibility = View.VISIBLE
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapComponent.handleLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        mapComponent.handleSaveInstanceState(outState)
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {
        if (mapboxMap == null) return

        map = mapboxMap

        /*
        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map))
            if (!success) {
                Log.e("MapsActivity", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "Can't find style. Error: ", e)
        }
        */

        map!!.setStyleUrl("mapbox://styles/moopat/cjndg38970vq62rnu542m5fhf")
        map!!.uiSettings.isLogoEnabled = false
        map!!.uiSettings.isAttributionEnabled = false
        map!!.uiSettings.isRotateGesturesEnabled = false
        map!!.uiSettings.isCompassEnabled = false

        map!!.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(47.0707, 15.4395), 11.0))

        try {

            model.featureCollection = GeoJSON.parse(resources.openRawResource(R.raw.graz)) as FeatureCollection

            map!!.addOnMapClickListener { latLng ->
                if (!mayClickMap) {
                    startBounceAnimation(0)
                    return@addOnMapClickListener
                }
                hideNotificationsNow()
                model.makeSelectionByCoordinates(latLng)
            }

            model.startNewRound()

        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
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
        lines.flatMap { it.points }.forEach { builder.include(it) }
        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 250))
    }

    fun LineString.toPolyline(map: MapboxMap) = map.addPolyline(PolylineOptions()
            .addAll(positions.asSequence().map { LatLng(it.latitude, it.longitude) }.toList())
            .width(8f)
            .color(Color.WHITE))

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_TERMS && resultCode != RESULT_OK) {
            FirebaseAnalytics.getInstance(this).logEvent(Constants.Events.TERMS_DECLINED, null)
            finish()
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
