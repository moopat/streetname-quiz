package at.trycatch.streets

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import at.trycatch.streets.activity.StarterActivity
import at.trycatch.streets.viewmodel.MapsViewModel
import com.cocoahero.android.geojson.FeatureCollection
import com.cocoahero.android.geojson.GeoJSON
import com.cocoahero.android.geojson.LineString
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import org.json.JSONException
import java.io.IOException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var model: MapsViewModel
    private lateinit var mMap: GoogleMap
    private val handler = Handler()
    private val lines = mutableListOf<Polyline>()
    private var bounceAnimation: YoYo.YoYoString? = null

    // Updated by changes in the game state. Ad-hoc evaluation.
    private var mayClickMap = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        startActivity(Intent(this, StarterActivity::class.java))

        model = ViewModelProviders.of(this).get(MapsViewModel::class.java)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

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
                lines.add(it.toPolyline(mMap))
            }
        })

        model.solvable.observe(this, Observer {
            if (it == true) confirm.show() else confirm.hide()
        })

        confirm.setOnClickListener {
            if (model.solve()) {
                rlSuccess.visibility = View.VISIBLE
                YoYo.with(Techniques.StandUp).duration(500).playOn(rlSuccess)
                scheduleToHideNotifications()
                zoomToCurrentSelection()
            } else {
                rlError.visibility = View.VISIBLE
                tvErrorDetail.text = getString(R.string.solution_incorrect_detail, model.getSelectedStreetName())
                YoYo.with(Techniques.StandUp).duration(500).playOn(rlError)
                scheduleToHideNotifications()
            }
        }

        btnNext.setOnClickListener {
            model.startNewRound()
        }

        btnSolution.setOnClickListener {
            model.solveRound {
                zoomToCurrentSelection()
            }
        }

        btnMore.setOnClickListener {
            val menu = PopupMenu(this, it)
            menu.inflate(R.menu.menu_main)
            menu.show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        try {
            val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map))
            if (!success) {
                Log.e("MapsActivity", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("MapsActivity", "Can't find style. Error: ", e)
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(47.0707, 15.4395), 11f))

        try {

            model.featureCollection = GeoJSON.parse(resources.openRawResource(R.raw.graz)) as FeatureCollection

            googleMap.setOnMapClickListener { latLng ->
                if (!mayClickMap) return@setOnMapClickListener
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
        stopBounceAnimation()
        bounceAnimation = YoYo.with(Techniques.Bounce).delay(2000).playOn(btnNext)
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 250))
    }

    fun LineString.toPolyline(googleMap: GoogleMap) = googleMap.addPolyline(PolylineOptions()
            .addAll(positions.asSequence().map { LatLng(it.latitude, it.longitude) }.toList())
            .width(8f)
            .color(Color.WHITE))
}
