package com.kirakishou.photoexchange.helper.concurrency.rx.scheduler

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by kirakishou on 9/17/2017.
 */
class NormalSchedulers : SchedulerProvider {
    override fun BG(): Scheduler = Schedulers.io()
    override fun UI(): Scheduler = AndroidSchedulers.mainThread()
}