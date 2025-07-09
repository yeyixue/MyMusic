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
        loadMusicList()
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
                Log.d("recycleview","  点击了$noteInfo  ")
            }
        })

        // 监听滚动事件，实现超过中间位置自动切换
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // 当滚动停止时，检查是否需要自动切换页面
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    handlePageSwitch(recyclerView, layoutManager)
                }
            }
        })
    }

    // 处理页面切换逻辑
    private fun handlePageSwitch(recyclerView: RecyclerView, layoutManager: LinearLayoutManager) {
        // 获取第一个完全可见的 item 位置
        val firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

        // 如果没有完全可见的 item，获取第一个可见 item 的位置
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        // 如果列表为空，直接返回
        if (firstVisibleItemPosition == RecyclerView.NO_POSITION) return

        // 获取 RecyclerView 的高度（用于计算中间位置）
        val recyclerViewHeight = recyclerView.height

        // 如果有完全可见的 item，直接滚动到该位置
        if (firstCompletelyVisibleItemPosition != RecyclerView.NO_POSITION) {
            return
        }

        // 如果没有完全可见的 item，计算哪个 item 超过了中间位置
        for (i in firstVisibleItemPosition..lastVisibleItemPosition) {
            val view = layoutManager.findViewByPosition(i) ?: continue

            // 计算 item 顶部距离 RecyclerView 顶部的距离
            val top = recyclerView.getChildAt(0).top

            // 如果 item 顶部超过了 RecyclerView 的中间位置，滚动到下一个 item
            if (top < -recyclerViewHeight / 2) {
                recyclerView.smoothScrollToPosition(i + 1)
                return
            }

            // 如果 item 底部超过了 RecyclerView 的中间位置，滚动到当前 item
            val bottom = recyclerView.getChildAt(0).bottom
            if (bottom > recyclerViewHeight / 2) {
                recyclerView.smoothScrollToPosition(i)
                return
            }
        }
    }

    private fun loadMusicList() {
        // 添加 TYPE_ITEM1（音乐类型）：type=1，设置 color
        musicList.add(NoteInfo(
            color = R.color.red,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))
        musicList.add(NoteInfo(
            color = R.color.blue,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))

        musicList.add(NoteInfo(
            color = 0,  // 该类型用不到 color，填 0 即可
            type = MusicRecycleViewAdapter.TYPE_ITEM2,
            imgRes = R.drawable.flower  // 视频类型的图片资源
        ))
        musicList.add(NoteInfo(
            color = R.color.purple_200,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))
        musicList.add(NoteInfo(
            color = R.color.teal_700,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))
        musicList.add(NoteInfo(
            color = 0,
            type = MusicRecycleViewAdapter.TYPE_ITEM2,
            imgRes = R.drawable.imgjiagou  // 另一张图片
        ))
        musicList.add(NoteInfo(
            color = R.color.black,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))
        musicList.add(NoteInfo(
            color = R.color.purple_700,
            type = MusicRecycleViewAdapter.TYPE_ITEM1
        ))

    }

    override fun setListener() {
    }

    override fun initData() {
    }
}