package ee.ut.cs.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import ee.ut.cs.weatherapplication.MainActivity
import ee.ut.cs.weatherapplication.weatherapplication.WeatherItem

class MainReceiver(var mainActivity: MainActivity) : BroadcastReceiver(){
    companion object{
        val TAG = MainReceiver::class.java.name
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.getSerializableExtra(MainActivity.WEATHER)?.apply {
            this as ArrayList<*>
            mainActivity.updateWeather(this)
        }

    }

}