package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetReceivedPhotosRequest<T>(
    private val userId: String,
    private val photoIds: String,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: MyGson
) : AbstractRequest<T>() {

    override fun execute(): Single<T> {
        return apiService.getReceivedPhotos(userId, photoIds)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GetReceivedPhotosResponse>(gson, GetReceivedPhotosResponse::class))
            .map { response ->
                if (ErrorCode.GetReceivedPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GetReceivedPhotosErrors.Ok) {
                    return@map GetReceivedPhotosResponse.success(response.receivedPhotos)
                } else {
                    return@map GetReceivedPhotosResponse.fail(ErrorCode.fromInt(ErrorCode.GetReceivedPhotosErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GetReceivedPhotosResponse {
        return when (error) {
            is GeneralException.ApiException -> GetReceivedPhotosResponse.fail(error.errorCode)
            is SocketTimeoutException,
            is TimeoutException -> GetReceivedPhotosResponse.fail(ErrorCode.GetReceivedPhotosErrors.LocalTimeout())
            else -> GetReceivedPhotosResponse.fail(ErrorCode.GetReceivedPhotosErrors.UnknownError())
        }
    }
}