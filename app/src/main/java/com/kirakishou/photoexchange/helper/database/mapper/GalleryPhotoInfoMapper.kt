package com.kirakishou.photoexchange.helper.database.mapper

import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse

object GalleryPhotoInfoMapper {

    object FromResponse {
        object ToEntity {
            fun toGalleryPhotoInfoEntity(galleryPhotoInfoResponseData: GalleryPhotoInfoResponse.GalleryPhotosInfoData): GalleryPhotoInfoEntity {
                return GalleryPhotoInfoEntity.create(
                    galleryPhotoInfoResponseData.id,
                    galleryPhotoInfoResponseData.isFavourited,
                    galleryPhotoInfoResponseData.isReported
                )
            }

            fun toGalleryPhotoInfoEntityList(galleryPhotoInfoResponseDataList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): List<GalleryPhotoInfoEntity> {
                return galleryPhotoInfoResponseDataList.map { toGalleryPhotoInfoEntity(it) }
            }
        }

        object ToObject {
            fun toGalleryPhotoInfo(galleryPhotoInfoResponseData: GalleryPhotoInfoResponse.GalleryPhotosInfoData): GalleryPhotoInfo {
                return GalleryPhotoInfo(
                    galleryPhotoInfoResponseData.id,
                    galleryPhotoInfoResponseData.isFavourited,
                    galleryPhotoInfoResponseData.isReported
                )
            }

            fun toGalleryPhotoInfoList(galleryPhotoInfoResponseDataList: List<GalleryPhotoInfoResponse.GalleryPhotosInfoData>): List<GalleryPhotoInfo> {
                return galleryPhotoInfoResponseDataList.map { toGalleryPhotoInfo(it) }
            }
        }
    }

    object ToObject {
        fun toGalleryPhotoInfo(galleryPhotoInfoEntity: GalleryPhotoInfoEntity?): GalleryPhotoInfo? {
            if (galleryPhotoInfoEntity == null) {
                return null
            }

            return GalleryPhotoInfo(
                galleryPhotoInfoEntity.galleryPhotoId,
                galleryPhotoInfoEntity.isFavourited,
                galleryPhotoInfoEntity.isReported
            )
        }

        fun toGalleryPhotoInfoList(galleryPhotoInfoEntityList: List<GalleryPhotoInfoEntity?>): List<GalleryPhotoInfo> {
            return galleryPhotoInfoEntityList.mapNotNull { toGalleryPhotoInfo(it) }
        }
    }
}