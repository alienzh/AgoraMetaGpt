package io.agora.gpt.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

open class BaseFragment : Fragment() {

    @JvmField
    protected var mIsFront = false
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        initContentView(inflater, container, false)
        initData()
        initView()
        return null
    }

    protected open fun initView() {}
    protected open fun initClickEvent() {}
    protected open fun initContentView(inflater: LayoutInflater, container: ViewGroup?, attachToParent: Boolean) {}
    protected open fun initData() {
        mIsFront = false
    }

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
    }
}