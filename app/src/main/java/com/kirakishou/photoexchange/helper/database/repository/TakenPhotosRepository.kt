package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.dao.TakenPhotosDao
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider

/**
 * Created by kirakishou on 11/8/2017.
 */
class TakenPhotosRepository(
        private val database: MyDatabase,
        private val schedulers: SchedulerProvider
) {
    private val takenPhotosDao: TakenPhotosDao by lazy { database.takenPhotosDao() }


}