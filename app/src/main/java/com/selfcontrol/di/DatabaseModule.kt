package com.selfcontrol.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.selfcontrol.data.local.dao.*
import com.selfcontrol.data.local.database.DatabaseCallback
import com.selfcontrol.data.local.database.SelfControlDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add lockAccessibility column to policies table
            database.execSQL("ALTER TABLE policies ADD COLUMN lockAccessibility INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create api_logs table for debugging
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS api_logs (
                    id TEXT PRIMARY KEY NOT NULL,
                    url TEXT NOT NULL,
                    method TEXT NOT NULL,
                    requestBody TEXT,
                    responseCode INTEGER,
                    responseBody TEXT,
                    hasToken INTEGER NOT NULL,
                    timestamp INTEGER NOT NULL,
                    duration INTEGER NOT NULL,
                    error TEXT
                )
            """)
            database.execSQL("CREATE INDEX IF NOT EXISTS index_api_logs_timestamp ON api_logs(timestamp)")
        }
    }
    
    /**
     * Migration 4->5: Add sync queue fields to apps table for offline-first sync support
     */
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add sync status tracking columns to apps table
            database.execSQL("ALTER TABLE apps ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'SYNCED'")
            database.execSQL("ALTER TABLE apps ADD COLUMN syncRetryCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE apps ADD COLUMN lastSyncAttempt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE apps ADD COLUMN needsImmediateSync INTEGER NOT NULL DEFAULT 0")
            
            // Create index for efficient pending sync queries
            database.execSQL("CREATE INDEX IF NOT EXISTS index_apps_syncStatus ON apps(syncStatus)")
        }
    }
    
    /**
     * Migration 5→6: Add accessibility services and permissions tables
     */
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create accessibility_services table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS accessibility_services (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    service_id TEXT NOT NULL,
                    package_name TEXT NOT NULL,
                    service_name TEXT NOT NULL,
                    label TEXT NOT NULL,
                    is_enabled INTEGER NOT NULL DEFAULT 0,
                    is_locked INTEGER NOT NULL DEFAULT 0,
                    created_at INTEGER NOT NULL,
                    updated_at INTEGER NOT NULL
                )
            """)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_accessibility_services_service_id ON accessibility_services(service_id)")
            
            // Create device_permissions table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS device_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    permission_name TEXT NOT NULL,
                    is_granted INTEGER NOT NULL DEFAULT 0,
                    last_checked INTEGER NOT NULL
                )
            """)
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_device_permissions_permission_name ON device_permissions(permission_name)")
        }
    }

    @Singleton
    @Provides
    fun provideSelfControlDatabase(
        @ApplicationContext context: Context,
        callback: DatabaseCallback // We inject the callback class, BUT DatabaseCallback needs Provider<Database>
        // Use simpler approach: seed manually in Application start or use callback with Provider
    ): SelfControlDatabase {
        // Since DatabaseCallback depends on Database, we have a circular dependency if we inject Database into Callback directly.
        // We solved this by using Provider<SelfControlDatabase> in DatabaseCallback constructor.
        // However, we can't instantiate Callback easily here without Hilt creating it.
        // But Hilt creates Config, passing it to Room builder.
        
        // Actually, we can use a simpler Callback that doesn't depend on the Database instance from Hilt.
        // It's passed 'db' in onCreate.
        
        return Room.databaseBuilder(
            context,
            SelfControlDatabase::class.java,
            "selfcontrol.db"
        )
        // .addCallback(callback) // We'll skip complex callback injection for now and handle seeding elsewhere or simplify it.
        // To properly inject callback that uses Provider, we need to let Hilt provide it.
        // But Room.databaseBuilder needs the instance.
        // Let's rely on standard creation for now.
        .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
        .fallbackToDestructiveMigration() // For dev phase, but migrations take precedence
        .build()
    }
    
    // Provide DAOs
    @Provides
    fun provideAppDao(db: SelfControlDatabase): AppDao = db.appDao()
    
    @Provides
    fun providePolicyDao(db: SelfControlDatabase): PolicyDao = db.policyDao()
    
    @Provides
    fun provideUrlDao(db: SelfControlDatabase): UrlDao = db.urlDao()
    
    @Provides
    fun provideRequestDao(db: SelfControlDatabase): RequestDao = db.requestDao()
    
    @Provides
    fun provideViolationDao(db: SelfControlDatabase): ViolationDao = db.violationDao()
    
    @Provides
    fun provideSettingsDao(db: SelfControlDatabase): SettingsDao = db.settingsDao()
    
    @Provides
    fun provideApiLogDao(db: SelfControlDatabase): ApiLogDao = db.apiLogDao()
    
    @Provides
    fun provideAccessibilityServiceDao(db: SelfControlDatabase): AccessibilityServiceDao = db.accessibilityServiceDao()
    
    @Provides
    fun providePermissionDao(db: SelfControlDatabase): PermissionDao = db.permissionDao()
}
