package ee.ut.cs.weatherapplication

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import ee.ut.cs.adapter.MenuAdapter
import ee.ut.cs.adapter.SearchAdapter
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting
import ee.ut.cs.db.LocalCityDB
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_search.*

class MenuActivity : AppCompatActivity() {

    var cityDeleted = false
    lateinit var db: LocalCityDB
    lateinit var setting: Setting
    // adapter and list for recycler view
    private lateinit var myAdapter: MenuAdapter
    var citylist = ArrayList<City>()

    val TAG = MenuActivity::class.java.name


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
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        intent?.getStringExtra(SearchActivity.CITY_NAME)?.apply {
            setting.cityName = this
        }

        setContentView(R.layout.activity_menu)
        add_icon.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivityForResult(intent, 1)
        }

        // create adapter
        myAdapter = MenuAdapter(citylist, this)
        city_recycler.layoutManager = LinearLayoutManager(this)
        city_recycler.adapter = myAdapter
        getCities()
        menu_back_icon.setOnClickListener {
            if (cityDeleted){
                val data = Intent().apply {
                    putExtra(SearchActivity.CITY_NAME, citylist[0].cityName)
                    putExtra(SearchActivity.CITY_LATITUDE, citylist[0].latitude)
                    putExtra(SearchActivity.CITY_LONGITUDE, citylist[0].longitude)
                }
                setResult(Activity.RESULT_OK, data);
            }

            finish()
        }

        mode_switch.isChecked = setting.mode==0
        mode_switch.setOnClickListener {
            if (setting.mode==0){
                setting.mode = 1
            } else {
                setting.mode = 0
            }
            LocalCityDB
                .getInstance(applicationContext)
                .getSettingDao()
                .replaceSetting(setting)
            recreate()
        }

    }

    fun getCities(){
        val db = LocalCityDB.getInstance(applicationContext)
        val cityArray = db.getCityDao().loadCities()
        citylist.clear()
        citylist.addAll(cityArray)
        myAdapter.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        if (cityDeleted){
            val data = Intent().apply {
                putExtra(SearchActivity.CITY_NAME, citylist[0].cityName)
                putExtra(SearchActivity.CITY_LATITUDE, citylist[0].latitude)
                putExtra(SearchActivity.CITY_LONGITUDE, citylist[0].longitude)
            }
            setResult(Activity.RESULT_OK, data);
        }

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        getCities()
        RESULT_OK
    }


}