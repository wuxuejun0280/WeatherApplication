package ee.ut.cs.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ee.ut.cs.bean.City
import ee.ut.cs.bean.Setting

@Database(entities = [City::class, Setting::class], version = 2)
abstract class LocalCityDB : RoomDatabase() {

    companion object {
        private lateinit var cityDB : LocalCityDB

        @Synchronized fun getInstance(context: Context) : LocalCityDB {

            if (!this::cityDB.isInitialized) {
                cityDB = Room.databaseBuilder(
                    context, LocalCityDB::class.java, "myRecipes")
                    .fallbackToDestructiveMigration() // each time schema changes, data is lost!
                    .allowMainThreadQueries() // if possible, use background thread instead
                    .build()
            }
            return cityDB

        }
    }

    abstract fun getCityDao(): CityDAO
    abstract fun getSettingDao(): SettingDAO
}