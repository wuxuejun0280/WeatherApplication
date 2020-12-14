package ee.ut.cs.adapter


import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.Adapter
import ee.ut.cs.bean.City
import ee.ut.cs.db.LocalCityDB
import ee.ut.cs.weatherapplication.MenuActivity
import ee.ut.cs.weatherapplication.R
import ee.ut.cs.weatherapplication.SearchActivity
import kotlinx.android.synthetic.main.menu_item.view.*
import kotlinx.android.synthetic.main.search_item.view.textView


class MenuAdapter(val dataset: ArrayList<City>, val activity: MenuActivity) : Adapter<SearchViewHolder>() {
    private lateinit var view: View

    val TAG = MenuAdapter::class.java.name

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        view = LayoutInflater.from(parent.context).inflate(R.layout.menu_item, parent, false)
        val viewHolder = SearchViewHolder(view)
        return viewHolder
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val city = dataset[position]
        holder.item.textView.text = city.cityName
        holder.item.delete_icon.setOnClickListener {
            if (dataset[position].cityName == activity.setting.cityName){
                activity.cityDeleted = true
            }

            val db = LocalCityDB.getInstance(activity.applicationContext)
            db.getCityDao().deleteCity(dataset[position])
            activity.getCities()

        }
        holder.item.textView.setOnClickListener {
            val data = Intent().apply {
                putExtra(SearchActivity.CITY_NAME, dataset[position].cityName)
                putExtra(SearchActivity.CITY_LATITUDE, dataset[position].latitude)
                putExtra(SearchActivity.CITY_LONGITUDE, dataset[position].longitude)
            }
            activity.setResult(Activity.RESULT_OK, data);
            activity.finish()
        }
        if (city.cityName==activity.setting.cityName){
            holder.item.delete_icon.isClickable = false
            holder.item.delete_icon.visibility = View.INVISIBLE
        } else {
            holder.item.delete_icon.isClickable = true
            holder.item.delete_icon.visibility = View.VISIBLE
        }

    }


}