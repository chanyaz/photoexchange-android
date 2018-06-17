package com.kirakishou.photoexchange.mvp.model.exception

import com.kirakishou.photoexchange.mvp.model.other.ErrorCode

sealed class GeneralException : Exception() {
    class ErrorCodeException(val errorCode: ErrorCode) : GeneralException()
    class ApiException(val errorCode: ErrorCode) : GeneralException()
}

sealed class UploadPhotoServiceException : Exception() {
    class CouldNotGetUserIdException(val errorCode: ErrorCode.GetUserIdError) : UploadPhotoServiceException()
}

sealed class ReceivePhotosServiceException : Exception() {
    class NoUploadedPhotos : ReceivePhotosServiceException()
    class CouldNotGetUserId : ReceivePhotosServiceException()
    class NoPhotosToSendBack : ReceivePhotosServiceException()
    class OnKnownError(val errorCode: ErrorCode.ReceivePhotosErrors) : ReceivePhotosServiceException()
}

sealed class PhotoUploadingException : Exception() {
    class PhotoDoesNotExistOnDisk : PhotoUploadingException()
    class CouldNotRotatePhoto : PhotoUploadingException()
    class DatabaseException : PhotoUploadingException()
    class ApiException(val remoteErrorCode: ErrorCode.UploadPhotoErrors) : PhotoUploadingException()
}

sealed class GetReceivedPhotosException : Exception() {
    class OnKnownError(val errorCode: ErrorCode.GetReceivedPhotosErrors) : GetReceivedPhotosException()
}