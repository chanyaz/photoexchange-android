package com.kirakishou.photoexchange.helper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import com.crashlytics.android.Crashlytics
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.DaggerFindPhotoAnswerServiceComponent
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.event.PhotoReceivedEvent
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import com.kirakishou.photoexchange.mwvm.model.state.LookingForPhotoState
import com.kirakishou.photoexchange.mwvm.viewmodel.FindPhotoAnswerServiceViewModel
import com.kirakishou.photoexchange.ui.activity.AllPhotosViewActivity
import io.fabric.sdk.android.Fabric
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import javax.inject.Inject

class FindPhotoAnswerService : JobService() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    @Inject
    lateinit var photoAnswerRepo: PhotoAnswerRepository

    @Inject
    lateinit var takenPhotosRepo: TakenPhotosRepository

    @Inject
    lateinit var eventBus: EventBus

    private lateinit var viewModel: FindPhotoAnswerServiceViewModel

    private val tag = "[${this::class.java.simpleName}]: "
    private var isRxInited = false
    private var notificationManager: NotificationManager? = null
    private val compositeDisposable = CompositeDisposable()

    override fun onCreate() {
        super.onCreate()

        Fabric.with(this, Crashlytics())
        resolveDaggerDependency()
        viewModel = FindPhotoAnswerServiceViewModel(photoAnswerRepo, takenPhotosRepo, apiClient, schedulers)
    }

    override fun onDestroy() {
        cleanUp()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters): Boolean {
        Timber.tag(tag).d("onStartJob()")

        when (getJobTypeFromParams(params)) {
            IMMEDIATE_JOB_TYPE -> Timber.tag(tag).d("onStartJob() IMMEDIATE_JOB_TYPE")
            PERIODIC_JOB_TYPE -> Timber.tag(tag).d("onStartJob() PERIODIC_JOB_TYPE")
        }

        val currentTime = TimeUtils.getTimeFast()
        val formattedTime = TimeUtils.formatDateAndTime(currentTime)
        Timber.tag(tag).d("onStartJob() Job started at: $formattedTime")

        initRx(params)

        try {
            handleCommand(params)
        } catch (error: Throwable) {
            onUnknownError(params, error)
            return false
        }

        updateNotificationShowLookingForPhoto()
        return true
    }

    private fun handleCommand(params: JobParameters) {
        val userId = getUserIdFromParams(params)
        viewModel.inputs.findPhotoAnswer(userId)
    }

    override fun onStopJob(params: JobParameters): Boolean {
        Timber.tag(tag).d("onStopJob() onStopJob")
        return true
    }

    private fun initRx(params: JobParameters) {
        if (isRxInited) {
            return
        }

        isRxInited = true

        compositeDisposable += viewModel.outputs.findPhotoStateObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ state -> onFindPhotoStateChanged(params, state) })

        compositeDisposable += viewModel.errors.onBadResponseObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ onBadResponse(params, it) })

        compositeDisposable += viewModel.errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ onUnknownError(params, it) })
    }

    private fun onFindPhotoStateChanged(params: JobParameters, state: LookingForPhotoState) {
        when (state) {
            is LookingForPhotoState.UploadMorePhotos -> {
                Timber.tag(tag).d("onFindPhotoStateChanged() UploadMorePhotos Upload more photos")

                eventBus.post(PhotoReceivedEvent.uploadMorePhotos())
                cancelAll(this)
                finish(params, false)

                cancelNotification()
            }

            is LookingForPhotoState.LocalRepositoryError -> {
                Timber.tag(tag).d("onFindPhotoStateChanged() LocalRepositoryError Could not mark a photo as received")

                eventBus.post(PhotoReceivedEvent.fail())
                cancelAll(this)
                finish(params, false)

                cancelNotification()
            }

            is LookingForPhotoState.ServerHasNoPhotos -> {
                Timber.tag(tag).d("onFindPhotoStateChanged() ServerHasNoPhotos No photos on server to send back")

                eventBus.post(PhotoReceivedEvent.noPhotos())

                if (isJobImmediate(params)) {
                    Timber.tag(tag).d("onFindPhotoStateChanged() ServerHasNoPhotos Current job is immediate, changing it to periodic")
                    finish(params, false)
                    switchJobToPeriodic(getUserIdFromParams(params))
                } else {
                    Timber.tag(tag).d("onFindPhotoStateChanged() ServerHasNoPhotos Current job is periodic, no need to change it")
                    finish(params, true)
                }
            }

            is LookingForPhotoState.PhotoFound -> {
                val userId = getUserIdFromParams(params)

                if (!state.allFound) {
                    Timber.tag(tag).d("onFindPhotoStateChanged() PhotoFound allFound = ${state.allFound} Reschedule job")
                    Timber.tag(tag).d(state.photoAnswer.toString())

                    eventBus.post(PhotoReceivedEvent.successNotAllReceived(state.photoAnswer))

                    if (isJobPeriodic(params)) {
                        Timber.tag(tag).d("onFindPhotoStateChanged() PhotoFound Current job is periodic, changing it to immediate")
                        switchJobToImmediate(userId)
                    } else {
                        Timber.tag(tag).d("onFindPhotoStateChanged() PhotoFound Current job is immediate, no need to change it")
                        finish(params, true)
                    }
                } else {
                    Timber.tag(tag).d("onFindPhotoStateChanged() PhotoFound allFound = ${state.allFound} we are done")
                    eventBus.post(PhotoReceivedEvent.successAllReceived(state.photoAnswer))

                    finish(params, false)
                }

                updateNotificationShowPhotoFound()
            }

            is LookingForPhotoState.UnknownError -> {
                onUnknownError(params, state.error)
            }
        }
    }

    private fun onBadResponse(params: JobParameters, errorCode: ServerErrorCode) {
        Timber.tag(tag).d("onBadResponse() BadResponse errorCode: $errorCode")

        eventBus.post(PhotoReceivedEvent.fail())
        finish(params, false)
    }

    private fun onUnknownError(params: JobParameters, error: Throwable) {
        Timber.tag(tag).d("onUnknownError() onUnknownError")
        Timber.e(error)

        eventBus.post(PhotoReceivedEvent.fail())
        finish(params, false)
    }

    private fun finish(params: JobParameters, reschedule: Boolean) {
        jobFinished(params, reschedule)
    }

    private fun cleanUp() {
        Timber.tag(tag).d("cleanUp()")

        compositeDisposable.clear()
        viewModel.cleanUp()
    }

    //utils
    private fun getUserIdFromParams(params: JobParameters): String {
        val userId = params.extras.getString("user_id")
        checkNotNull(userId)

        return userId
    }

    private fun getJobTypeFromParams(params: JobParameters): Int {
        val jobType = params.extras.getInt("job_type", -1)
        check(jobType != -1)

        return jobType
    }

    private fun isJobPeriodic(params: JobParameters): Boolean {
        val jobType = getJobTypeFromParams(params)
        return jobType == PERIODIC_JOB_TYPE
    }

    private fun isJobImmediate(params: JobParameters): Boolean {
        val jobType = getJobTypeFromParams(params)
        return jobType == IMMEDIATE_JOB_TYPE
    }

    private fun switchJobToImmediate(userId: String) {
        scheduleImmediateJob(userId, this)
    }

    private fun switchJobToPeriodic(userId: String) {
        schedulePeriodicJob(userId, this)
    }

    //notifications
    private fun updateNotificationShowLookingForPhoto() {
        val newNotification = createNotificationLookingForPhoto()
        getNotificationManager().notify(Constants.NOTIFICATION_ID, newNotification)
    }

    private fun updateNotificationShowPhotoFound() {
        val newNotification = createNotificationPhotoFound()
        getNotificationManager().notify(Constants.NOTIFICATION_ID, newNotification)
    }

    private fun createNotificationPhotoFound(): Notification {
        if (AndroidUtils.isOreoOrHigher()) {
            createNotificationChannelIfNotExists()

            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                    .setContentTitle("Done")
                    .setContentText("Photo has been found!")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getNotificationIntent())
                    .setAutoCancel(true)
                    .build()
        } else {
            return NotificationCompat.Builder(this)
                    .setContentTitle("Done")
                    .setContentText("Photo has been found!")
                    .setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getNotificationIntent())
                    .setAutoCancel(true)
                    .build()
        }
    }

    private fun createNotificationLookingForPhoto(): Notification {
        if (AndroidUtils.isOreoOrHigher()) {
            createNotificationChannelIfNotExists()

            return NotificationCompat.Builder(this, Constants.CHANNEL_ID)
                    .setContentTitle("Searching")
                    .setContentText("Looking for your photo")
                    .setSmallIcon(android.R.drawable.ic_menu_search)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getNotificationIntent())
                    .setAutoCancel(false)
                    .build()

        } else {
            return NotificationCompat.Builder(this)
                    .setContentTitle("Searching")
                    .setContentText("Looking for your photo")
                    .setSmallIcon(android.R.drawable.ic_menu_search)
                    .setWhen(System.currentTimeMillis())
                    .setContentIntent(getNotificationIntent())
                    .setAutoCancel(false)
                    .build()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelIfNotExists() {
        if (getNotificationManager().getNotificationChannel(Constants.CHANNEL_ID) == null) {
            val notificationChannel = NotificationChannel(Constants.CHANNEL_ID, Constants.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW)

            notificationChannel.enableLights(false)
            notificationChannel.enableVibration(false)
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            getNotificationManager().createNotificationChannel(notificationChannel)
        }
    }

    private fun cancelNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(Constants.NOTIFICATION_ID)
    }

    private fun getNotificationIntent(): PendingIntent {
        val notificationIntent = Intent(this, AllPhotosViewActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        //notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notificationIntent.putExtra("open_received_photos_fragment", true)

        return PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun getNotificationManager(): NotificationManager {
        if (notificationManager == null) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return notificationManager!!
    }

    private fun resolveDaggerDependency() {
        DaggerFindPhotoAnswerServiceComponent
                .builder()
                .findPhotoAnswerServiceModule(FindPhotoAnswerServiceModule(this))
                .networkModule(NetworkModule(PhotoExchangeApplication.baseUrl))
                .gsonModule(GsonModule())
                .apiClientModule(ApiClientModule())
                .schedulerProviderModule(SchedulerProviderModule())
                .eventBusModule(EventBusModule())
                .databaseModule(DatabaseModule(PhotoExchangeApplication.databaseName))
                .mapperModule(MapperModule())
                .build()
                .inject(this)
    }

    companion object {
        private val JOB_ID = 1

        private val IMMEDIATE_JOB_TYPE = 0
        private val PERIODIC_JOB_TYPE = 1

        //Add slight delay so we have time to cancel previous job if the schedule
        //function was called twice over minimumDelay period of time
        fun scheduleImmediateJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putString("user_id", userId)
            extras.putInt("job_type", IMMEDIATE_JOB_TYPE)

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(5_000)
                    .setOverrideDeadline(5_000)
                    .setExtras(extras)
                    .setBackoffCriteria(2_000, JobInfo.BACKOFF_POLICY_LINEAR)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            val result = jobScheduler.schedule(jobInfo)

            check(result == JobScheduler.RESULT_SUCCESS)
        }

        fun schedulePeriodicJob(userId: String, context: Context) {
            val extras = PersistableBundle()
            extras.putString("user_id", userId)
            extras.putInt("job_type", PERIODIC_JOB_TYPE)

            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, FindPhotoAnswerService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setRequiresDeviceIdle(false)
                    .setRequiresCharging(false)
                    .setMinimumLatency(5_000)
                    .setOverrideDeadline(30_000)
                    .setExtras(extras)
                    .setBackoffCriteria(5_000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .build()

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
            val result = jobScheduler.schedule(jobInfo)

            check(result == JobScheduler.RESULT_SUCCESS)
        }

        fun cancelAll(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancelAll()
        }

        fun isAlreadyRunning(context: Context): Boolean {
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            return scheduler.allPendingJobs.any { it.id == JOB_ID }
        }
    }
}
