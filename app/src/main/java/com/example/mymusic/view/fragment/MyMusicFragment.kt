package com.example.mymusic.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.mymusic.R
import com.example.mymusic.viewmodel.MyMusicViewModel

class MyMusicFragment : BaseMusicFragment() {
    private lateinit var mMyMusicViewModel: MyMusicViewModel

    override fun getLayoutResId(): Int {
        return R.layout.fragment_my_music
    }

    override fun initView() {

        // 正确初始化 ViewModel（获取 MyMusicViewModel 实例）
        mMyMusicViewModel = ViewModelProvider(this).get(MyMusicViewModel::class.java)
    }

    override fun setListener() {
    }

    override fun initData() {
    }

}