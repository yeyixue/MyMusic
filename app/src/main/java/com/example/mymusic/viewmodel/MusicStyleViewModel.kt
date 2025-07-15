package com.example.mymusic.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicStyleInfo

class MusicStyleViewModel: ViewModel() {

    fun setSomeData(musicList: MutableList<MusicStyleInfo>) {
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "欢喜节奏"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "冥想之音"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "热血摇滚"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "慢生活"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "深夜电台"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "轻快早晨"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "治愈系"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "电子舞曲"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "欢喜节奏"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "冥想之音"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "热血摇滚"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "慢生活"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "深夜电台"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "轻快早晨"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "治愈系"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "慢生活"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "深夜电台"))
        musicList.add(MusicStyleInfo(R.mipmap.tiktokwhite, "轻快早晨"))
    }

}
