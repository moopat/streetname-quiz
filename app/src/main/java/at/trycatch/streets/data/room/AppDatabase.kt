package at.trycatch.streets.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.trycatch.streets.model.City
import at.trycatch.streets.model.District
import at.trycatch.streets.model.Street
import at.trycatch.streets.model.StreetToDistrict

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Database(entities = [City::class, District::class, Street::class, StreetToDistrict::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
}
