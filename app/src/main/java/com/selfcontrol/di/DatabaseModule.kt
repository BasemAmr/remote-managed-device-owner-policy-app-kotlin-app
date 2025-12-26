package com.selfcontrol.di

import android.content.Context
import androidx.room.Room
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
        .fallbackToDestructiveMigration() // For dev phase
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
}
