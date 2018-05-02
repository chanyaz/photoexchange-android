package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.UploadPhotoData
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/17/2018.
 */
class UploadPhotoServicePresenter(
    private val callbacks: WeakReference<UploadPhotoServiceCallbacks>,
    private val myPhotosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider,
    private val updatePhotosUseCase: UploadPhotosUseCase
) {
    private val tag = "UploadPhotoServicePresenter"
    private val uploadPhotosSubject = PublishSubject.create<Unit>().toSerialized()
    private var compositeDisposable = CompositeDisposable()

    init {
        compositeDisposable += uploadPhotosSubject
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .doOnNext { Timber.tag(tag).d("before firstOrError") }
            .firstOrError()
            .doOnSuccess { Timber.tag(tag).d("after firstOrError") }
            .map {
                val userId = settingsRepository.getUserId()
                    ?: return@map UploadPhotoData.empty()
                val location = settingsRepository.getLastLocation()
                    ?: return@map UploadPhotoData.empty()

                Timber.tag(tag).d("userId = $userId, location = $location")

                return@map UploadPhotoData(false, userId, location)
            }
            .doOnError { error ->
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
                callbacks.get()?.onError(error)
                callbacks.get()?.stopService()
            }
            .doOnSuccess { data ->
                if (data.isEmpty()) {
                    callbacks.get()?.stopService()
                }
            }
            .filter { data -> !data.isEmpty() }
            .doOnEvent { _, _ -> callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnPrepare()) }
            .concatMap { data -> updatePhotosUseCase.uploadPhotos(data.userId, data.location, callbacks) }
            .doOnSuccess { callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnEnd()) }
            .doOnError { error ->
                callbacks.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
                callbacks.get()?.onError(error)
            }
            .doOnEvent { _, _ -> callbacks.get()?.stopService() }
            .subscribe()
    }

    fun onDetach() {
        this.compositeDisposable.clear()
    }

    fun uploadPhotos() {
        Timber.tag(tag).d("uploadPhotos called")
        uploadPhotosSubject.onNext(Unit)
    }
}