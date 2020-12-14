package ee.ut.cs.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.google.gson.JsonObject
import com.koushikdutta.ion.Ion
import ee.ut.cs.bean.City
import ee.ut.cs.db.LocalCityDB
import ee.ut.cs.weatherapplication.R
import ee.ut.cs.weatherapplication.SearchActivity
import kotlinx.android.synthetic.main.search_item.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SearchAdapter(val dataset: ArrayList<City>, val applicationContext: Context, val googleAPIKey:String, val activity: SearchActivity) : Adapter<SearchViewHolder>() {
    private lateinit var view: View

    val TAG = SearchAdapter::class.java.name

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        view = LayoutInflater.from(parent.context).inflate(R.layout.search_item, parent, false)
        val viewHolder = SearchViewHolder(view)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val city = dataset[position]
        holder.item.textView.text = city.cityName
        holder.item.setOnClickListener {
//            db.getCityDao().insertCity(dataset.get(position))
//            val citiestemp = db.getCityDao().loadCities()
//            Log.i(TAG, citiestemp[0].cityName)
            getLatLong(dataset[position])
        }

    }

    fun getLatLong(city: City){
        val uri = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?" +
                "input=${city.cityName.replace(" ","")}" +
                "&inputtype=textquery" +
                "&fields=name,geometry" +
                "&language=en" +
                "&key=$googleAPIKey"
        Ion.with(applicationContext)
            .load(uri)
            .asJsonObject()
            .setCallback { e, result ->
                result?.apply {updateCityDB(result, city.cityName)}
            }
    }

    fun updateCityDB(result: JsonObject, cityName:String){
        val db = LocalCityDB.getInstance(applicationContext)
        val cityJson = result
            .get("candidates").asJsonArray
            .get(0).asJsonObject
        val cityLatitude = cityJson
            .get("geometry").asJsonObject
            .get("location").asJsonObject
            .get("lat").asString
        val cityLongitued = cityJson
            .get("geometry").asJsonObject
            .get("location").asJsonObject
            .get("lng").asString
        db.getCityDao().insertCity(City(cityName,cityLatitude,cityLongitued))
        activity.finish()

    }

}