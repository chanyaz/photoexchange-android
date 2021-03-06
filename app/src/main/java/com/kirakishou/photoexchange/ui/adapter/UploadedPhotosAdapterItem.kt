package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto

sealed class UploadedPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is EmptyItem -> AdapterItemType.EMPTY
            is TakenPhotoItem -> AdapterItemType.VIEW_TAKEN_PHOTO
            is UploadedPhotoItem -> AdapterItemType.VIEW_UPLOADED_PHOTO
            is ProgressItem -> AdapterItemType.VIEW_PROGRESS
            is FailedToUploadItem -> AdapterItemType.VIEW_FAILED_TO_UPLOAD
            is UploadedPhotosAdapterItem.MessageItem -> AdapterItemType.VIEW_MESSAGE
        }
    }

    class EmptyItem : UploadedPhotosAdapterItem()
    class TakenPhotoItem(val takenPhoto: TakenPhoto) : UploadedPhotosAdapterItem()
    class UploadedPhotoItem(val uploadedPhoto: UploadedPhoto) : UploadedPhotosAdapterItem()
    class ProgressItem : UploadedPhotosAdapterItem()
    class FailedToUploadItem(val failedToUploadPhoto: TakenPhoto) : UploadedPhotosAdapterItem()
    class MessageItem(val message: String) : UploadedPhotosAdapterItem()
}