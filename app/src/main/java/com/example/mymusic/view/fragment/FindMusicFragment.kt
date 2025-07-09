package com.example.mymusic.view.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.mymusic.R
import com.example.mymusic.adapter.FindRecycleViewAdapter
import com.example.mymusic.adapter.MusicFindStyleInfo
import com.example.mymusic.viewmodel.fragment.FindMusicViewModel


class FindMusicFragment : BaseMusicFragment() {
    private lateinit var mFindMusicViewModel: FindMusicViewModel
    private lateinit var mFindRecycleViewAdapter: FindRecycleViewAdapter
    private val musicList = mutableListOf<MusicFindStyleInfo>()


    override fun getLayoutResId(): Int {
        return R.layout.fragment_find_music
    }

    override fun initView() {
        mFindMusicViewModel= ViewModelProvider(this).get(FindMusicViewModel::class.java)
        setupRecyclerView()
        setSomeData() // 在 initView 后调用
    }

    override fun setListener() {
    }


    private fun setupRecyclerView() {
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recycleViewFindMusic)

        // 设置垂直布局管理器，并启用页面切换模式
//        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        val layoutManager =GridLayoutManager(requireContext(),2)
        recyclerView.layoutManager = layoutManager

        // 创建适配器并设置数据
        mFindRecycleViewAdapter = FindRecycleViewAdapter(musicList)
        recyclerView.adapter = mFindRecycleViewAdapter


        // 设置点击事件监听
        mFindRecycleViewAdapter.setOnItemClickListener(object : FindRecycleViewAdapter.OnItemClickListener{
            override fun onItemClick(noteInfo: MusicFindStyleInfo) {
                Log.d("mFindRecycleViewAdapter","  点击了$noteInfo  ")
            }
        })

        // 监听滚动事件，实现超过中间位置自动切换
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

            }
        })
    }

    override fun initData() {

    }

    private fun setSomeData(){
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

        // 通知适配器数据已更新（建议调用 notifyDataSetChanged）
        mFindRecycleViewAdapter.notifyDataSetChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

}