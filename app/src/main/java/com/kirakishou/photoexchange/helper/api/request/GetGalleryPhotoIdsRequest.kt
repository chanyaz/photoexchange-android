package com.kirakishou.photoexchange.helper.api.request

import com.google.gson.Gson
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.operator.OnApiErrorSingle
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.gson.MyGson
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

class GetGalleryPhotoIdsRequest<T>(
    private val lastId: Long,
    private val photosPerPage: Int,
    private val apiService: ApiService,
    private val schedulerProvider: SchedulerProvider,
    private val gson: MyGson
) : AbstractRequest<T>() {

    @Suppress("UNCHECKED_CAST")
    override fun execute(): Single<T> {
        return apiService.getGalleryPhotoIds(lastId, photosPerPage)
            .subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .lift(OnApiErrorSingle<GalleryPhotoIdsResponse>(gson, GalleryPhotoIdsResponse::class))
            .map { response ->
                if (ErrorCode.GetGalleryPhotosErrors.fromInt(response.serverErrorCode!!) is ErrorCode.GetGalleryPhotosErrors.Ok) {
                    return@map GalleryPhotoIdsResponse.success(response.galleryPhotoIds)
                } else {
                    return@map GalleryPhotoIdsResponse.error(ErrorCode.fromInt(ErrorCode.GetGalleryPhotosErrors::class, response.serverErrorCode!!))
                }
            }
            .onErrorReturn(this::extractError) as Single<T>
    }

    private fun extractError(error: Throwable): GalleryPhotoIdsResponse {
        return when (error) {
            is GeneralException.ApiException -> GalleryPhotoIdsResponse.error(error.errorCode as ErrorCode.GetGalleryPhotosErrors)
            is SocketTimeoutException,
            is TimeoutException -> GalleryPhotoIdsResponse.error(ErrorCode.GetGalleryPhotosErrors.LocalTimeout())
            else -> GalleryPhotoIdsResponse.error(ErrorCode.GetGalleryPhotosErrors.UnknownError())
        }
    }
}