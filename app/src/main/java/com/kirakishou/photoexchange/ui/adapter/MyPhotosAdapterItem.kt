package com.kirakishou.photoexchange.ui.adapter

import com.kirakishou.photoexchange.mvp.model.TakenPhoto

sealed class MyPhotosAdapterItem : BaseAdapterItem() {

    override fun getType(): AdapterItemType {
        return when (this) {
            is EmptyItem -> AdapterItemType.EMPTY
            is MyPhotoItem -> AdapterItemType.VIEW_MY_PHOTO
            is ProgressItem -> AdapterItemType.VIEW_PROGRESS
            is ObtainCurrentLocationItem -> AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION
            is FailedToUploadItem -> AdapterItemType.VIEW_FAILED_TO_UPLOAD
        }
    }

    class EmptyItem : MyPhotosAdapterItem()
    class MyPhotoItem(val takenPhoto: TakenPhoto) : MyPhotosAdapterItem()
    class ProgressItem : MyPhotosAdapterItem()
    class FailedToUploadItem(val failedToUploadPhoto: TakenPhoto) : MyPhotosAdapterItem()
    class ObtainCurrentLocationItem : MyPhotosAdapterItem()
}