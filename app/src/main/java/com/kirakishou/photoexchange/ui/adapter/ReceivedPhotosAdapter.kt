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
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseAdapter
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItem
import com.kirakishou.photoexchange.mwvm.model.other.AdapterItemType
import com.kirakishou.photoexchange.mwvm.model.other.PhotoAnswer

/**
 * Created by kirakishou on 11/17/2017.
 */
class ReceivedPhotosAdapter(
        private val context: Context
) : BaseAdapter<PhotoAnswer>(context) {

    fun addFirst(item: AdapterItem<PhotoAnswer>) {
        items.add(0, item)
        notifyItemInserted(0)
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

    override fun getBaseAdapterInfo(): MutableList<BaseAdapterInfo> {
        return mutableListOf(
                BaseAdapterInfo(AdapterItemType.VIEW_ITEM, R.layout.adapter_item_sent_photo, SentPhotoViewHolder::class.java),
                BaseAdapterInfo(AdapterItemType.VIEW_PROGRESSBAR, R.layout.adapter_item_progress, ProgressBarViewHolder::class.java)
        )
    }

    override fun onViewHolderBound(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentPhotoViewHolder -> {
                if (items[position].value.isPresent()) {
                    val item = items[position].value.get()
                    val fullPath = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/${item.photoName}"

                    //TODO: do image loading via ImageLoader class
                    Glide.with(context)
                            .load(fullPath)
                            .apply(RequestOptions().centerCrop())
                            .into(holder.sentPhoto)
                }
            }

            is ProgressBarViewHolder -> {
                holder.progressBar.isIndeterminate = true
            }
        }
    }

    class SentPhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        @BindView(R.id.sent_photo)
        lateinit var sentPhoto: ImageView

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
}