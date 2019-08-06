package at.trycatch.streets.data

import android.content.Context
import at.trycatch.streets.activity.LegalActivity
import org.jetbrains.anko.defaultSharedPreferences

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class Settings(context: Context) {

    val preferences = context.defaultSharedPreferences

    fun hasAcceptedLatestTerms() = preferences.getInt("terms", 0) >= LegalActivity.VERSION

    fun acceptTerms() = preferences.edit().putInt("terms", LegalActivity.VERSION).apply()

    fun getPoints(): Long {
        return preferences.getLong("pts", 0)
    }

    fun addPoints(amount: Long) {
        preferences.edit().putLong("pts", getPoints() + amount).apply()
    }

    fun subtractPoints(amount: Long) {
        preferences.edit().putLong("pts", Math.max(-100, getPoints() - amount)).apply()
    }

    fun getInstalledVersion(cityId: String): Int {
        return preferences.getInt("version-$cityId", 0)
    }

    fun setInstalledVersion(cityId: String, version: Int) {
        preferences.edit().putInt("version-$cityId", version).apply()
    }

}