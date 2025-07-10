package com.example.mymusic.viewmodel.fragment

import androidx.lifecycle.ViewModel
import com.example.mymusic.adapter.MusicFindStyleInfo

class FindMusicViewModel: ViewModel() {


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

}