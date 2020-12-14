package ee.ut.cs.db

import androidx.room.*
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting

@Dao
interface SettingDAO {
    @Query("SELECT * FROM setting")
    fun loadSetting(): Array<Setting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun replaceSetting(vararg setting: Setting)


}