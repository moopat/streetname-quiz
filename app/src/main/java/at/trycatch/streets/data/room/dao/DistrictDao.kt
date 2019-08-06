package at.trycatch.streets.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.trycatch.streets.model.District

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Dao
interface DistrictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(district: District)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(districts: List<District>)

    @Query("DELETE FROM district WHERE cityId = :cityId;")
    fun deleteAll(cityId: String)

    @Query("SELECT * FROM district WHERE cityId = :cityId ORDER BY sequence ASC;")
    fun findAll(cityId: String): List<District>

}
