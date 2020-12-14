package ee.ut.cs.weatherapplication

import android.Manifest
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting
import ee.ut.cs.db.LocalCityDB
import ee.ut.cs.services.LocationHelper
import ee.ut.cs.weatherapplication.weatherapplication.WeatherItem
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs


/**
 * Implementation of App Widget functionality.
 */
class WeatherWidget : AppWidgetProvider() {
    lateinit var setting: Setting
    lateinit var db: LocalCityDB
    lateinit var context: Context
    lateinit var timezone:String
    val weatherItemList = ArrayList<WeatherItem>()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        this.context = context
        db = LocalCityDB.getInstance(context)
        updateSetting()
        checkForPermissions()
        // There may be multiple widgets active, so update all of them
    }


    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        this.context = context
        db = LocalCityDB.getInstance(context)
        updateSetting()
        checkForPermissions()
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    fun updateAllWidget(){
        val manager = AppWidgetManager.getInstance(context)
        val appWidgetIds = manager.getAppWidgetIds(ComponentName(context, WeatherWidget::class.java))
        for (index in appWidgetIds){
            updateAppWidget(context, manager, index, this)
        }
    }

    fun updateSetting(){
        val settingTemp = db.getSettingDao().loadSetting()
        if (settingTemp.isEmpty()){
            db.getSettingDao().replaceSetting(Setting(1, 0, "", "", ""))
        }
        setting = db.getSettingDao().loadSetting()[0]
    }

    private fun checkForPermissions(){
        when{
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->{
                updateWeather()
            }
        }
    }


    fun updateWeather(){

        getTimeZone(setting.latitude, setting.longitude)
    }

    private fun getWeather(lat: String, lon: String){
        val key = context.getString(R.string.openweather_api_key)
        Ion.with(context)
            .load(
                "https://api.openweathermap.org/data/2.5/onecall?" +
                        "lat=$lat" +
                        "&lon=$lon" +
                        "&appid=$key" +
                        "&units=metric"
            )
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply {
                    if (result.get("current") != null) {
                        jsonProcessing(result, lat.toDouble(), lon.toDouble())
                    }
                }

            }
    }

    private fun jsonProcessing(json: JsonObject, latitude: Double, longitude: Double){
        weatherItemList.clear()
        val curWeather = json
            .get("current").asJsonObject

        val humidity = curWeather.get("humidity").asString
        val wind = curWeather.get("wind_speed").asString
        val temp = curWeather.get("temp").asString

        var weatherItem = WeatherItem("")
        weatherItem.temperature = temp.toDouble()
        weatherItem.humidity = humidity.toInt()
        weatherItem.wind = wind.toDouble()
        weatherItemList.add(weatherItem)

        val forcast = json
            .get("daily").asJsonArray
        forcast.forEachIndexed { index, jsonElement ->
            if (index == 0){
                weatherItem = weatherItemList[0]
            } else {
                weatherItem = WeatherItem("")
                this.weatherItemList.add(weatherItem)
            }
            weatherItem.humidity = jsonElement.asJsonObject
                .get("humidity").asInt
            weatherItem.weather = jsonElement.asJsonObject
                .get("weather").asJsonArray
                .get(0).asJsonObject
                .get("main").asString
            weatherItem.description = jsonElement.asJsonObject
                .get("weather").asJsonArray
                .get(0).asJsonObject
                .get("description").asString
            val sunrise = jsonElement.asJsonObject
                .get("sunrise").asLong*1000
            weatherItem.sunrise = sunrise.toDate(latitude, longitude)
            weatherItem.tempmax = jsonElement.asJsonObject
                .get("temp").asJsonObject
                .get("max").asDouble
            weatherItem.tempmin = jsonElement.asJsonObject
                .get("temp").asJsonObject
                .get("min").asDouble

        }
        updateAllWidget()
        Log.i("WeatherWidget", weatherItem.toString()) //what gets written to the weatheritem item
    }

    fun Long.toDate(latitude: Double, longitude: Double):String{

        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(this),
            ZoneId.of(timezone)
        )
        return "${zdt.hour}:${zdt.minute}"
    }

    fun getTimeZone(latitude: String, longitude: String){
        val googleAPIKey = context.getString(R.string.google_maps_key)
        Ion.with(context)
            .load(
                "https://maps.googleapis.com/maps/api/timezone/json?" +
                        "location=$latitude,$longitude" +
                        "&timestamp=${Instant.now().toEpochMilli() / 1000}" +
                        "&key=$googleAPIKey"
            )
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply {
                    timezone = this.get("timeZoneId").asString
                    getWeather(setting.latitude, setting.longitude)
                }
            }
    }

    fun getWeatherIcon(weatherItem:WeatherItem, big:Boolean): Int{
        when{

            weatherItem.weather == "Thunderstorm"&&big -> return R.drawable.thunderstorm_big
            weatherItem.weather == "Drizzle"&&big -> return R.drawable.rain_big
            weatherItem.weather == "Rain"&&big -> return R.drawable.rain_big
            weatherItem.weather == "Snow"&&big&&weatherItem.description=="Light rain and snow" -> return R.drawable.snow_rain_big
            weatherItem.weather == "Snow"&&big&&weatherItem.description=="Rain and snow" -> return R.drawable.snow_rain_big
            weatherItem.weather == "Snow"&&big -> return R.drawable.snow_big
            weatherItem.weather == "Mist"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Smoke"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Haze"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Dust"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Fog"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Sand"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Ash"&&big -> return R.drawable.fog_big
            weatherItem.weather == "Squall"&&big -> return R.drawable.windy_big
            weatherItem.weather == "Tornado"&&big -> return R.drawable.windy_big
            weatherItem.weather == "Clear"&&big -> return R.drawable.sunny_big
            weatherItem.weather == "Clouds"&&big&&weatherItem.description=="few clouds: 11-25%" -> return R.drawable.partly_cloudy_big
            weatherItem.weather == "Clouds"&&big&&weatherItem.description=="scattered clouds: 25-50%" -> return R.drawable.partly_cloudy_big
            weatherItem.weather == "Clouds"&&big -> return R.drawable.cloudy_big

            weatherItem.weather == "Thunderstorm" -> return R.drawable.thunderstorm
            weatherItem.weather == "Drizzle" -> return R.drawable.rain
            weatherItem.weather == "Rain" -> return R.drawable.rain
            weatherItem.weather == "Snow"&&weatherItem.description=="Light rain and snow" -> return R.drawable.snow_rain
            weatherItem.weather == "Snow"&&weatherItem.description=="Rain and snow" -> return R.drawable.snow_rain
            weatherItem.weather == "Snow" -> return R.drawable.snow
            weatherItem.weather == "Mist" -> return R.drawable.fog
            weatherItem.weather == "Smoke" -> return R.drawable.fog
            weatherItem.weather == "Haze" -> return R.drawable.fog
            weatherItem.weather == "Dust" -> return R.drawable.fog
            weatherItem.weather == "Fog" -> return R.drawable.fog
            weatherItem.weather == "Sand" -> return R.drawable.fog
            weatherItem.weather == "Ash" -> return R.drawable.fog
            weatherItem.weather == "Squall" -> return R.drawable.windy
            weatherItem.weather == "Tornado" -> return R.drawable.windy
            weatherItem.weather == "Clear" -> return R.drawable.sunny
            weatherItem.weather == "Clouds"&&weatherItem.description=="few clouds: 11-25%" -> return R.drawable.partly_cloudy
            weatherItem.weather == "Clouds"&&weatherItem.description=="scattered clouds: 25-50%" -> return R.drawable.partly_cloudy
            weatherItem.weather == "Clouds" -> return R.drawable.cloudy
        }
        return -1
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int,
    provider: WeatherWidget
) {
//    val widgetText = context.getString(R.string.appwidget_text)
    // Construct the RemoteViews object
    val weatherItem = provider.weatherItemList[0]
    val views = RemoteViews(context.packageName, R.layout.weather_widget)
    views.setTextViewText(R.id.widget_city_tv, provider.setting.cityName.split(",", ignoreCase = true)[0])
    views.setTextViewText(R.id.widget_high_low_temperature_tv, String.format(context.getString(R.string.min_max_temperature), weatherItem.tempmax, weatherItem.tempmin))
    views.setTextViewText(R.id.widget_temperature_tv, weatherItem.temperature.toInt().toString())
    views.setTextViewText(R.id.widget_weather_tv, weatherItem.weather)

    views.setImageViewResource(R.id.location_icon, R.drawable.location_icon)

    views.setImageViewResource(R.id.weather_icon, provider.getWeatherIcon(weatherItem, true))
    views.setImageViewResource(R.id.widget_day_1_image, provider.getWeatherIcon(provider.weatherItemList[0], true))
    views.setImageViewResource(R.id.widget_day_2_image, provider.getWeatherIcon(provider.weatherItemList[1], true))
    views.setImageViewResource(R.id.widget_day_3_image, provider.getWeatherIcon(provider.weatherItemList[2], true))
    views.setImageViewResource(R.id.widget_day_4_image, provider.getWeatherIcon(provider.weatherItemList[3], true))
    views.setImageViewResource(R.id.widget_day_5_image, provider.getWeatherIcon(provider.weatherItemList[4], true))
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd")).toInt()
    views.setTextViewText(R.id.widget_day_2, (date+1).toString())
    views.setTextViewText(R.id.widget_day_3, (date+2).toString())
    views.setTextViewText(R.id.widget_day_4, (date+3).toString())
    views.setTextViewText(R.id.widget_day_5, (date+4).toString())


    // Instruct the widget manager to update the widget
    appWidgetManager.updateAppWidget(appWidgetId, views)
}