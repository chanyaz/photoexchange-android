package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/17/2018.
 */

@Module
class UploadPhotoServiceModule {

    @PerService
    @Provides
    fun provideUploadPhotoServicePresenter(myTakenPhotosRepository: TakenPhotosRepository,
                                           schedulerProvider: SchedulerProvider,
                                           uploadPhotosUseCase: UploadPhotosUseCase,
                                           getUserIdUseCase: GetUserIdUseCase): UploadPhotoServicePresenter {
        return UploadPhotoServicePresenter(myTakenPhotosRepository, schedulerProvider,
            uploadPhotosUseCase, getUserIdUseCase, Constants.UPLOAD_PHOTOS_DELAY_MS)
    }
}