package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/8/2017.
 */

@Module
class DatabaseModule(val dbName: String) {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): MyDatabase {
        return Room.databaseBuilder(context, MyDatabase::class.java, dbName).build()
    }

    @Singleton
    @Provides
    fun provideTakenPhotosRepository(database: MyDatabase, schedulers: SchedulerProvider, mapper: TakenPhotoMapper): TakenPhotosRepository {
        return TakenPhotosRepository(database, schedulers, mapper)
    }
}