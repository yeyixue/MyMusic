package com.example.mymusic.viewmodel.fragment

import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicRecycleViewAdapter
import com.example.mymusic.adapter.NoteInfo

class MainMusicViewModel: ViewModel() {



    // 处理页面切换逻辑
    fun handlePageSwitch(recyclerView: RecyclerView, layoutManager: LinearLayoutManager) {
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


    fun loadMusicList(musicList: MutableList<NoteInfo>) {
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

}