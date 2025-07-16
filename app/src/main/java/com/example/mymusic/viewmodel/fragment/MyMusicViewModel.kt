package com.example.mymusic.viewmodel.fragment

import androidx.lifecycle.ViewModel

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