package at.trycatch.streets.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import at.trycatch.streets.data.room.dao.CityDao
import at.trycatch.streets.data.room.dao.DistrictDao
import at.trycatch.streets.data.room.dao.StreetDao
import at.trycatch.streets.data.room.dao.StreetToDistrictDao
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

    abstract fun getCityDao(): CityDao
    abstract fun getDistrictDao(): DistrictDao
    abstract fun getStreetToDistrictDao(): StreetToDistrictDao
    abstract fun getStreetDao(): StreetDao

}
