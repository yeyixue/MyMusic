package com.example.mymusic.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.example.mymusic.R
import com.example.mymusic.viewmodel.fragment.FindMusicViewModel
import com.example.mymusic.viewmodel.fragment.MyMusicViewModel


class FindMusicFragment : BaseMusicFragment() {
    private lateinit var mFindMusicViewModel: FindMusicViewModel

    override fun getLayoutResId(): Int {
        return R.layout.fragment_find_music
    }

    override fun initView() {
        mFindMusicViewModel= ViewModelProvider(this).get(FindMusicViewModel::class.java)
    }

    override fun setListener() {
    }

    override fun initData() {
    }

}