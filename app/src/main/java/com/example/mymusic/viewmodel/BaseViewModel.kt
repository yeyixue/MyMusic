package com.example.mymusic.viewmodel

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class BaseViewModel : ViewModel() {

    private val mDisposables = CompositeDisposable()

    protected fun Disposable.addTo(viewModel: com.example.mymusic.viewmodel.BaseViewModel): Disposable {
        viewModel.mDisposables.add(this)
        return this
    }

    @CallSuper
    override fun onCleared() {
        super.onCleared()
        mDisposables.dispose()
    }
}