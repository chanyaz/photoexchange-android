package com.kirakishou.photoexchange.mvvm.model.event

import com.kirakishou.photoexchange.mvvm.model.other.EventType
import com.kirakishou.photoexchange.mvvm.model.other.UploadedPhoto

/**
 * Created by kirakishou on 11/7/2017.
 */
class PhotoUploadedEvent(
        val type: EventType,
        val status: SendPhotoEventStatus,
        val photo: UploadedPhoto?

) : BaseEvent() {

    companion object {
        fun success(photo: UploadedPhoto?): PhotoUploadedEvent {
            return PhotoUploadedEvent(EventType.UploadPhoto, SendPhotoEventStatus.SUCCESS, photo)
        }

        fun fail(): PhotoUploadedEvent {
            return PhotoUploadedEvent(EventType.UploadPhoto, SendPhotoEventStatus.FAIL, null)
        }
    }
}

