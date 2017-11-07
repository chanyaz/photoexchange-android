package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.helper.service.SendPhotoService
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.model.event.SendPhotoEvent
import com.kirakishou.photoexchange.mvvm.model.event.SendPhotoEventStatus
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotosViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.widget.FragmentTabsPager
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

class AllPhotosViewActivity : BaseActivity<AllPhotosViewActivityViewModel>(),
        TabLayout.OnTabSelectedListener, ViewPager.OnPageChangeListener {

    @BindView(R.id.sliding_tab_layout)
    lateinit var tabLayout: TabLayout

    @BindView(R.id.view_pager)
    lateinit var viewPager: ViewPager

    @Inject
    lateinit var viewModelFactory: AllPhotosViewActivityViewModelFactory

    override fun initViewModel(): AllPhotosViewActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos_view

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initTabs()
        getPhotoInfoFromIntent(intent)
    }

    private fun getPhotoInfoFromIntent(intent: Intent) {
        val lon = intent.getDoubleExtra("lon", 0.0)
        val lat = intent.getDoubleExtra("lat", 0.0)
        val userId = intent.getStringExtra("user_id")
        val photoFilePath = intent.getStringExtra("photo_file_path")
        val location = LonLat(lon, lat)

        check(lon != 0.0)
        check(lat != 0.0)
        check(userId.isNotEmpty())
        check(photoFilePath.isNotEmpty())

        getViewModel().photoInfo = AllPhotosViewActivityViewModel.PhotoInfo(location, userId, photoFilePath)
    }

    private fun initTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Sent"))
        tabLayout.addTab(tabLayout.newTab().setText("Received"))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = FragmentTabsPager(supportFragmentManager)
        viewPager.adapter = adapter
        viewPager.offscreenPageLimit = 1

        viewPager.addOnPageChangeListener(this)
        tabLayout.addOnTabSelectedListener(this)
    }

    override fun onActivityDestroy() {
        tabLayout.clearOnTabSelectedListeners()
        viewPager.clearOnPageChangeListeners()
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onTabReselected(tab: TabLayout.Tab) {
        if (viewPager.currentItem != tab.position) {
            viewPager.currentItem = tab.position
        }
    }

    override fun onTabUnselected(tab: TabLayout.Tab) {
    }

    override fun onTabSelected(tab: TabLayout.Tab) {
        if (viewPager.currentItem != tab.position) {
            viewPager.currentItem = tab.position
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        tabLayout.setScrollPosition(position, positionOffset, true)
    }

    override fun onPageSelected(position: Int) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SendPhotoEvent) {
        when (event.status) {
            SendPhotoEventStatus.SUCCESS -> {

            }

            SendPhotoEventStatus.FAIL -> {

            }
        }
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(this))
                .build()
                .inject(this)
    }
}
