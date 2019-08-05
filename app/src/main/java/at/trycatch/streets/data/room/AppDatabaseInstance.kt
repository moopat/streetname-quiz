package at.trycatch.streets.data.room

import android.content.Context
import androidx.room.Room
import at.trycatch.streets.util.SingletonHolder

/**
 * @author Markus Deutsch <markus@moop.at>
 */
class AppDatabaseInstance private constructor(private val context: Context) {

    companion object : SingletonHolder<AppDatabaseInstance, Context>(::AppDatabaseInstance)

    val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "snq").build()
}
