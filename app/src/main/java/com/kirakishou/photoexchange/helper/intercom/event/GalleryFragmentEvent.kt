package com.kirakishou.photoexchange.helper.intercom.event

import com.kirakishou.photoexchange.mvp.model.GalleryPhoto

sealed class GalleryFragmentEvent : BaseEvent {
    sealed class GeneralEvents : GalleryFragmentEvent() {
        class ShowProgressFooter : GeneralEvents()
        class HideProgressFooter : GeneralEvents()
        class OnPageSelected : GeneralEvents()
        class PageIsLoading : GeneralEvents()
        class ShowGalleryPhotos(val photos: List<GalleryPhoto>) : GeneralEvents()
    }
}