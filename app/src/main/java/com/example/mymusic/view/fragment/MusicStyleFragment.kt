package com.example.mymusic.view.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mymusic.R
import com.example.mymusic.viewmodel.MusicHeadViewModel


class MusicStyleFragment : BaseMusicFragment() {
    private lateinit var viewmodel:ViewModel
    override fun getLayoutResId(): Int =R.layout.fragment_music_style

    override fun initView() {
        viewmodel= ViewModelProvider(this).get(MusicHeadViewModel::class.java)
    }

    override fun setListener() {
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}