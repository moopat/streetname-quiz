package at.trycatch.streets.data.room.dao

import androidx.lifecycle.LiveData
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

    @Query("SELECT * FROM district WHERE cityId = :cityId ORDER BY sequence ASC")
    fun findAll(cityId: String): List<District>

    @Query("SELECT * FROM district WHERE cityId = :cityId ORDER BY sequence ASC")
    fun findAllLive(cityId: String): LiveData<List<District>>

    @Query("SELECT COUNT(*) FROM street WHERE cityId = :cityId AND consecutiveCorrectGuesses > 0")
    fun getNumberOfSolvedStreets(cityId: String): Int

    @Query("SELECT COUNT(*) FROM streettodistrict std LEFT JOIN street s ON s.id = std.streetId WHERE s.cityId = :cityId AND std.districtId = :districtId AND consecutiveCorrectGuesses > 0")
    fun getNumberOfSolvedStreets(cityId: String, districtId: String): Int

    @Query("SELECT COUNT(*) FROM street WHERE cityId = :cityId")
    fun getTotalNumberOfStreets(cityId: String): Int

    @Query("SELECT COUNT(*) FROM streettodistrict std LEFT JOIN street s ON s.id = std.streetId WHERE s.cityId = :cityId AND std.districtId = :districtId")
    fun getTotalNumberOfStreets(cityId: String, districtId: String): Int


}
