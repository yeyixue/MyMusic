package com.example.mymusic.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.remote.ApiService
import com.example.mymusic.repo.remote.RetrofitConnection
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class MyMusicViewModel : ViewModel() {


//    // 根据风格更新歌单
//    fun updatePlayListByStyle(style: String) {
//        val disposable = apiService.getSongsByStyle(style)
//            .subscribeOn(Schedulers.io())
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe({ resp ->
//                if (resp.code == 200) {
//                    _playlist.value = resp.content
//                } else {
//                    Log.e(TAG, "updatePlayListByStyle: server returned code=${resp.code}, message=${resp.message}")
//                }
//            }, { error ->
//                Log.e(TAG, "updatePlayListByStyle error: ", error)
//            })
//
//        disposables.add(disposable)
//    }




}
