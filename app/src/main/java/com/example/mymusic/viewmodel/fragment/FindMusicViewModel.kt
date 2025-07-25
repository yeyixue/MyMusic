package com.example.mymusic.viewmodel.fragment

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicFindStyleInfo
import com.example.mymusic.repo.entity.MusicInfo
import com.example.mymusic.repo.remote.RetrofitConnection
import com.example.mymusic.viewmodel.BaseViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class FindMusicViewModel: BaseViewModel() {

    private val _songListLiveData = MutableLiveData<List<MusicInfo>>()
    val songListLiveData: LiveData<List<MusicInfo>> = _songListLiveData


    fun setSomeData(musicList:MutableList<MusicFindStyleInfo>){
        // 添加假数据
        musicList.add(MusicFindStyleInfo("欢喜节奏", "拒绝精神内耗，放松心情"))
        musicList.add(MusicFindStyleInfo("冥想之音", "静心冥想，释放压力"))
        musicList.add(MusicFindStyleInfo("热血摇滚", "燃烧卡路里，释放激情"))
        musicList.add(MusicFindStyleInfo("慢生活", "慢下来，感受每一个音符"))
        musicList.add(MusicFindStyleInfo("深夜电台", "夜晚的陪伴，温柔的旋律"))
        musicList.add(MusicFindStyleInfo("轻快早晨", "清新音乐唤醒活力"))
        musicList.add(MusicFindStyleInfo("治愈系", "温柔旋律抚慰心灵"))
        musicList.add(MusicFindStyleInfo("电子舞曲", "动感节奏嗨翻全场"))
        musicList.add(MusicFindStyleInfo("欢喜节奏", "拒绝精神内耗，放松心情"))
        musicList.add(MusicFindStyleInfo("冥想之音", "静心冥想，释放压力"))
        musicList.add(MusicFindStyleInfo("热血摇滚", "燃烧卡路里，释放激情"))
        musicList.add(MusicFindStyleInfo("慢生活", "慢下来，感受每一个音符"))
        musicList.add(MusicFindStyleInfo("深夜电台", "夜晚的陪伴，温柔的旋律"))
        musicList.add(MusicFindStyleInfo("轻快早晨", "清新音乐唤醒活力"))
        musicList.add(MusicFindStyleInfo("治愈系", "温柔旋律抚慰心灵"))
        musicList.add(MusicFindStyleInfo("电子舞曲", "动感节奏嗨翻全场"))
    }

    @SuppressLint("CheckResult")
    fun loadSongsByStyle(style: String) {
        val apiService = RetrofitConnection.apiService
        apiService.getSongsByStyle(style)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ resp ->
                if (resp.code == 200) {
                    val newList = resp.content
                    _songListLiveData.value = newList // 数据加载完成后更新 LiveData
                    Log.d("FindMusicViewModel", "成功获取歌单: ${newList.size}首")
                } else {
                    Log.e("FindMusicViewModel", "server error: ${resp.message}")
                }
            }, { error ->
                Log.e("FindMusicViewModel", "网络请求错误: ", error)
            }).addTo(this)
    }

}