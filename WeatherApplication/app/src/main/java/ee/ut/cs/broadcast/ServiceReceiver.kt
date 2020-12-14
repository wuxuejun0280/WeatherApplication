package ee.ut.cs.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import ee.ut.cs.services.WeatherService
import ee.ut.cs.weatherapplication.MainActivity
import ee.ut.cs.weatherapplication.weatherapplication.WeatherItem

class ServiceReceiver(var service: WeatherService) : BroadcastReceiver(){
    companion object{
        val TAG = ServiceReceiver::class.java.name
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        service.updateWeather(intent)

    }

}