package ee.ut.cs.db

import androidx.room.*
import ee.ut.cs.bean.City

@Dao
interface CityDAO {
    @Query("SELECT * FROM city")
    fun loadCities(): Array<City>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCity(vararg city: City)

    @Delete
    fun deleteCity(vararg city: City)

}