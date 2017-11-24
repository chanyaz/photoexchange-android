package com.kirakishou.photoexchange.mwvm.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.viewmodel.TakePhotoActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class TakePhotoActivityViewModelFactory
@Inject constructor(
        val takenPhotosRepo: TakenPhotosRepository,
        val schedulers: SchedulerProvider
): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TakePhotoActivityViewModel(takenPhotosRepo, schedulers) as T
    }
}