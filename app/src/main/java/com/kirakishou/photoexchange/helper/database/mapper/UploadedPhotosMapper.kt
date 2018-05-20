package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse

object UploadedPhotosMapper {

    object FromResponse {
        object ToEntity {
            fun toUploadedPhotoEntity(uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoData): UploadedPhotoEntity {
                return UploadedPhotoEntity.create(
                    uploadedPhotoData.photoName,
                    uploadedPhotoData.photoId,
                    uploadedPhotoData.receiverLon,
                    uploadedPhotoData.receiverLat
                )
            }

            fun toUploadedPhotoEntities(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): List<UploadedPhotoEntity> {
                return uploadedPhotoDataList.map { toUploadedPhotoEntity(it) }
            }
        }

        object ToObject {
            fun toUploadedPhoto(uploadedPhotoData: GetUploadedPhotosResponse.UploadedPhotoData): UploadedPhoto {
                return UploadedPhoto(
                    uploadedPhotoData.photoId,
                    uploadedPhotoData.photoName,
                    UploadedPhoto.ReceiverInfo(uploadedPhotoData.receiverLon, uploadedPhotoData.receiverLat)
                )
            }

            fun toUploadedPhotos(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): List<UploadedPhoto> {
                return uploadedPhotoDataList.map { toUploadedPhoto(it) }
            }
        }
    }

    object FromEntity {
        object ToObject {
            fun toUploadedPhoto(uploadedPhotoEntity: UploadedPhotoEntity): UploadedPhoto {
                return UploadedPhoto(
                    uploadedPhotoEntity.photoId!!,
                    uploadedPhotoEntity.photoName!!,
                    UploadedPhoto.ReceiverInfo(uploadedPhotoEntity.lon, uploadedPhotoEntity.lat)
                )
            }

            fun toUploadedPhotos(uploadedPhotoEntityList: List<UploadedPhotoEntity>): List<UploadedPhoto> {
                return uploadedPhotoEntityList.map { toUploadedPhoto(it) }
            }
        }
    }

    object FromObject {
        object ToEntity {
            fun toUploadedPhotoEntity(takenPhoto: TakenPhoto): UploadedPhotoEntity {
                return UploadedPhotoEntity.create(
                    takenPhoto.photoName!!,
                    takenPhoto.id
                )
            }
        }
    }
}