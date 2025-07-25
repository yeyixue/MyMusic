package com.example.mymusic.repo.playlist

import androidx.annotation.CallSuper
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable

abstract class BaseRepository
{

    protected val mDisposables = CompositeDisposable()

    /**
     * 释放Repository 的资源
     * */
    fun release() {
        if (mDisposables.isDisposed) return
        mDisposables.dispose()
    }

    protected fun onDestroy(){
        release()
    }

    protected fun Disposable.addTo(repository: com.example.mymusic.repo.playlist.BaseRepository) {
        repository.mDisposables.add(this)
    }
}