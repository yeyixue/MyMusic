package com.example.mymusic.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.adapter.NoteInfo
import com.example.mymusic.viewmodel.fragment.MainMusicViewModel
import com.example.mymusic.viewmodel.fragment.MyMusicViewModel


class MainMusicFragment : BaseMusicFragment() {
    private lateinit var mMainMusicViewModel: MainMusicViewModel
    private lateinit var mMusicRecycleViewAdapter:MusicRecycleViewAdapter
    private val musicList = mutableListOf<NoteInfo>()
    override fun getLayoutResId(): Int {
        return R.layout.fragment_main_music
    }

    override fun initView() {
        mMainMusicViewModel= ViewModelProvider(this).get(MainMusicViewModel::class.java)
        // 加载信息
        mMainMusicViewModel.loadMusicList(musicList)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycleView)

        // 设置垂直布局管理器，并启用页面切换模式
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager

        // 创建适配器并设置数据
        mMusicRecycleViewAdapter = MusicRecycleViewAdapter(musicList)
        recyclerView.adapter = mMusicRecycleViewAdapter

        // 设置 PagerSnapHelper 实现页面切换效果
        val snapHelper = PagerSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)
        // 设置点击事件监听
        mMusicRecycleViewAdapter.setOnItemClickListener(object : MusicRecycleViewAdapter.OnItemClickListener{
            override fun onItemClick(noteInfo: NoteInfo) {
                Log.d("mMusicRecycleViewAdapter","  点击了$noteInfo  ")
            }
        })

        // 监听滚动事件，实现超过中间位置自动切换
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // 当滚动停止时，检查是否需要自动切换页面
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mMainMusicViewModel.handlePageSwitch(recyclerView, layoutManager)
                }
            }
        })
    }





    override fun setListener() {
    }

    override fun initData() {
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}