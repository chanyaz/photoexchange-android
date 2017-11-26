package com.kirakishou.photoexchange.ui.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import java.io.File

/**
 * Created by kirakishou on 11/26/2017.
 */
class QueuedUpPhotosAdapter(
        private val context: Context
) : BaseAdapter<TakenPhoto>(context) {

    fun addQueuedUpPhotos(queuedUpPhotosList: List<TakenPhoto>) {
        checkInited()

        val converted = queuedUpPhotosList
                .map { takenPhoto -> AdapterItem(takenPhoto, AdapterItemType.VIEW_QUEUED_UP_PHOTO) }

        items.addAll(converted)
        notifyItemRangeInserted(0, converted.size)
    }

    fun removeQueuedUpPhoto(id: Long) {
        checkInited()

        val index = items.asSequence()
                .indexOfFirst { it.value.get().id == id }

        check(index != -1)

        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun removeQueuedUpPhotos(ids: List<Long>) {
        for (id in ids) {
            removeQueuedUpPhoto(id)
        }
    }

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_QUEUED_UP_PHOTO, R.layout.adapter_item_photo_queued_up, QueuedUpPhotoViewHolder::class.java))
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is QueuedUpPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    holder.progressBar.isIndeterminate = true

                    Glide.with(context)
                            .load(File(item.photoFilePath))
                            .apply(RequestOptions().centerCrop())
                            .into(holder.photoView)
                }
            }
        }
    }

    class QueuedUpPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.image_view)
        lateinit var photoView: ImageView

        @BindView(R.id.loading_indicator)
        lateinit var progressBar: ProgressBar

        init {
            ButterKnife.bind(this, itemView)
        }
    }
}