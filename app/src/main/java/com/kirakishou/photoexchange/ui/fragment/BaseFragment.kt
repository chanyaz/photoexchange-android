package com.kirakishou.photoexchange.ui.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.RxLifecycle
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseFragment : Fragment() {

    protected val compositeDisposable = CompositeDisposable()
    protected lateinit var lifecycle: RxLifecycle
    private lateinit var unBinder: Unbinder

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        lifecycle = RxLifecycle()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.onCreate()
        retainInstance = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        val viewId = getContentView()
        val root = inflater.inflate(viewId, container, false)
        unBinder = ButterKnife.bind(this, root)

        return root
    }

    override fun onStart() {
        super.onStart()
        lifecycle.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resolveDaggerDependency()
        onFragmentViewCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        lifecycle.onResume()
    }

    override fun onPause() {
        super.onPause()
        lifecycle.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        onFragmentViewDestroy()
        unBinder.unbind()
    }

    override fun onStop() {
        super.onStop()
        lifecycle.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycle.onDestroy()
    }

    override fun onDetach() {
        super.onDetach()

        compositeDisposable.clear()
        PhotoExchangeApplication.watch(this, this::class.simpleName)
    }

    protected abstract fun getContentView(): Int
    protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
    protected abstract fun onFragmentViewDestroy()
    protected abstract fun resolveDaggerDependency()
}