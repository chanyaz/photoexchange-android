package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.service.SendPhotoService
import com.kirakishou.photoexchange.mvvm.model.*
import com.kirakishou.photoexchange.mvvm.model.dto.PhotoNameWithId
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import com.kirakishou.photoexchange.ui.adapter.TakenPhotosAdapter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject

class SentPhotosListFragment : BaseFragment<AllPhotosViewActivityViewModel>() {

    @BindView(R.id.sent_photos_list)
    lateinit var sentPhotosRv: RecyclerView

    private lateinit var adapter: TakenPhotosAdapter

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    private val retryButtonSubject = PublishSubject.create<TakenPhoto>()

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_sent_photos_list

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
        initRecycler()
        initRx()

        //getViewModel().inputs.getTakenPhotos(0, 5)
        getViewModel().inputs.getLastTakenPhoto()
    }

    override fun onFragmentViewDestroy() {
    }

    private fun initRx() {
        compositeDisposable += getViewModel().outputs.onTakenPhotosPageFetchedObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onTakenPhotosPageFetched)

        compositeDisposable += getViewModel().outputs.onLastTakenPhotoObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    //TODO
                    //update this photo in the DB:
                    // - set photoName to it.photoName
                    // - set failedToUpload to false
                    // - set wasSent to true
                }
                .subscribe(this::onLastTakenPhoto)

        compositeDisposable += getViewModel().outputs.onFailedToUploadPhotosObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onFailedToUploadPhotos)

        compositeDisposable += retryButtonSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onRetryButtonClicked)
    }

    private fun initRecycler() {
        val layoutManager = LinearLayoutManager(activity)

        adapter = TakenPhotosAdapter(activity, retryButtonSubject)
        adapter.init()

        sentPhotosRv.layoutManager = layoutManager
        sentPhotosRv.adapter = adapter
        sentPhotosRv.setHasFixedSize(true)
    }

    private fun onRetryButtonClicked(takenPhoto: TakenPhoto) {
        Timber.e("photoName: ${takenPhoto.photoName}")
    }

    private fun onFailedToUploadPhotos(failedToUploadPhotos: List<TakenPhoto>) {
        adapter.runOnAdapterHandler {
            for (photo in failedToUploadPhotos) {
                adapter.add(AdapterItem(photo, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
            }
        }
    }

    private fun onLastTakenPhoto(lastTakenPhoto: TakenPhoto) {
        if (!lastTakenPhoto.isEmpty()) {
            serviceUploadPhoto(lastTakenPhoto)

            adapter.runOnAdapterHandler {
                adapter.add(AdapterItem(lastTakenPhoto, AdapterItemType.VIEW_FAILED_TO_UPLOAD))
            }
        }
    }

    private fun serviceUploadPhoto(lastTakenPhoto: TakenPhoto) {
        val intent = Intent(activity, SendPhotoService::class.java)
        intent.putExtra("command", ServiceCommand.SEND_PHOTO.value)
        intent.putExtra("photo_id", lastTakenPhoto.id)
        intent.putExtra("lon", lastTakenPhoto.lon)
        intent.putExtra("lat", lastTakenPhoto.lat)
        intent.putExtra("user_id", lastTakenPhoto.userId)
        intent.putExtra("photo_file_path", lastTakenPhoto.photoFilePath)

        activity.startService(intent)
    }

    private fun onTakenPhotosPageFetched(takenPhotosList: List<TakenPhoto>) {
        Timber.d("Found ${takenPhotosList.size} taken photos in the DB")
        takenPhotosList.forEach { Timber.d(it.toString()) }

        adapter.runOnAdapterHandler {
            for (takenPhoto in takenPhotosList) {
                if (takenPhoto.wasSent) {
                    adapter.add(AdapterItem(takenPhoto, AdapterItemType.VIEW_ITEM))
                } else {
                    adapter.add(AdapterItem(AdapterItemType.VIEW_PROGRESSBAR))
                }
            }
        }
    }

    fun onPhotoUploaded(response: PhotoNameWithId) {
        adapter.runOnAdapterHandler {
            adapter.updateType(response.photoId, response.photoName)
        }
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotosViewActivity))
                .build()
                .inject(this)
    }
}
