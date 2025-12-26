package com.selfcontrol.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selfcontrol.data.local.entity.SettingsEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider

class DatabaseCallback @Inject constructor(
    private val database: Provider<SelfControlDatabase>,
    private val scope: CoroutineScope
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        
        // Seed initial data
        scope.launch(Dispatchers.IO) {
            populateDatabase()
        }
    }

    private suspend fun populateDatabase() {
        val db = database.get()
        
        // Check if settings exist, if not create default
        val settingsDao = db.settingsDao()
        if (settingsDao.getSettings() == null) {
            settingsDao.insertSettings(
                SettingsEntity(
                    deviceId = UUID.randomUUID().toString()
                )
            )
        }
    }
}
