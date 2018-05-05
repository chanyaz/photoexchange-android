package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.model.exception.ApiException
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetPhotoAnswersRequest<T>(
    private val photoNames: String,
    private val userId: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: Gson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return apiService.getPhotoAnswers(photoNames, userId)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<PhotoAnswerResponse>(gson, PhotoAnswerResponse::class.java))
            .map { response ->
                if (response.serverErrorCode!! == 0) {
                    return@map PhotoAnswerResponse.success(response.photoAnswers)
                } else {
                    return@map PhotoAnswerResponse.error(ErrorCode.fromInt(ErrorCode.GetPhotoAnswersErrors::class.java, response.serverErrorCode))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): PhotoAnswerResponse {
        return when (error) {
            is ApiException -> PhotoAnswerResponse.error(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> PhotoAnswerResponse.error(ErrorCode.GetPhotoAnswersErrors.Local.Timeout())
            else -> PhotoAnswerResponse.error(ErrorCode.GetPhotoAnswersErrors.Remote.UnknownError())
        }
    }
}