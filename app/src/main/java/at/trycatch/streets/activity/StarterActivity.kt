package at.trycatch.streets.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import at.trycatch.streets.R
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_starter.*

class StarterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_starter)

        Glide.with(this).load(R.drawable.bg_main).into(ivBackground)

        btnStart.setOnClickListener {
            finish()
        }

        btnLegal.setOnClickListener { startActivity(Intent(this, LegalActivity::class.java)) }

    }
}