package at.trycatch.streets.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import android.os.Bundle
import com.mapbox.mapboxsdk.maps.MapView

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class MapboxLifecycleObserver(private val map: MapView, savedInstanceState: Bundle?) : LifecycleObserver {

    init {
        map.onCreate(savedInstanceState)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun handleStart() {
        map.onStart()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun handleResume() {
        map.onResume()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun handlePause() {
        map.onPause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun handleStop() {
        map.onStop()
    }

    fun handleLowMemory() {
        map.onLowMemory()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun handleDestroy() {
        map.onDestroy()
    }

    fun handleSaveInstanceState(outState: Bundle?) {
        if (outState != null) map.onSaveInstanceState(outState)
    }


}