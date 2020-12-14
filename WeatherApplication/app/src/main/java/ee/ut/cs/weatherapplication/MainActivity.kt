package ee.ut.cs.weatherapplication

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting
import ee.ut.cs.broadcast.MainReceiver
import ee.ut.cs.db.LocalCityDB
import ee.ut.cs.services.LocationHelper
import ee.ut.cs.services.WeatherService
import ee.ut.cs.weatherapplication.weatherapplication.WeatherItem
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    companion object{
        val TAG = MenuActivity::class.java.name
        val SEND_WEATHER = "ee.ut.cs.SEND_WEATHER"
        val SEND_UPDATE_REQUEST = "ee.ut.cs.SEND_WEATHER"
        val WEATHER = "WEATHER"
        val CITY = "CITY"
    }

    lateinit var db: LocalCityDB
    lateinit var setting: Setting

    private lateinit var myReceiver: MainReceiver
    private lateinit var intentFilter: IntentFilter

    val requestPermissions = registerForActivityResult(ActivityResultContracts.RequestPermission()){ permission -> permission
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init setting if not exist
        db = LocalCityDB.getInstance(applicationContext)
        val settingTemp = db.getSettingDao().loadSetting()
        if (settingTemp.isEmpty()){
            db.getSettingDao().replaceSetting(Setting(1, 0, "", "", ""))
        }
        setting = db.getSettingDao().loadSetting()[0]
        if (setting.mode==1){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

        setContentView(R.layout.activity_main)
        menu_icon.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java).apply {
                putExtra(SearchActivity.CITY_NAME, setting.cityName)
            }
            startActivityForResult(intent, 1)
        }

        myReceiver = MainReceiver(this)
        intentFilter = IntentFilter()
        intentFilter.addAction(SEND_WEATHER)
        registerReceiver(myReceiver, intentFilter)



        checkForPermissions()
        initDate()
//        if(savedInstanceState == null){
//            supportFragmentManager.beginTransaction().replace(R.id.container, MainFragment.newInstance()).commitNow()
//        }
    }

    override fun onResume() {
        super.onResume()
        sendBroadcast()
    }

    override fun onPause() {
        super.onPause()
        checkForPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(myReceiver)
        checkForPermissions()
    }

    //Creating menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.settings, menu)
        return true
    }

    //Selected menu item event
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.id_settings -> {
                true
            }
            else -> {true}
        }
    }


    fun initDate(){
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd")).toInt()
        day_2.text = (date+1).toString()
        day_3.text = (date+2).toString()
        day_4.text = (date+3).toString()
        day_5.text = (date+4).toString()
    }

    fun getCurrentLocation(){
        val helper = LocationHelper(applicationContext)
        val location = helper.getCurrentLocationUsingGPS()
        location?.let {
            if (setting.cityName!=""&&abs(setting.latitude.toDouble() - it.latitude)<1 && abs(setting.longitude.toDouble() - it.longitude)<1){
                val intent = Intent(this, WeatherService::class.java).apply {
                    putExtra(SearchActivity.CITY_NAME, setting.cityName)
                    putExtra(SearchActivity.CITY_LATITUDE, setting.latitude)
                    putExtra(SearchActivity.CITY_LONGITUDE, setting.longitude)
                }
                startService(intent)
                updateWidgets(applicationContext)
            }
            matchCity(location.latitude, location.longitude)
        }

    }

    fun matchCity(latitude: Double, longitude: Double){
        val cities = LocalCityDB.getInstance(applicationContext).getCityDao().loadCities()
        for (city in cities){
            if (abs(city.latitude.toDouble() - latitude)<1 && abs(city.longitude.toDouble() - longitude)<1){
                setting.cityName = city.cityName
                setting.latitude = city.latitude
                setting.longitude = city.longitude
                updateSetting()
                val intent = Intent(this, WeatherService::class.java).apply {
                    putExtra(SearchActivity.CITY_NAME, setting.cityName)
                    putExtra(SearchActivity.CITY_LATITUDE, setting.latitude)
                    putExtra(SearchActivity.CITY_LONGITUDE, setting.longitude)
                }
                startService(intent)
                return
            }
        }
        getCityName(latitude, longitude)
    }

    fun getCityName(latitude: Double, longitude: Double){
        val googleAPIKey = getString(R.string.google_maps_key)
        Ion.with(applicationContext)
            .load(
                "https://maps.googleapis.com/maps/api/geocode/json?" +
                        "latlng=${latitude.toString()},${longitude.toString()}" +
                        "&language=en" +
                        "&result_type=administrative_area_level_2" +
                        "&key=$googleAPIKey"
            )
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply { addNewCity(result)  }
            }
    }

    fun addNewCity(result: JsonObject){
        if (result.get("results").asJsonArray.size()==0){
            if (setting.cityName==""){
                setting.cityName = "Tartu, Estonia"
                setting.latitude = "58.3854"
                setting.longitude = "26.7247"
                updateSetting()
                db.getCityDao().insertCity(City(setting.cityName, setting.latitude, setting.longitude))
            }

            val intent = Intent(this, WeatherService::class.java).apply {
                putExtra(SearchActivity.CITY_NAME, setting.cityName)
                putExtra(SearchActivity.CITY_LATITUDE, setting.latitude)
                putExtra(SearchActivity.CITY_LONGITUDE, setting.longitude)
            }
            startService(intent)
            return
        }

        val cityName = result
            .get("results").asJsonArray
            .get(0).asJsonObject
            .get("formatted_address").asString
        val latitude = result
            .get("results").asJsonArray
            .get(0).asJsonObject
            .get("geometry").asJsonObject
            .get("location").asJsonObject
            .get("lat").asString
        val longitude = result
            .get("results").asJsonArray
            .get(0).asJsonObject
            .get("geometry").asJsonObject
            .get("location").asJsonObject
            .get("lng").asString
        db.getCityDao().insertCity(City(cityName, latitude.toString(), longitude.toString()))
        setting.cityName = cityName
        setting.latitude = latitude
        setting.longitude = longitude
        updateSetting()
        val intent = Intent(this, WeatherService::class.java).apply {
            putExtra(SearchActivity.CITY_NAME, setting.cityName)
            putExtra(SearchActivity.CITY_LATITUDE, setting.latitude)
            putExtra(SearchActivity.CITY_LONGITUDE, setting.longitude)
        }
        startService(intent)

    }

    fun updateSetting(){
        val settingTemp = db.getSettingDao().loadSetting()[0]
        setting.mode = settingTemp.mode
        db.getSettingDao().replaceSetting(setting)
        sendBroadcast()
    }

    fun sendBroadcast(){
        val newdata = Intent(SEND_UPDATE_REQUEST).apply {
            putExtra(SearchActivity.CITY_NAME, setting.cityName)
            putExtra(SearchActivity.CITY_LATITUDE, setting.latitude)
            putExtra(SearchActivity.CITY_LONGITUDE, setting.longitude)
        }
        sendBroadcast(newdata)
    }

    fun updateWeather(weatherItemList: ArrayList<*>){
        if (weatherItemList.size==0){
            return
        }
        val weatherItem = weatherItemList[0] as WeatherItem
        city_tv.text = setting.cityName.split(",", ignoreCase = true)[0]

            city_tv.textSize = 20F

        high_low_temperature_tv.text = String.format(
            getString(R.string.min_max_temperature),
            weatherItem.tempmax,
            weatherItem.tempmin
        )
        temperature_tv.text = weatherItem.temperature.toInt().toString()
        weather_tv.text = weatherItem.weather
        sunrise_tv.text = weatherItem.sunrise
        wind_tv.text = String.format(
            getString(R.string.wind_speed),
            weatherItem.wind.toInt().toString()
        )
        humidity_tv.text = String.format(
            getString(R.string.humidity),
            weatherItem.humidity.toString()
        )
        weather_big_icon.setImageResource(getWeatherIcon(weatherItem, true))
        day_1_image.setImageResource(getWeatherIcon(weatherItem, false))
        day_2_image.setImageResource(getWeatherIcon(weatherItemList[1] as WeatherItem, false))
        day_3_image.setImageResource(getWeatherIcon(weatherItemList[2] as WeatherItem, false))
        day_4_image.setImageResource(getWeatherIcon(weatherItemList[3] as WeatherItem, false))
        day_5_image.setImageResource(getWeatherIcon(weatherItemList[4] as WeatherItem, false))
    }

    fun getWeatherIcon(weatherItem: WeatherItem, big: Boolean): Int{
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


    //Checking permissions
    private fun checkForPermissions(){
        when{
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ->{
                getCurrentLocation()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                //This case means user previously denied permission. Explain and ask permission again
                showPermissionRequestExplanation(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    "We really do need location permission!"
                ){
                    requestPermissions.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissions.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (permissionsGranted) {
            getCurrentLocation()
        }
    }

    fun Context.showPermissionRequestExplanation(
        permission: String,
        message: String,
        retry: (() -> Unit)? = null
    ){
        AlertDialog.Builder(this).apply {
            setTitle("Note from devops!")
            setMessage(message)
            setPositiveButton("OK") { _, _ -> retry?.invoke()}
        }.show()
    }

    fun updateWidgets(context: Context) {
        val intent = Intent(context.applicationContext, WeatherWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        val widgetManager = AppWidgetManager.getInstance(context)
        val ids = widgetManager.getAppWidgetIds(
            ComponentName(
                context,
                WeatherWidget::class.java
            )
        )
        widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        intent.putExtra(SearchActivity.CITY_NAME, setting.cityName)
        context.sendBroadcast(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val cityname = data?.getStringExtra(SearchActivity.CITY_NAME)
        val latitude = data?.getStringExtra(SearchActivity.CITY_LATITUDE)
        val longitude = data?.getStringExtra(SearchActivity.CITY_LONGITUDE)
        cityname?.let {
            if (latitude != null) {
                if (longitude != null) {
                    val newdata = Intent(SEND_UPDATE_REQUEST).apply {
                        setting.cityName = it
                        setting.latitude = latitude
                        setting.longitude = longitude
                        updateSetting()
                        putExtra(SearchActivity.CITY_NAME, it)
                        putExtra(SearchActivity.CITY_LATITUDE, latitude)
                        putExtra(SearchActivity.CITY_LONGITUDE, longitude)
                    }
                    sendBroadcast(newdata)
                }
            }
        }


    }


}