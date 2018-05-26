package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity

@Dao
abstract class UploadedPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(uploadedPhotoEntityList: List<UploadedPhotoEntity>): Array<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(uploadedPhotoEntity: UploadedPhotoEntity): Long

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.REMOTE_PHOTO_ID_COLUMN} IN (:uploadedPhotoIds)")
    abstract fun findMany(uploadedPhotoIds: List<Long>): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} IN (:photoNames)")
    abstract fun findManyByPhotoName(photoNames: List<String>): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.REMOTE_PHOTO_ID_COLUMN} IN (:photoIds)")
    abstract fun findManyByRemotePhotoId(photoIds: List<Long>): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_TRUE}")
    abstract fun findAllWithReceiverInfo(): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_FALSE}")
    abstract fun findAllWithoutReceiverInfo(): List<UploadedPhotoEntity>

    @Query("SELECT ${UploadedPhotoEntity.REMOTE_PHOTO_ID_COLUMN} FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :uploadedPhotoName")
    abstract fun findByPhotoIdByName(uploadedPhotoName: String): Long

    @Query("UPDATE ${UploadedPhotoEntity.TABLE_NAME} " +
        "SET ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_TRUE} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :uploadedPhotoName")
    abstract fun updateReceiverInfo(uploadedPhotoName: String): Int

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<UploadedPhotoEntity>

    @Query("SELECT COUNT(*) FROM ${UploadedPhotoEntity.TABLE_NAME}")
    abstract fun count(): Long
}