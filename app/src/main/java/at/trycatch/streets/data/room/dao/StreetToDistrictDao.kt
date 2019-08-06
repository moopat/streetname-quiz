package at.trycatch.streets.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.trycatch.streets.model.StreetToDistrict

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Dao
interface StreetToDistrictDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: StreetToDistrict)

    @Query("DELETE FROM StreetToDistrict")
    fun deleteAll()

    @Query("DELETE FROM StreetToDistrict WHERE districtId LIKE :cityId")
    fun deleteAll(cityId: String)

}
