package com.example.mymusic.view.fragment

import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.StyleRecycleViewAdapter
import com.example.mymusic.adapter.MusicStyleInfo
import com.example.mymusic.viewmodel.MusicStyleViewModel
import androidx.lifecycle.ViewModelProvider

class MusicStyleFragment : BaseMusicFragment() {
    private lateinit var mMusicStyleViewModel: MusicStyleViewModel
    private lateinit var mStyleRecycleViewAdapter: StyleRecycleViewAdapter
    private lateinit var mRecyclerView: RecyclerView
    private val musicStyleList = mutableListOf<MusicStyleInfo>()

    // 保存滚动监听器的引用，便于销毁时移除
    private lateinit var scrollListener: RecyclerView.OnScrollListener

    override fun getLayoutResId(): Int = R.layout.fragment_music_style

    override fun initView() {
        mMusicStyleViewModel = ViewModelProvider(this)[MusicStyleViewModel::class.java]
        mRecyclerView = rootView.findViewById(R.id.recycleViewMusicStyle)
        setupRecyclerView()
        mMusicStyleViewModel.setSomeData(musicStyleList)
        mStyleRecycleViewAdapter.notifyDataSetChanged()
    }

    override fun setListener() {}

    override fun initData() {}

    private fun setupRecyclerView() {
        // 网格布局（3列）
        val layoutManager = GridLayoutManager(requireContext(), 3)
        mRecyclerView.layoutManager = layoutManager

        // 适配器与点击事件
        mStyleRecycleViewAdapter = StyleRecycleViewAdapter(musicStyleList)
        mRecyclerView.adapter = mStyleRecycleViewAdapter

        mStyleRecycleViewAdapter.setOnItemClickListener { noteInfo ->
            Log.d("mStyleRecycleViewAdapter", "点击了$noteInfo")
        }

        // 保存滚动监听器引用
        scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                // 滚动逻辑
            }
        }
        // 添加滚动监听
        mRecyclerView.addOnScrollListener(scrollListener)
    }

    // 视图销毁时清理监听（关键步骤）
    override fun onDestroyView() {
        super.onDestroyView()
        // 1. 移除RecyclerView的滚动监听，避免废弃视图继续拦截事件
        mRecyclerView.removeOnScrollListener(scrollListener)
        // 2. 清除适配器引用，切断与视图的关联
        mRecyclerView.adapter = null
        // 3. 清除点击事件监听器（如果适配器支持）
        mStyleRecycleViewAdapter.setOnItemClickListener(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 彻底释放资源
    }
}