package io.agora.gpt.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable

open class BaseFragment : Fragment() {

    val compositeDisposable: CompositeDisposable by lazy {
        CompositeDisposable()
    }

    @JvmField
    protected var mIsFront = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initContentView(inflater, container, false)
        initData()
        initView()
        initClickEvent()
        initListener()
        return null
    }

    protected open fun initView() {}
    protected open fun initClickEvent() {}
    protected open fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {}
    protected open fun initData() {
        mIsFront = false
    }

    protected fun initListener() {}
    override fun onResume() {
        super.onResume()
        mIsFront = true
    }

    override fun onPause() {
        super.onPause()
        mIsFront = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDetach() {
        super.onDetach()
        compositeDisposable.dispose()
    }
}