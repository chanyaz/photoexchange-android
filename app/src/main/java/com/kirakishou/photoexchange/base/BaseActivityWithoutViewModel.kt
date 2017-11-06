package com.kirakishou.photoexchange.base

import android.arch.lifecycle.LifecycleRegistry
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.mvvm.model.Fickle
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

/**
 * Created by kirakishou on 11/6/2017.
 */
abstract class BaseActivityWithoutViewModel : AppCompatActivity() {
    private val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    protected val compositeDisposable = CompositeDisposable()
    protected val unknownErrorsSubject = PublishSubject.create<Throwable>()

    private var unBinder: Fickle<Unbinder> = Fickle.empty()


    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        compositeDisposable += unknownErrorsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)

        resolveDaggerDependency()

        setContentView(getContentView())
        unBinder = Fickle.of(ButterKnife.bind(this))

        onActivityCreate(savedInstanceState, intent)
    }

    override fun onDestroy() {
        compositeDisposable.clear()

        unBinder.ifPresent {
            it.unbind()
        }

        onActivityDestroy()
        super.onDestroy()
    }

    open fun onShowToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    @CallSuper
    open fun onBadResponse(serverErrorCode: ServerErrorCode) {
        Timber.d("ServerErrorCode: $serverErrorCode")
    }

    @CallSuper
    open fun onUnknownError(error: Throwable) {
        Timber.e(error)

        if (error.message != null) {
            onShowToast(error.message!!)
        } else {
            onShowToast("Неизвестная ошибка")
        }

        finish()
    }

    open fun runActivity(clazz: Class<*>, finishCurrentActivity: Boolean = false) {
        val intent = Intent(this, clazz)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    open fun finishActivity() {
        finish()
    }

    open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean) {
        val intent = Intent(this, clazz)
        intent.putExtras(args)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    protected abstract fun getContentView(): Int
    protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
    protected abstract fun onActivityDestroy()
    protected abstract fun resolveDaggerDependency()
}