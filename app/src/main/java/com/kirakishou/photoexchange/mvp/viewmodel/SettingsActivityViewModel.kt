package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.RestoreAccountUseCase
import com.kirakishou.photoexchange.mvp.model.other.Constants.DOMAIN_NAME
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

class SettingsActivityViewModel(
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val restoreAccountUseCase: RestoreAccountUseCase
) : BaseViewModel() {
    private val TAG = "SettingsActivityViewModel"


    fun resetMakePublicPhotoOption(): Observable<Unit> {
        return Observable.fromCallable { settingsRepository.saveMakePublicFlag(null) }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun getUserId(): Single<String> {
        return Single.fromCallable { settingsRepository.getUserId() }
            .subscribeOn(schedulerProvider.IO())
            .doOnError { Timber.tag(TAG).e(it) }
    }

    fun restoreOldAccount(oldUserId: String): Single<Either<ErrorCode.CheckAccountExistsErrors, Boolean>> {
        val suffix = "@${DOMAIN_NAME}"

        val userId = if (!oldUserId.endsWith(suffix, true)) {
            oldUserId + suffix
        } else {
            oldUserId
        }

        return restoreAccountUseCase.restoreAccount(userId)
    }
}