package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.Toast
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.seconds
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapter
import com.kirakishou.photoexchange.ui.adapter.UploadedPhotosAdapterSpanSizeLookup
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewState
import com.kirakishou.photoexchange.ui.viewstate.UploadedPhotosFragmentViewStateEvent
import com.kirakishou.photoexchange.ui.widget.EndlessRecyclerOnScrollListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class UploadedPhotosFragment : BaseFragment() {

    @BindView(R.id.my_photos_list)
    lateinit var photosList: RecyclerView

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var viewModel: PhotosActivityViewModel

    lateinit var adapter: UploadedPhotosAdapter
    lateinit var endlessScrollListener: EndlessRecyclerOnScrollListener

    private val TAG = "UploadedPhotosFragment"
    private val PHOTO_ADAPTER_VIEW_WIDTH = 288
    private val ADAPTER_LOAD_MORE_ITEMS_DELAY_MS = 1.seconds()
    private val adapterButtonsClickSubject = PublishSubject.create<UploadedPhotosAdapter.UploadedPhotosAdapterButtonClickEvent>().toSerialized()
    private var viewState = UploadedPhotosFragmentViewState()
    private val loadMoreSubject = PublishSubject.create<Int>()
    private var photosPerPage = 0
    private var lastId = Long.MAX_VALUE

    override fun getContentView(): Int = R.layout.fragment_uploaded_photos

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRx()
        initRecyclerView()
        loadPhotos()

        restoreFragmentFromViewState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        viewState.saveToBundle(outState)
    }

    private fun restoreFragmentFromViewState(savedInstanceState: Bundle?) {
        viewState = UploadedPhotosFragmentViewState()
            .also { it.loadFromBundle(savedInstanceState) }
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRecyclerView() {
        val columnsCount = AndroidUtils.calculateNoOfColumns(requireContext(), PHOTO_ADAPTER_VIEW_WIDTH)

        adapter = UploadedPhotosAdapter(requireContext(), imageLoader, adapterButtonsClickSubject)
        adapter.init()

        val layoutManager = GridLayoutManager(requireContext(), columnsCount)
        layoutManager.spanSizeLookup = UploadedPhotosAdapterSpanSizeLookup(adapter, columnsCount)
        photosPerPage = Constants.UPLOADED_PHOTOS_PER_ROW * layoutManager.spanCount

        endlessScrollListener = EndlessRecyclerOnScrollListener(layoutManager, photosPerPage, loadMoreSubject)

        photosList.layoutManager = layoutManager
        photosList.adapter = adapter
        photosList.clearOnScrollListeners()
        photosList.addOnScrollListener(endlessScrollListener)
    }

    private fun initRx() {
        compositeDisposable += viewModel.onPhotoUploadEventSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { event -> onUploadingEvent(event) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += viewModel.uploadedPhotosFragmentViewStateSubject
            .observeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { viewState -> onViewStateChanged(viewState) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()

        compositeDisposable += adapterButtonsClickSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe(viewModel.uploadedPhotosAdapterButtonClickSubject::onNext)

        compositeDisposable += loadMoreSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnNext { addProgressFooter() }
            .observeOn(Schedulers.io())
            .concatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
            .delay(ADAPTER_LOAD_MORE_ITEMS_DELAY_MS, TimeUnit.MILLISECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { removeProgressFooter() }
            .doOnNext { photos -> addUploadedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun loadPhotos() {
        compositeDisposable += viewModel.loadMyPhotos()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSuccess { photos -> addTakenPhotosToAdapter(photos) }
            .toObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { addProgressFooter() }
            .observeOn(Schedulers.io())
            .flatMap { viewModel.loadNextPageOfUploadedPhotos(lastId, photosPerPage) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { photos -> addUploadedPhotosToAdapter(photos) }
            .doOnError { Timber.tag(TAG).e(it) }
            .subscribe()
    }

    private fun onViewStateChanged(viewStateEvent: UploadedPhotosFragmentViewStateEvent) {
        if (!isAdded) {
            return
        }

        photosList.post {
            when (viewStateEvent) {
                is UploadedPhotosFragmentViewStateEvent.ScrollToTop -> {
                    photosList.scrollToPosition(0)
                }
                is UploadedPhotosFragmentViewStateEvent.Default -> {

                }
                is UploadedPhotosFragmentViewStateEvent.ShowObtainCurrentLocationNotification -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is UploadedPhotosFragmentViewStateEvent.HideObtainCurrentLocationNotification -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is UploadedPhotosFragmentViewStateEvent.RemovePhoto -> {
                    adapter.removePhotoById(viewStateEvent.photo.id)
                }
                is UploadedPhotosFragmentViewStateEvent.AddPhoto -> {
                    adapter.addTakenPhoto(viewStateEvent.photo)
                }
                else -> throw IllegalArgumentException("Unknown UploadedPhotosFragmentViewStateEvent $viewStateEvent")
            }
        }
    }

    private fun onUploadingEvent(event: PhotoUploadEvent) {
        if (!isAdded) {
            return
        }

        photosList.post {
            when (event) {
                is PhotoUploadEvent.OnLocationUpdateStart -> {
                    adapter.showObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnLocationUpdateEnd -> {
                    adapter.hideObtainCurrentLocationNotification()
                }
                is PhotoUploadEvent.OnPrepare -> {

                }
                is PhotoUploadEvent.OnPhotoUploadStart -> {
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.PHOTO_UPLOADING })
                }
                is PhotoUploadEvent.OnProgress -> {
                    adapter.addTakenPhoto(event.photo)
                    adapter.updatePhotoProgress(event.photo.id, event.progress)
                }
                is PhotoUploadEvent.OnUploaded -> {
                    adapter.removePhotoById(event.photo.photoId)
                    adapter.addUploadedPhoto(event.photo)
                }
                is PhotoUploadEvent.OnFailedToUpload -> {
                    adapter.removePhotoById(event.photo.id)
                    adapter.addTakenPhoto(event.photo.also { it.photoState = PhotoState.FAILED_TO_UPLOAD })
                    (requireActivity() as PhotosActivity).showUploadPhotoErrorMessage(event.errorCode)
                }
                is PhotoUploadEvent.OnFoundPhotoAnswer -> {
                    adapter.updateUploadedPhotoSetReceiverInfo(event.photo)
                }
                is PhotoUploadEvent.OnEnd -> {
                }

                is PhotoUploadEvent.OnCouldNotGetUserIdFromServerError,
                is PhotoUploadEvent.OnUnknownError -> {
                    handleErrorEvent(event)
                }
                else -> throw IllegalArgumentException("Unknown PhotoUploadEvent $event")
            }
        }
    }

    private fun handleErrorEvent(event: PhotoUploadEvent) {
        when (event) {
            is PhotoUploadEvent.OnCouldNotGetUserIdFromServerError -> {
                Timber.tag(TAG).e("Could not get user id from the server")
                (requireActivity() as PhotosActivity).onShowToast("Could not get user id from the server")
            }
            is PhotoUploadEvent.OnUnknownError -> {
                (requireActivity() as PhotosActivity).showUnknownErrorMessage(event.error)
            }
            else -> IllegalStateException("Unknown event $event")
        }

        adapter.updateAllPhotosState(PhotoState.FAILED_TO_UPLOAD)
    }

    private fun addUploadedPhotosToAdapter(uploadedPhotos: List<UploadedPhoto>) {
        if (!isAdded) {
            return
        }

        endlessScrollListener.pageLoaded()

        photosList.post {
            if (uploadedPhotos.isNotEmpty()) {
                adapter.addUploadedPhotos(uploadedPhotos)
            } else {
                endlessScrollListener.reachedEnd()
            }
        }
    }

    private fun addTakenPhotosToAdapter(takenPhotos: List<TakenPhoto>) {
        if (!isAdded) {
            return
        }

        photosList.post {
            if (takenPhotos.isNotEmpty()) {
                adapter.clear()
                adapter.addTakenPhotos(takenPhotos)
            } else {
                //TODO: show notification that no photos has been uploaded yet
            }
        }
    }

    private fun addProgressFooter() {
        photosList.post {
            adapter.showProgressFooter()
        }
    }

    private fun removeProgressFooter() {
        photosList.post {
            adapter.hideProgressFooter()
        }
    }

    private fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        (requireActivity() as PhotosActivity).showToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (requireActivity() as PhotosActivity).activityComponent
            .inject(this)
    }

    companion object {
        fun newInstance(): UploadedPhotosFragment {
            val fragment = UploadedPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
