package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.PhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/11/2018.
 */

@Module
open class PhotosActivityModule(
    val activity: PhotosActivity
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                takenPhotosRepository: TakenPhotosRepository,
                                uploadedPhotosRepository: UploadedPhotosRepository,
                                galleryPhotoRepository: GalleryPhotoRepository,
                                receivedPhotosRepository: ReceivedPhotosRepository,
                                settingsRepository: SettingsRepository,
                                galleryPhotosUseCase: GetGalleryPhotosUseCase,
                                favouritePhotoUseCase: FavouritePhotoUseCase,
                                getUploadedPhotosUseCase: GetUploadedPhotosUseCase,
                                getReceivedPhotosUseCase: GetReceivedPhotosUseCase,
                                reportPhotoUseCase: ReportPhotoUseCase): PhotosActivityViewModelFactory {
        return PhotosActivityViewModelFactory(
            takenPhotosRepository,
            uploadedPhotosRepository,
            galleryPhotoRepository,
            settingsRepository,
            receivedPhotosRepository,
            galleryPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            getUploadedPhotosUseCase,
            getReceivedPhotosUseCase,
            schedulerProvider)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: PhotosActivityViewModelFactory): PhotosActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(PhotosActivityViewModel::class.java)
    }
}