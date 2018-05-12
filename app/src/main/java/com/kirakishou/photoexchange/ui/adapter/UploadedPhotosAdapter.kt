package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.support.v7.widget.AppCompatButton
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import io.reactivex.subjects.Subject

/**
 * Created by kirakishou on 3/18/2018.
 */
class UploadedPhotosAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
    private val adapterButtonsClickSubject: Subject<UploadedPhotosAdapterButtonClickEvent>
) : BaseAdapter<MyPhotosAdapterItem>(context) {

    private val HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX = 0

    private val headerItems = arrayListOf<MyPhotosAdapterItem>()
    private val queuedUpItems = arrayListOf<MyPhotosAdapterItem>()
    private val failedToUploadItems = arrayListOf<MyPhotosAdapterItem>()
    private val uploadedItems = arrayListOf<MyPhotosAdapterItem>()
    private val uploadedWithReceiverInfo = arrayListOf<MyPhotosAdapterItem>()

    private val duplicatesCheckerSet = hashSetOf<Long>()
    private val photosProgressMap = hashMapOf<Long, Int>()

    init {
        headerItems.add(HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX, MyPhotosAdapterItem.EmptyItem())
    }

    fun updateUploadedPhotoSetReceiverInfo(receivedPhoto: ReceivedPhoto) {
        val uploadedItemIndex = uploadedItems.indexOfFirst { (it as MyPhotosAdapterItem.UploadedPhotoItem).uploadedPhoto.photoId == receivedPhoto.id!! }
        val uploadedPhoto = (uploadedItems[uploadedItemIndex] as MyPhotosAdapterItem.UploadedPhotoItem).uploadedPhoto

        uploadedPhoto.receiverInfo = UploadedPhoto.ReceiverInfo(receivedPhoto.lon, receivedPhoto.lat)

        uploadedItems.removeAt(uploadedItemIndex)
        uploadedWithReceiverInfo.add(0, MyPhotosAdapterItem.UploadedPhotoItem(uploadedPhoto))

        val prevElementsCount = headerItems.size + queuedUpItems.size + failedToUploadItems.size
        notifyItemMoved(prevElementsCount + uploadedItemIndex, prevElementsCount + uploadedItems.size)
    }

    fun updateAllPhotosState(newPhotoState: PhotoState) {
        for (photoIndex in 0 until queuedUpItems.size) {
            val item = queuedUpItems[photoIndex] as MyPhotosAdapterItem.TakenPhotoItem
            val failedToUploadItem = MyPhotosAdapterItem.FailedToUploadItem(item.takenPhoto)
            failedToUploadItem.failedToUploadPhoto.photoState = newPhotoState
            queuedUpItems[photoIndex] = failedToUploadItem
        }

        notifyItemRangeChanged(headerItems.size, queuedUpItems.size)
    }

    fun updatePhotoProgress(photoId: Long, newProgress: Int) {
        checkInited()

        if (!isPhotoAlreadyAdded(photoId)) {
            return
        }

        val photoIndex = getPhotoGlobalIndexById(photoId)

        photosProgressMap[photoId] = newProgress
        notifyItemChanged(photoIndex)
    }

    fun showObtainCurrentLocationNotification() {
        if (headerItems[HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] is MyPhotosAdapterItem.ObtainCurrentLocationItem) {
            return
        }

        headerItems[HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] = MyPhotosAdapterItem.ObtainCurrentLocationItem()
        notifyItemChanged(HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX)
    }

    fun hideObtainCurrentLocationNotification() {
        if (headerItems[HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] is MyPhotosAdapterItem.EmptyItem) {
            return
        }

        headerItems[HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX] = MyPhotosAdapterItem.EmptyItem()
        notifyItemChanged(HEADER_OBTAIN_CURRENT_LOCATION_NOTIFICATION_INDEX)
    }

    fun addTakenPhotos(photos: List<TakenPhoto>) {
        for (photo in photos) {
            addTakenPhoto(photo)
        }
    }

    fun addTakenPhoto(photo: TakenPhoto) {
        if (isPhotoAlreadyAdded(photo)) {
            return
        }

        duplicatesCheckerSet.add(photo.id)

        when (photo.photoState) {
            PhotoState.PHOTO_QUEUED_UP,
            PhotoState.PHOTO_UPLOADING -> {
                queuedUpItems.add(0, MyPhotosAdapterItem.TakenPhotoItem(photo))
                notifyItemInserted(headerItems.size)
            }
            PhotoState.FAILED_TO_UPLOAD -> {
                failedToUploadItems.add(0, MyPhotosAdapterItem.FailedToUploadItem(photo))
                notifyItemInserted(headerItems.size + queuedUpItems.size)
            }
            PhotoState.PHOTO_TAKEN -> {
                //Do nothing
            }
            else -> throw IllegalArgumentException("Unknown photoState: ${photo.photoState}")
        }
    }

    fun addUploadedPhotos(photos: List<UploadedPhoto>) {
        for (photo in photos) {
            addUploadedPhoto(photo)
        }
    }

    fun addUploadedPhoto(photo: UploadedPhoto) {
        if (!isPhotoAlreadyAdded(photo)) {
            return
        }

        duplicatesCheckerSet.add(photo.photoId)

        if (!photo.hasReceivedInfo()) {
            uploadedItems.add(0, MyPhotosAdapterItem.UploadedPhotoItem(photo))
            notifyItemInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size)
        } else {
            uploadedWithReceiverInfo.add(0, MyPhotosAdapterItem.UploadedPhotoItem(photo))
            notifyItemInserted(headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size)
        }
    }

    fun removePhotoById(photoId: Long) {
        if (!isPhotoAlreadyAdded(photoId)) {
            return
        }

        duplicatesCheckerSet.remove(photoId)

        var globalIndex = headerItems.size
        var localIndex = -1

        for ((index, adapterItem) in queuedUpItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.TakenPhotoItem
            if (adapterItem.takenPhoto.id == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            queuedUpItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
            return
        }

        for ((index, adapterItem) in failedToUploadItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            failedToUploadItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
            return
        }

        for ((index, adapterItem) in uploadedItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.UploadedPhotoItem
            if (adapterItem.uploadedPhoto.photoId == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            uploadedItems.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
        }

        for ((index, adapterItem) in uploadedWithReceiverInfo.withIndex()) {
            adapterItem as MyPhotosAdapterItem.UploadedPhotoItem
            if (adapterItem.uploadedPhoto.photoId == photoId) {
                localIndex = index
                break
            }

            ++globalIndex
        }

        if (localIndex != -1) {
            uploadedWithReceiverInfo.removeAt(localIndex)
            notifyItemRemoved(globalIndex)
        }
    }

    private fun getPhotoGlobalIndexById(photoId: Long): Int {
        if (!isPhotoAlreadyAdded(photoId)) {
            return -1
        }

        var index = headerItems.size

        for (adapterItem in queuedUpItems) {
            adapterItem as MyPhotosAdapterItem.TakenPhotoItem
            if (adapterItem.takenPhoto.id == photoId) {
                return index
            }

            ++index
        }

        for (adapterItem in failedToUploadItems) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                return index
            }

            ++index
        }

        for (adapterItem in uploadedItems) {
            adapterItem as MyPhotosAdapterItem.UploadedPhotoItem
            if (adapterItem.uploadedPhoto.photoId == photoId) {
                return index
            }

            ++index
        }

        for (adapterItem in uploadedWithReceiverInfo) {
            adapterItem as MyPhotosAdapterItem.UploadedPhotoItem
            if (adapterItem.uploadedPhoto.photoId == photoId) {
                return index
            }

            ++index
        }

        return -1
    }

    private fun getAdapterItemByIndex(index: Int): MyPhotosAdapterItem? {
        val headerItemsRange = IntRange(0, headerItems.size - 1)
        val queuedUpItemsRange = IntRange(headerItemsRange.endInclusive, headerItemsRange.endInclusive + queuedUpItems.size)
        val failedToUploadItemsRange = IntRange(queuedUpItemsRange.endInclusive, queuedUpItemsRange.endInclusive + failedToUploadItems.size)
        val uploadedItemsRange = IntRange(failedToUploadItemsRange.endInclusive, failedToUploadItemsRange.endInclusive + uploadedItems.size)
        val uploadedWithAnswerItemsRange = IntRange(uploadedItemsRange.endInclusive, uploadedItemsRange.endInclusive + uploadedWithReceiverInfo.size)

        return when (index) {
            in headerItemsRange -> {
                headerItems[index]
            }
            in queuedUpItemsRange -> {
                queuedUpItems[index - headerItems.size]
            }
            in failedToUploadItemsRange -> {
                failedToUploadItems[index - (headerItems.size + queuedUpItems.size)]
            }
            in uploadedItemsRange -> {
                uploadedItems[index - (headerItems.size + queuedUpItems.size + failedToUploadItems.size)]
            }
            in uploadedWithAnswerItemsRange -> {
                uploadedWithReceiverInfo[index - (headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size)]
            }
            else -> null
        }
    }

    private fun updateAdapterItemById(photoId: Long, updateFunction: (photo: TakenPhoto) -> TakenPhoto) {
        var currentIndex = headerItems.size

        if (!isPhotoAlreadyAdded(photoId)) {
            return
        }

        for ((localIndex, adapterItem) in queuedUpItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.TakenPhotoItem
            if (adapterItem.takenPhoto.id == photoId) {
                val updatedAdapterItem = updateFunction(adapterItem.takenPhoto)
                queuedUpItems[localIndex] = MyPhotosAdapterItem.TakenPhotoItem(updatedAdapterItem)
                notifyItemChanged(currentIndex)
                return
            }

            ++currentIndex
        }

        for ((localIndex, adapterItem) in failedToUploadItems.withIndex()) {
            adapterItem as MyPhotosAdapterItem.FailedToUploadItem
            if (adapterItem.failedToUploadPhoto.id == photoId) {
                val updatedAdapterItem = updateFunction(adapterItem.failedToUploadPhoto)
                failedToUploadItems[localIndex] = MyPhotosAdapterItem.FailedToUploadItem(updatedAdapterItem)
                notifyItemChanged(currentIndex)
                return
            }

            ++currentIndex
        }
    }

    private fun isPhotoAlreadyAdded(photoId: Long): Boolean {
        return duplicatesCheckerSet.contains(photoId)
    }

    private fun isPhotoAlreadyAdded(takenPhoto: TakenPhoto): Boolean {
        return isPhotoAlreadyAdded(takenPhoto.id)
    }

    private fun isPhotoAlreadyAdded(uploadedPhoto: UploadedPhoto): Boolean {
        return isPhotoAlreadyAdded(uploadedPhoto.photoId)
    }

    fun clear() {
        hideObtainCurrentLocationNotification()

        queuedUpItems.clear()
        failedToUploadItems.clear()
        uploadedItems.clear()
        uploadedWithReceiverInfo.clear()
        duplicatesCheckerSet.clear()

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return getAdapterItemByIndex(position)!!.getType().type
    }

    override fun getItemCount(): Int {
        return headerItems.size + queuedUpItems.size + failedToUploadItems.size + uploadedItems.size + uploadedWithReceiverInfo.size
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
            BaseAdapterInfo(AdapterItemType.EMPTY, R.layout.adapter_item_empty, EmptyViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_TAKEN_PHOTO, R.layout.adapter_item_taken_photo, TakenPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_TAKEN_PHOTO, R.layout.adapter_item_uploaded_photo, UploadedPhotoViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_PROGRESS, R.layout.adapter_item_progress, ProgressViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_OBTAIN_CURRENT_LOCATION_NOTIFICATION,
                R.layout.adapter_item_obtain_current_location, ObtainCurrentLocationViewHolder::class.java),
            BaseAdapterInfo(AdapterItemType.VIEW_FAILED_TO_UPLOAD,
                R.layout.adapter_failed_to_upload_photo, FailedToUploadPhotoViewHolder::class.java)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TakenPhotoViewHolder -> {
                val takenPhoto = (getAdapterItemByIndex(position) as? MyPhotosAdapterItem.TakenPhotoItem)?.takenPhoto
                    ?: return

                holder.photoidTextView.text = takenPhoto.id.toString()

                when (takenPhoto.photoState) {
                    PhotoState.PHOTO_QUEUED_UP,
                    PhotoState.PHOTO_UPLOADING,
                    PhotoState.FAILED_TO_UPLOAD -> {
                        if (takenPhoto.photoState == PhotoState.PHOTO_QUEUED_UP || takenPhoto.photoState == PhotoState.PHOTO_UPLOADING) {
                            holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploading_color))
                        } else {
                            holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_failed_to_upload_color))
                        }

                        holder.uploadingMessageHolderView.visibility = View.VISIBLE

                        takenPhoto.photoTempFile?.let { photoFile ->
                            imageLoader.loadImageFromDiskInto(photoFile, holder.photoView)
                        }

                        if (photosProgressMap.containsKey(takenPhoto.id)) {
                            holder.loadingProgress.progress = photosProgressMap[takenPhoto.id]!!
                        }
                    }
                    PhotoState.PHOTO_TAKEN -> {
                        throw IllegalStateException("photo with state PHOTO_TAKEN should not be here!")
                    }
                }
            }
            is UploadedPhotoViewHolder -> {
                val uploadedPhoto = (getAdapterItemByIndex(position) as? MyPhotosAdapterItem.UploadedPhotoItem)?.uploadedPhoto
                    ?: return

                val drawable = if (!uploadedPhoto.hasReceivedInfo()) {
                    context.getDrawable(R.drawable.ic_done)
                } else {
                    context.getDrawable(R.drawable.ic_done_all)
                }

                holder.receivedIconImageView.setImageDrawable(drawable)
                holder.photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploaded_color))
                holder.receivedIconImageView.visibility = View.VISIBLE

                uploadedPhoto.photoName.let { photoName ->
                    imageLoader.loadImageFromNetInto(photoName, ImageLoader.PhotoSize.Small, holder.photoView)
                }

                photosProgressMap.remove(uploadedPhoto.photoId)
            }
            is FailedToUploadPhotoViewHolder -> {
                val failedPhoto = (getAdapterItemByIndex(position) as? MyPhotosAdapterItem.FailedToUploadItem)?.failedToUploadPhoto
                    ?: return

                require(failedPhoto.photoState == PhotoState.FAILED_TO_UPLOAD)

                failedPhoto.photoTempFile?.let { photoFile ->
                    imageLoader.loadImageFromDiskInto(photoFile, holder.photoView)
                }

                holder.deleteFailedToUploadPhotoButton.setOnClickListener {
                    adapterButtonsClickSubject.onNext(UploadedPhotosAdapterButtonClickEvent.DeleteButtonClick(failedPhoto))
                }

                holder.retryToUploadFailedPhoto.setOnClickListener {
                    adapterButtonsClickSubject.onNext(UploadedPhotosAdapterButtonClickEvent.RetryButtonClick(failedPhoto))
                }
            }
            is ProgressViewHolder -> {
                //Do nothing
            }
            is EmptyViewHolder -> {
                //Do nothing
            }
            is ObtainCurrentLocationViewHolder -> {
                //Do nothing
            }
            else -> IllegalArgumentException("Unknown viewHolder: ${holder::class.java.simpleName}")
        }
    }

    class EmptyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ObtainCurrentLocationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar = itemView.findViewById<ProgressBar>(R.id.progressbar)
    }

    class FailedToUploadPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val deleteFailedToUploadPhotoButton = itemView.findViewById<AppCompatButton>(R.id.delete_failed_to_upload_photo)
        val retryToUploadFailedPhoto = itemView.findViewById<AppCompatButton>(R.id.retry_to_upload_failed_photo)
    }

    class UploadedPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoidTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
        val receivedIconImageView = itemView.findViewById<ImageView>(R.id.received_icon_image_view)
    }

    class TakenPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoidTextView = itemView.findViewById<TextView>(R.id.photo_id_text_view)
        val photoView = itemView.findViewById<ImageView>(R.id.photo_view)
        val uploadingMessageHolderView = itemView.findViewById<CardView>(R.id.uploading_message_holder)
        val loadingProgress = itemView.findViewById<ProgressBar>(R.id.loading_progress)
        val photoUploadingStateIndicator = itemView.findViewById<View>(R.id.photo_uploading_state_indicator)
    }

    sealed class UploadedPhotosAdapterButtonClickEvent {
        class DeleteButtonClick(val photo: TakenPhoto) : UploadedPhotosAdapterButtonClickEvent()
        class RetryButtonClick(val photo: TakenPhoto) : UploadedPhotosAdapterButtonClickEvent()
    }
}