package at.trycatch.streets.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import at.trycatch.streets.model.Street

/**
 * @author Markus Deutsch <markus@moop.at>
 */
@Dao
interface StreetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(street: Street)

    @Query("SELECT * FROM street WHERE id = :streetId AND cityId = :cityId")
    fun findStreet(streetId: String, cityId: String): Street?

    @Query("SELECT * FROM street WHERE cityId = :cityId ORDER BY id ASC LIMIT 1 OFFSET :index")
    fun findStreetByIndex(cityId: String, index: Int): Street?

    @Query("SELECT s.* FROM street s LEFT JOIN streettodistrict d ON s.id = d.districtId WHERE d.districtId = :districtId AND s.cityId = :cityId ORDER BY s.id ASC LIMIT 1 OFFSET :index")
    fun findStreetByIndex(cityId: String, districtId: String, index: Int): Street?

    @Query("SELECT * FROM street WHERE cityId = :cityId")
    fun findAll(cityId: String): List<Street>

    @Query("SELECT s.* FROM street s LEFT JOIN streettodistrict d ON s.id = d.districtId WHERE d.districtId = :districtId AND s.cityId = :cityId GROUP BY s.id")
    fun findAll(cityId: String, districtId: String): List<Street>

    @Query("UPDATE street SET flaggedForDeletion = 1 WHERE cityId = :cityId")
    fun flagStreetsAsDeletable(cityId: String)

    @Query("DELETE FROM street WHERE flaggedForDeletion = 1 AND cityId = :cityId")
    fun deleteFlagged(cityId: String)

}
