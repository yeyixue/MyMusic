package com.example.mymusic.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.mymusic.R
import com.example.mymusic.viewmodel.MyMusicViewModel
import com.example.mymusic.viewmodel.fragment.MyMusicFragmentViewModel

class MyMusicFragment : BaseMusicFragment() {
    private lateinit var mMyMusicFragmentViewModel: MyMusicFragmentViewModel

    override fun getLayoutResId(): Int {
        return R.layout.fragment_my_music
    }

    override fun initView() {

        // 正确初始化 ViewModel（获取 MyMusicFragmentViewModel 实例）
        mMyMusicFragmentViewModel = ViewModelProvider(this).get(MyMusicFragmentViewModel::class.java)
    }

    override fun setListener() {
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}