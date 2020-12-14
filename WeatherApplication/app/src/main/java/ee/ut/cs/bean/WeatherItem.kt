package ee.ut.cs.weatherapplication.weatherapplication

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class WeatherItem(
    var place: String,
    var temperature: Double = 0.0,
    var tempmax: Double = 0.0,
    var tempmin: Double = 0.0,
    var weather: String = "N/A",
    var weatherdesc: String = "N/A",
    var sunrise: String = "N/A",
    var humidity: Int = 0,
    var wind: Double = 0.0,
    var description: String = "") : Parcelable {


//    override fun toString(): String {
//        return "Weather in $place: $temperature C, max $tempmax C, min $tempmin C \n" +
//                "Weather: $weather($weatherdesc), sunrise at: $sunrise, humidity: $humidity%, wind: $wind m/s"
//    }
}
