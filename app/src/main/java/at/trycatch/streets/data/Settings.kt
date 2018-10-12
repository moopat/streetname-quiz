package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.activity.LegalActivity
import org.jetbrains.anko.defaultSharedPreferences

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class Settings(context: Context) {

    private val preferences = context.defaultSharedPreferences

    fun hasAcceptedLatestTerms() = preferences.getInt("terms", 0) >= LegalActivity.VERSION

    fun acceptTerms() = preferences.edit().putInt("terms", LegalActivity.VERSION).apply()

}