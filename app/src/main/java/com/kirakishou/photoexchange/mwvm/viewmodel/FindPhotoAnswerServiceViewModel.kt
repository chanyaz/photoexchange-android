package com.kirakishou.photoexchange.mwvm.viewmodel

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.wires.errors.FindPhotoAnswerServiceErrors
import com.kirakishou.photoexchange.mwvm.wires.inputs.FindPhotoAnswerServiceInputs
import com.kirakishou.photoexchange.mwvm.wires.outputs.FindPhotoAnswerServiceOutputs
import com.kirakishou.photoexchange.mwvm.model.dto.PhotoAnswerReturnValue
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/12/2017.
 */
class FindPhotoAnswerServiceViewModel(
        private val photoAnswerRepo: PhotoAnswerRepository,
        private val apiClient: ApiClient,
        private val schedulers: SchedulerProvider
) : FindPhotoAnswerServiceInputs,
    FindPhotoAnswerServiceOutputs,
    FindPhotoAnswerServiceErrors {

    val inputs: FindPhotoAnswerServiceInputs = this
    val outputs: FindPhotoAnswerServiceOutputs = this
    val errors: FindPhotoAnswerServiceErrors = this

    private val compositeDisposable = CompositeDisposable()

    //inputs
    private val findPhotoAnswerSubject = PublishSubject.create<String>()

    //outputs
    private val uploadMorePhotosSubject = PublishSubject.create<Unit>()
    private val couldNotMarkPhotoAsReceivedSubject = PublishSubject.create<Unit>()
    private val noPhotosToSendBackSubject = PublishSubject.create<Unit>()
    private val userHasNoUploadedPhotosSubject = PublishSubject.create<Unit>()
    private val onPhotoAnswerFoundSubject = PublishSubject.create<PhotoAnswerReturnValue>()

    //errors
    private val badResponseSubject = PublishSubject.create<ServerErrorCode>()
    private val unknownErrorSubject = PublishSubject.create<Throwable>()

    init {
        fun setUpFindPhotoAnswer() {
            val userIdObservable = findPhotoAnswerSubject
                    .share()

            val responseObservable = userIdObservable
                    .subscribeOn(schedulers.provideIo())
                    .observeOn(schedulers.provideIo())
                    .flatMap { userId -> apiClient.findPhotoAnswer(userId).toObservable() }
                    .share()

            val responseErrorCode = responseObservable
                    .map { ServerErrorCode.from(it.serverErrorCode) }
                    .share()

            val photoAnswerReturnValueObservable = responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.OK }
                    .zipWith(responseObservable)
                    .map { it.second }
                    .map { response ->
                        val photoAnswer = PhotoAnswer.fromPhotoAnswerJsonObject(response.photoAnswer)
                        return@map PhotoAnswerReturnValue(photoAnswer, response.allFound)
                    }
                    .share()

            val markPhotoResponseObservable = photoAnswerReturnValueObservable
                    .zipWith(userIdObservable)
                    .flatMap {
                        val userId = it.second
                        val photoAnswer = it.first.photoAnswer

                        return@flatMap apiClient.markPhotoAsReceived(photoAnswer.photoRemoteId, userId)
                                .toObservable()
                    }
                    .share()

            compositeDisposable += markPhotoResponseObservable
                    .map { ServerErrorCode.from(it.serverErrorCode) }
                    .filter { errorCode -> errorCode != ServerErrorCode.OK }
                    .map { Unit }
                    .subscribe(couldNotMarkPhotoAsReceivedSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += markPhotoResponseObservable
                    .map { ServerErrorCode.from(it.serverErrorCode) }
                    .filter { errorCode -> errorCode == ServerErrorCode.OK }
                    .zipWith(photoAnswerReturnValueObservable)
                    .map { it.second }
                    .flatMap { photoAnswerRetValue ->
                        return@flatMap photoAnswerRepo.saveOne(photoAnswerRetValue.photoAnswer)
                                .toObservable()
                    }
                    .doOnNext {
                        if (Constants.isDebugBuild) {
                            val allPhotoAnswers = photoAnswerRepo.findAll().blockingGet()
                            allPhotoAnswers.forEach { Timber.d(it.toString()) }
                        }
                    }
                    .zipWith(photoAnswerReturnValueObservable)
                    .map { it.second }
                    .subscribe(onPhotoAnswerFoundSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS }
                    .map { Unit }
                    .subscribe(userHasNoUploadedPhotosSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.NO_PHOTOS_TO_SEND_BACK }
                    .map { Unit }
                    .subscribe(noPhotosToSendBackSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode == ServerErrorCode.UPLOAD_MORE_PHOTOS }
                    .map { Unit }
                    .subscribe(uploadMorePhotosSubject::onNext, unknownErrorSubject::onNext)

            compositeDisposable += responseErrorCode
                    .filter { errorCode -> errorCode != ServerErrorCode.OK }
                    .filter { errorCode -> errorCode != ServerErrorCode.USER_HAS_NO_UPLOADED_PHOTOS }
                    .filter { errorCode -> errorCode != ServerErrorCode.NO_PHOTOS_TO_SEND_BACK }
                    .filter { errorCode -> errorCode != ServerErrorCode.UPLOAD_MORE_PHOTOS }
                    .subscribe(badResponseSubject::onNext, unknownErrorSubject::onNext)
        }

        setUpFindPhotoAnswer()
    }

    override fun findPhotoAnswer(userId: String) {
        findPhotoAnswerSubject.onNext(userId)
    }

    fun cleanUp() {
        compositeDisposable.clear()

        Timber.d("FindPhotoAnswerServiceViewModel detached")
    }

    //outputs
    override fun uploadMorePhotosObservable(): Observable<Unit> = uploadMorePhotosSubject
    override fun couldNotMarkPhotoAsReceivedObservable(): Observable<Unit> = couldNotMarkPhotoAsReceivedSubject
    override fun userHasNoUploadedPhotosObservable(): Observable<Unit> = userHasNoUploadedPhotosSubject
    override fun noPhotosToSendBackObservable(): Observable<Unit> = noPhotosToSendBackSubject
    override fun onPhotoAnswerFoundObservable(): Observable<PhotoAnswerReturnValue> = onPhotoAnswerFoundSubject

    //errors
    override fun onBadResponseObservable(): Observable<ServerErrorCode> = badResponseSubject
    override fun onUnknownErrorObservable(): Observable<Throwable> = unknownErrorSubject
}