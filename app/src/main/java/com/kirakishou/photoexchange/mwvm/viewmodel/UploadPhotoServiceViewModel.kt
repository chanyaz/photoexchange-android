package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.wires.errors.UploadPhotoServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.UploadPhotoServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.UploadPhotoServiceOutputs
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.other.UploadedPhoto
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoToBeUploaded
import com.kirakishou.photoexchange.mwvm.model.exception.ApiException
import com.kirakishou.photoexchange.mwvm.model.net.response.StatusResponse
import com.kirakishou.photoexchange.mwvm.model.net.response.UploadPhotoResponse
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/4/2017.
 */


class UploadPhotoServiceViewModel(
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider,
        private val uploadedPhotosRepo: UploadedPhotosRepository
) : UploadPhotoServiceInputs,
        UploadPhotoServiceOutputs,
        UploadPhotoServiceErrors {

    val inputs: UploadPhotoServiceInputs = this
    val outputs: UploadPhotoServiceOutputs = this
    val errors: UploadPhotoServiceErrors = this

    private val compositeDisposable = CompositeDisposable()
    private val compositeJob = CompositeJob()
    private val MAX_ATTEMPTS = 3

    private val sendPhotoResponseSubject = PublishSubject.create<UploadedPhoto>()
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    override fun uploadPhoto(photoFilePath: String, location: LonLat, userId: String) {
        compositeJob += async {
            try {
                val response = repeatRequest(MAX_ATTEMPTS, PhotoToBeUploaded(photoFilePath, location, userId)) { arg ->
                    apiClient.sendPhoto(arg).await()
                }

                if (response == null) {
                    unknownErrorSubject.onNext(ApiException(ServerErrorCode.UNKNOWN_ERROR))
                    return@async
                }

                val errorCode = ServerErrorCode.from(response.serverErrorCode)
                Timber.d("Received response, serverErrorCode: $errorCode")

                if (errorCode != ServerErrorCode.OK) {
                    badResponseSubject.onNext(errorCode)
                    return@async
                }

                val photoId = uploadedPhotosRepo.saveOne(location.lon, location.lat, userId, photoFilePath, response.photoName).await()
                val uploadedPhoto = UploadedPhoto(photoId, location.lon, location.lat, userId, response.photoName, photoFilePath)

                sendPhotoResponseSubject.onNext(uploadedPhoto)
            } catch (error: Throwable) {
                unknownErrorSubject.onNext(error)
            }
        }
    }

    fun cleanUp() {
        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
        Timber.d("UploadPhotoServiceViewModel cleanUp")
    }

    private suspend fun <Argument, Result> repeatRequest(maxAttempts: Int, arg: Argument, block: suspend (arg: Argument) -> Result): Result? {
        var attempts = maxAttempts
        var response: Result? = null

        while (attempts-- > 0) {
            try {
                response = block(arg)
                return response!!
            } catch (error: Throwable) {
                Timber.d(error)
            }
        }

        return null
    }

    override fun onSendPhotoResponseObservable(): Observable<UploadedPhoto> = sendPhotoResponseSubject
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}



















