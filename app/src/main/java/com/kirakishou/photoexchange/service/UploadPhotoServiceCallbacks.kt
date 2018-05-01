package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent

/**
 * Created by kirakishou on 3/17/2018.
 */
interface UploadPhotoServiceCallbacks {
    fun onUploadingEvent(event: PhotoUploadEvent)
    fun onError(error: Throwable)
    fun stopService()
}