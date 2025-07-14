package com.example.mymusic.viewmodel

import android.content.res.Resources
import android.view.MenuItem
import androidx.lifecycle.ViewModel
import com.example.mymusic.R

class MusicHeadViewModel: ViewModel() {

    // 更新播放图标的辅助方法
   fun updatePlayIcon(menuItem: MenuItem, isPlaying: Boolean,resources: Resources) {
        menuItem.title = ""
        menuItem.icon = if (isPlaying) {
            resources.getDrawable(R.mipmap.runing, null)
        } else {
            resources.getDrawable(R.mipmap.stop, null)
        }
    }

}