package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.mwvm.model.adapter.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.adapter.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.RecipientLocation
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import io.reactivex.subjects.PublishSubject

/**
 * Created by kirakishou on 11/7/2017.
 */
class UploadedPhotosAdapter(
        private val context: Context,
        private val imageLoader: ImageLoader,
        private val visiblePhotosSubject: PublishSubject<TakenPhoto>
) : BaseAdapter<TakenPhoto>(context) {

    private val selector = UploadedPhotosIdSelectorFunction()
    private val duplicatesCheckerSet = mutableSetOf<Long>()
    private val noPhotosUploadedMessage: String = context.getString(R.string.no_photos_uploaded)
    private val notificationTypes = arrayListOf(
            AdapterItemType.VIEW_PROGRESSBAR.ordinal,
            AdapterItemType.VIEW_MESSAGE.ordinal,
            AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal)

    private fun isDuplicate(item: AdapterItem<TakenPhoto>): Boolean {
        if (item.getType() != AdapterItemType.VIEW_ITEM.ordinal) {
            return false
        }

        val photoAnswer = item.value.get()
        val id = selector.select(photoAnswer)

        return !duplicatesCheckerSet.add(id)
    }

    fun addFirst(item: AdapterItem<TakenPhoto>) {
        checkInited()

        if (isDuplicate(item)) {
            return
        }

        items.add(0, item)
        notifyItemInserted(0)
    }

    override fun add(item: AdapterItem<TakenPhoto>) {
        if (isDuplicate(item)) {
            return
        }

        if (items.isEmpty() || items.first().getType() !in notificationTypes) {
            items.add(0, item)
            notifyItemInserted(0)
        } else {
            items.add(1, item)
            notifyItemInserted(1)
        }
    }

    override fun addAll(items: List<AdapterItem<TakenPhoto>>) {
        items.forEach { add(it) }
    }

    fun addPhotoUploadingIndicator() {
        checkInited()

        if (items.isEmpty() || items.first().getType() != AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal) {
            items.add(0, AdapterItem(AdapterItemType.VIEW_PHOTO_UPLOADING))
            notifyItemInserted(0)
        }
    }

    fun removePhotoUploadingIndicator() {
        checkInited()

        if (items.isNotEmpty() && items.first().getType() == AdapterItemType.VIEW_PHOTO_UPLOADING.ordinal) {
            items.removeAt(0)
            notifyItemRemoved(0)
        }
    }

    fun addProgressFooter() {
        checkInited()

        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_PROGRESSBAR.ordinal) {
            items.add(AdapterItem(AdapterItemType.VIEW_PROGRESSBAR))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun removeProgressFooter() {
        checkInited()

        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_PROGRESSBAR.ordinal) {
            val index = items.lastIndex

            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun addMessageFooter() {
        checkInited()

        if (items.isEmpty() || items.last().getType() != AdapterItemType.VIEW_MESSAGE.ordinal) {
            items.add(AdapterItem(AdapterItemType.VIEW_MESSAGE))
            notifyItemInserted(items.lastIndex)
        }
    }

    fun removeMessageFooter() {
        checkInited()

        if (items.isNotEmpty() && items.last().getType() == AdapterItemType.VIEW_MESSAGE.ordinal) {
            val index = items.lastIndex

            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun updatePhotosRecipientLocation(recipientLocationList: List<RecipientLocation>) {
        for (recipientLocation in recipientLocationList) {
            val photoIndex = items.indexOfFirst { it.getType() == AdapterItemType.VIEW_ITEM.ordinal && it.value.get().photoName == recipientLocation.photoName }
            if (photoIndex == -1) {
                continue
            }

            val newPhoto = AdapterItem(TakenPhoto.createWithRecipientLocation(items[photoIndex].value.get(), recipientLocation.getLocation()), AdapterItemType.VIEW_ITEM)
            items[photoIndex] = newPhoto

            notifyItemChanged(photoIndex)
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_sent_photos, SentPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_MESSAGE, R.layout.adapter_item_message, MessageViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PHOTO_UPLOADING, R.layout.adapter_item_photo_uploading, PhotoUploadingViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    imageLoader.loadImageFromNetInto(item.photoName, ImageLoader.PhotoSize.Small, holder.photoView)

                    if (item.recipientLocation == null) {
                        holder.stateIcon.setImageDrawable(context.getDrawable(R.drawable.ic_done))
                        visiblePhotosSubject.onNext(item)
                    } else {
                        holder.stateIcon.setImageDrawable(context.getDrawable(R.drawable.ic_done_all))
                    }
                }
            }

            is MessageViewHolder -> {
                holder.messageTv.text = noPhotosUploadedMessage
            }

            is ProgressBarViewHolder -> {
            }

            is PhotoUploadingViewHolder -> {
            }
        }
    }

    class SentPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.photo)
        lateinit var photoView: ImageView

        @BindView(R.id.photo_uploading_state_icon)
        lateinit var stateIcon: ImageView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class ProgressBarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.progressbar)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.message)
        lateinit var messageTv: TextView

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    class PhotoUploadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }

    inner class UploadedPhotosIdSelectorFunction {
        fun select(item: TakenPhoto): Long = item.id
    }
}



















