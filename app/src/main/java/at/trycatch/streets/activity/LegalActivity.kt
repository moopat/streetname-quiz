package at.trycatch.streets.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import at.trycatch.streets.R
import kotlinx.android.synthetic.main.activity_legal.*

class LegalActivity : AppCompatActivity() {

    companion object {
        const val VERSION = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)
        webView.loadUrl("file:///android_asset/legal.html")
    }
}
