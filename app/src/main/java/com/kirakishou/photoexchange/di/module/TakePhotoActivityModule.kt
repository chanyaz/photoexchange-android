package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.TakePhotoActivity
import dagger.Module
import dagger.Provides
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/3/2018.
 */

@Module
open class TakePhotoActivityModule(
    val activity: TakePhotoActivity
) {

    @PerActivity
    @Provides
    open fun provideViewModelFactory(coroutinesPool: CoroutineThreadPoolProvider,
                                     photosRepository: PhotosRepository,
                                     settingsRepository: SettingsRepository): TakePhotoActivityViewModelFactory {
        return TakePhotoActivityViewModelFactory(WeakReference(activity), photosRepository, settingsRepository, coroutinesPool)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: TakePhotoActivityViewModelFactory): TakePhotoActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(TakePhotoActivityViewModel::class.java)
    }
}