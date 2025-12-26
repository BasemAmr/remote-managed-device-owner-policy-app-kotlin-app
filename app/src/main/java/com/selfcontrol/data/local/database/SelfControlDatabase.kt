package com.selfcontrol.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.selfcontrol.data.local.dao.*
import com.selfcontrol.data.local.entity.*

/**
 * Main Room database for Self-Control app
 */
@Database(
    entities = [
        AppEntity::class,
        PolicyEntity::class,
        UrlEntity::class,
        RequestEntity::class,
        ViolationEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SelfControlDatabase : RoomDatabase() {
    
    abstract fun appDao(): AppDao
    abstract fun policyDao(): PolicyDao
    abstract fun urlDao(): UrlDao
    abstract fun requestDao(): RequestDao
    abstract fun violationDao(): ViolationDao
    abstract fun settingsDao(): SettingsDao
    
    companion object {
        const val DATABASE_NAME = "selfcontrol.db"
    }
}
