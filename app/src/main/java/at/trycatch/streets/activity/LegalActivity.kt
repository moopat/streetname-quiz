package at.trycatch.streets.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import at.trycatch.streets.R
import kotlinx.android.synthetic.main.activity_legal.*

class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_legal)
        webView.loadUrl("file:///android_asset/legal.html")
    }
}
