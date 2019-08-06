package at.trycatch.streets.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import at.trycatch.streets.R
import at.trycatch.streets.adapter.DistrictAdapter
import at.trycatch.streets.data.CityProvider
import at.trycatch.streets.data.DistrictProvider
import at.trycatch.streets.data.Settings
import kotlinx.android.synthetic.main.activity_district_picker.*

class DistrictPickerActivity : AppCompatActivity() {

    private val adapter = DistrictAdapter {
        settings.setDistrict(it)
        finish()
    }

    private lateinit var provider: DistrictProvider
    private lateinit var cityProvider: CityProvider
    private lateinit var settings: Settings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_district_picker)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        provider = DistrictProvider(this)
        cityProvider = CityProvider(this)
        settings = Settings(this)

        recyclerView.adapter = adapter

        provider.getAllDistrictsWithProgress("graz") {
            runOnUiThread { adapter.setDistricts(it) }
        }

        cityProvider.getCityWithProgress("graz") {
            runOnUiThread { adapter.setCity(it) }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
