package ee.ut.cs.weatherapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import ee.ut.cs.adapter.SearchAdapter
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting
import ee.ut.cs.db.LocalCityDB
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : AppCompatActivity()  {
    lateinit var db: LocalCityDB
    lateinit var setting: Setting
    // adapter and list for recycler view
    private lateinit var myAdapter: SearchAdapter
    var citylist = ArrayList<City>()

    companion object{
        val TAG = SearchActivity::class.java.name
        val CITY_NAME = "CITY_NAME"
        val CITY_LATITUDE = "CITY_LATITUDE"
        val CITY_LONGITUDE = "CITY_LONGITUDE"

    }

    fun addCityToList(city: City) {
        citylist.add(city)
        myAdapter.notifyDataSetChanged()
    }

    fun clearList(){
        citylist.clear()
        myAdapter.notifyDataSetChanged()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // init setting if not exist
        db = LocalCityDB.getInstance(applicationContext)
        val settingTemp = db.getSettingDao().loadSetting()
        if (settingTemp.isEmpty()){
            db.getSettingDao().replaceSetting(Setting(1,0,"","",""))
        }
        setting = db.getSettingDao().loadSetting()[0]
        if (setting.mode==1){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }


        setContentView(R.layout.activity_search)
        // create adapter
        myAdapter = SearchAdapter(citylist
            , applicationContext
            , getString(R.string.google_maps_key)
            , this)
        city_list_rec.layoutManager = LinearLayoutManager(this)
        city_list_rec.adapter = myAdapter
        // create on change listener
        search_tv.doOnTextChanged { text, start, before, count ->
            clearList()
            retrivePlaceListAutomatch(text.toString())
        }
        search_back_icon.setOnClickListener {
            finish()
        }
    }

    fun retrivePlaceListAutomatch(input:String){
        val googleAPIKey = getString(R.string.google_maps_key)
        Ion.with(applicationContext)
            .load(
                "https://maps.googleapis.com/maps/api/place/autocomplete/json?" +
                        "input=$input" +
                        "&types=(cities)" +
                        "&language=en" +
                        "&key=$googleAPIKey"
            )
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply { updatePlaceList(result) }
            }
    }

    fun updatePlaceList(result: JsonObject){
        val predictions = result.get("predictions").asJsonArray
        for (prediction in predictions){
            val cityname = prediction.asJsonObject.get("description").asString
            addCityToList(City(cityname,"-1", "-1"))
            Log.i(TAG, cityname)
        }
    }


}