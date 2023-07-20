package io.agora.metagpt.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import io.reactivex.disposables.CompositeDisposable;

public class BaseFragment extends Fragment {
    protected CompositeDisposable compositeDisposable;
    protected boolean mIsFront;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initContentView(inflater, container, false);

        initData();

        initView();

        initClickEvent();

        initListener();

        return null;
    }

    protected void initView() {
    }

    protected void initClickEvent() {
    }

    protected void initContentView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, boolean attachToParent) {
    }

    protected void initData() {
        compositeDisposable = new CompositeDisposable();
        mIsFront = false;
    }

    protected void initListener() {
    }


    @Override
    public void onResume() {
        super.onResume();
        mIsFront = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        mIsFront = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (null != compositeDisposable) {
            compositeDisposable.dispose();
        }
    }
}
