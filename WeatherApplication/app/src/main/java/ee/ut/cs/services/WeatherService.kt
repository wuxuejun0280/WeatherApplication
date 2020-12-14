package ee.ut.cs.services

import android.app.SearchableInfo
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import ee.ut.cs.bean.City
import ee.ut.cs.broadcast.MainReceiver
import ee.ut.cs.broadcast.ServiceReceiver
import ee.ut.cs.weatherapplication.MainActivity
import ee.ut.cs.weatherapplication.R
import ee.ut.cs.weatherapplication.SearchActivity
import ee.ut.cs.weatherapplication.weatherapplication.WeatherItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import us.dustinj.timezonemap.TimeZoneMap
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime


class WeatherService : Service() {
    lateinit var city:City
    lateinit var timezone:String
    val weatherItemList = ArrayList<WeatherItem>()
    var timer = Instant.now().toEpochMilli()

    private lateinit var myReceiver: ServiceReceiver
    private lateinit var intentFilter: IntentFilter

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!this::myReceiver.isInitialized){
            myReceiver = ServiceReceiver(this)
            intentFilter = IntentFilter()
            intentFilter.addAction(MainActivity.SEND_UPDATE_REQUEST)
            registerReceiver(myReceiver, intentFilter)

            val scope = CoroutineScope(Dispatchers.Default)
            scope.launch {
                while (true) {
                    if (Instant.now().toEpochMilli() - timer > 1000 * 60 * 60 * 2) {
                        updateWeather()
                    }
                    delay(1000)
                }
            }
        }

        if (this::city.isInitialized){
            broadcastWeather()
            return super.onStartCommand(intent, flags, startId)
        } else {
            city = City("","","")
        }
        updateWeather(intent)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(myReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun updateWeather(intent: Intent?){
        val tempcity = City("","","")
        intent?.getStringExtra(SearchActivity.CITY_LATITUDE)?.apply {
            tempcity.latitude = this
        }
        intent?.getStringExtra(SearchActivity.CITY_LONGITUDE)?.apply {
            tempcity.longitude = this
        }
        intent?.getStringExtra(SearchActivity.CITY_NAME)?.apply {
            tempcity.cityName = this
        }
        if (tempcity.latitude==""||tempcity.longitude==""||tempcity.cityName==""){
            return
        }
        if (tempcity.latitude==city.latitude
            &&tempcity.longitude==city.longitude
            &&tempcity.cityName==city.cityName){
            broadcastWeather()
            return
        }
        city.cityName = tempcity.cityName
        city.longitude = tempcity.longitude
        city.latitude = tempcity.latitude
        getTimeZone(city.latitude, city.longitude)

        getWeather(city.latitude, city.longitude)
    }

    fun updateWeather(){
        if (city.latitude==""||city.longitude==""||city.cityName==""){
            return
        }
        getTimeZone(city.latitude, city.longitude)

        getWeather(city.latitude, city.longitude)
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
            weatherItem.place = city.cityName
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
        broadcastWeather()

        Log.i("jsonstuff", weatherItem.toString()) //what gets written to the weatheritem item
    }

    fun broadcastWeather(){
        val data = Intent(MainActivity.SEND_WEATHER).apply {
            putExtra(MainActivity.WEATHER, weatherItemList)
        }
        sendBroadcast(data)
    }


    fun Long.toDate(latitude: Double, longitude: Double):String{

        val zdt = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(this),
            ZoneId.of(timezone)
        )
        return "${zdt.hour}:${zdt.minute}"
    }

    fun getTimeZone(latitude: String, longitude: String){
        val googleAPIKey = getString(R.string.google_maps_key)
        Ion.with(applicationContext)
            .load(
                "https://maps.googleapis.com/maps/api/timezone/json?" +
                        "location=$latitude,$longitude" +
                        "&timestamp=${Instant.now().toEpochMilli()/1000}" +
                        "&key=$googleAPIKey"
            )
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply {
                    timezone = this.get("timeZoneId").asString
                }
            }
    }


    private fun getWeather(lat: String, lon: String){
        val key = getString(R.string.openweather_api_key)
        Ion.with(applicationContext)
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

    private fun getForecast(city: String) : JSONObject{ // to do
        val key = "625fb971cf63d7b9e7c8ff84909c55ac"
        val data = URL("https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$key").readText()
        return JSONObject(data)
    }
}