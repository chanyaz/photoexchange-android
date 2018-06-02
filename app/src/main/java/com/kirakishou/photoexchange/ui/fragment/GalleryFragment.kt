package com.kirakishou.photoexchange.ui.fragment


import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.filterErrorCodes
import com.kirakishou.photoexchange.helper.intercom.StateEventListener
import com.kirakishou.photoexchange.helper.intercom.event.BaseEvent
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.GalleryPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class GalleryFragment : BaseFragment() {

    @BindView(R.id.gallery_photos_list)
    lateinit var galleryPhotosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    private val TAG = "GalleryFragment"
    private val GALLERY_PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val loadMoreSubject = PublishSubject.create<Int>()
    private val adapterButtonClickSubject = PublishSubject.create<GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    lateinit var adapter: GalleryPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    override fun getContentView(): Int = R.layout.fragment_gallery

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadFirstPage()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun loadFirstPage() {
        compositeDisposable += Observable.just(1)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { endlessScrollListener.pageLoading() }
            .doOnNext { addProgressFooter() }
            .observeOn(Schedulers.io())
            .flatMap { viewModel.loadNextPageOfGalleryPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { removeProgressFooter() }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> addPhotoToAdapter(result.value)
                    is Either.Error -> handleError(result.error)
                }
            }, {
                Timber.tag(TAG).e(it)
            })
    }

    private fun initRx() {
        compositeDisposable += loadMoreSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { addProgressFooter() }
            .observeOn(Schedulers.io())
            .concatMap { viewModel.loadNextPageOfGalleryPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { removeProgressFooter() }
            .subscribe({ result ->
                when (result) {
                    is Either.Value -> addPhotoToAdapter(result.value)
                    is Either.Error -> handleError(result.error)
                }
            }, {
                Timber.tag(TAG).e(it)
            })

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.FavouriteClicked::class.java)
            .concatMap { viewModel.favouritePhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .subscribe({ resultPair ->
                val result = resultPair.first
                val photoName = resultPair.second

                when (result) {
                    is Either.Value -> favouritePhoto(photoName, result.value.isFavourited, result.value.favouritesCount)
                    is Either.Error -> handleError(result.error)
                }
            }, {
                Timber.tag(TAG).e(it)
            })

        compositeDisposable += adapterButtonClickSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .filter { buttonClicked -> buttonClicked is GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked }
            .cast(GalleryPhotosAdapter.GalleryPhotosAdapterButtonClickEvent.ReportClicked::class.java)
            .concatMap { viewModel.reportPhoto(it.photoName).zipWith(Observable.just(it.photoName)) }
            .subscribe({ resultPair ->
                val result = resultPair.first
                val photoName = resultPair.second

                when (result) {
                    is Either.Value -> reportPhoto(photoName, result.value)
                    is Either.Error -> handleError(result.error)
                }
            }, {
                Timber.tag(TAG).e(it)
            })
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), GALLERY_PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = GalleryPhotosAdapter(requireContext(), imageLoader, adapterButtonClickSubject)

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = GalleryPhotosAdapterSpanSizeLookup(adapter, columnsCount)

        photosPerPage = Constants.GALLERY_PHOTOS_PER_ROW * layoutManager.spanCount
        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, photosPerPage, loadMoreSubject)

        galleryPhotosList.layoutManager = layoutManager
        galleryPhotosList.adapter = adapter
        galleryPhotosList.clearOnScrollListeners()
        galleryPhotosList.addOnScrollListener(endlessScrollListener)
    }

    private fun favouritePhoto(photoName: String, isFavourited: Boolean, favouritesCount: Long) {
        galleryPhotosList.post {
            if (!adapter.favouritePhoto(photoName, isFavourited, favouritesCount)) {
                return@post
            }
        }
    }

    private fun reportPhoto(photoName: String, isReported: Boolean) {
        galleryPhotosList.post {
            if (!adapter.reportPhoto(photoName, isReported)) {
                return@post
            }

            if (isReported) {
                (requireActivity() as PhotosActivity).showToast(getString(R.string.photo_reported_text), Toast.LENGTH_SHORT)
            } else {
                (requireActivity() as PhotosActivity).showToast(getString(R.string.photo_unreported_text), Toast.LENGTH_SHORT)
            }
        }
    }

    private fun addProgressFooter() {
        galleryPhotosList.post {
            adapter.addProgressFooter()
        }
    }

    private fun removeProgressFooter() {
        galleryPhotosList.post {
            adapter.removeProgressFooter()
        }
    }

    private fun addPhotoToAdapter(galleryPhotos: List<GalleryPhoto>) {
        if (!isAdded) {
            return
        }

        galleryPhotosList.post {
            endlessScrollListener.pageLoaded()

            if (galleryPhotos.isNotEmpty()) {
                lastId = galleryPhotos.last().galleryPhotoId
                adapter.addAll(galleryPhotos)
            }

            if (galleryPhotos.size < photosPerPage) {
                endlessScrollListener.reachedEnd()
            }
        }
    }

    private fun handleError(errorCode: ErrorCode) {
        galleryPhotosList.post {
            adapter.removeProgressFooter()
        }

        (requireActivity() as PhotosActivity).showErrorCodeToast(errorCode)
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as PhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): GalleryFragment {
            val fragment = GalleryFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
