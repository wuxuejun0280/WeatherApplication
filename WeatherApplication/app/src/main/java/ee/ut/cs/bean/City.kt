package ee.ut.cs.bean

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize


@Entity(tableName = "city")
data class City(
    @PrimaryKey var cityName: String,
    var latitude: String,
    var longitude: String)