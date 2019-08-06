package at.trycatch.streets.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.trycatch.streets.model.City

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Dao
interface CityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(city: City)

    @Query("SELECT * FROM city;")
    fun getAll(): List<City>

    @Query("DELETE FROM city;")
    fun deleteAll()
}
