package com.kirakishou.photoexchange.mwvm.model.other

/**
 * Created by kirakishou on 11/10/2017.
 */
class TakenPhoto private constructor(
        val id: Long,
        val location: LonLat,
        val photoFilePath: String,
        val userId: String,
        var photoName: String
) {

    fun isEmpty(): Boolean {
        return id == -1L
    }

    fun copy(id: Long): TakenPhoto {
        return TakenPhoto(id, location, photoFilePath, userId, photoName)
    }

    companion object {
        fun empty(): TakenPhoto {
            return TakenPhoto(-1L, LonLat.empty(), "", "", "")
        }

        fun create(location: LonLat, photoFilePath: String, userId: String): TakenPhoto {
            return TakenPhoto(-1L, location, photoFilePath, userId, "")
        }

        fun create(id: Long, location: LonLat, photoFilePath: String, userId: String, photoName: String): TakenPhoto {
            return TakenPhoto(id, location, photoFilePath, userId, photoName)
        }
    }
}