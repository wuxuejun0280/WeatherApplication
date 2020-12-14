package ee.ut.cs.bean

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setting")
data class Setting(
    @PrimaryKey var id: Int,
    var mode: Int,
    var cityName: String,
    var latitude: String,
    var longitude: String)