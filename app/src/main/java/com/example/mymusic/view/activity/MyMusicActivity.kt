package com.example.mymusic.view.activity

import DepthPageTransformer
import android.view.MotionEvent
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicAdapter
import com.example.mymusic.view.fragment.MusicHeadFragment
import com.example.mymusic.view.fragment.MusicStyleFragment
import com.example.mymusic.viewmodel.MyMusicViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MyMusicActivity : BaseMusicActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var mMusicHeadFragment: MusicHeadFragment
    private lateinit var mMusicStyleFragment: MusicStyleFragment
    private lateinit var tabLayout: TabLayout
    private lateinit var mMyMusicViewModel: MyMusicViewModel

    var currentMusicId:Int=-1
    val isPlaying: Boolean=false

    override fun getLayoutResId(): Int =R.layout.activity_my_music

    override fun initViews() {
        mMyMusicViewModel= ViewModelProvider(this).get(MyMusicViewModel::class.java)
        // 初始化 Fragments
        mMusicHeadFragment = MusicHeadFragment()
        mMusicStyleFragment = MusicStyleFragment()
        viewPager=findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabs)

    }

    override fun setListener() {
    }

    override fun initData() {
        setViewPager()

        setMusicDefault()

    }

    private fun setMusicDefault() {
//        // 启动的时候从网络获取歌单，
//        mMyMusicViewModel.setPlayListDefault()

        //在播放页设置启动音乐
    }

    fun setViewPager(){
        // 这个顺序就是页面显示的顺序
        val fragmentList = mutableListOf<Fragment>(mMusicHeadFragment,mMusicStyleFragment)
        val adapter= MusicAdapter(this,fragmentList)
        viewPager.adapter=adapter

        // 设置tablayout
        // 正确关联 TabLayout 和 ViewPager2（使用 TabLayoutMediator）
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position){
                0 -> "我的音乐"
                1 -> "听歌模式"
                else ->null
            }
//            tab.icon = when(position) {
//                0 -> resources.getDrawable(R.drawable.ic_launcher_background, theme)
//                1 -> resources.getDrawable(R.drawable.flower, theme)
//                else -> null
//            }
        }.attach()

//        // 设置显示的页面
//        viewPager.currentItem = 1
        // 设置页面切换动画
        viewPager.setPageTransformer(DepthPageTransformer())
        viewPager.offscreenPageLimit=2

        viewPager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback(){
            // 记录滑动方向
            private var isSwipingLeft = false
            private var isSwipingRight = false
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
//                    isSwipingLeft = viewPager.currentItem < position  // 从右向左滑
//                    isSwipingRight = viewPager.currentItem > position  // 从左向右滑
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                // 完全显示第一页且尝试向右滑动
                if (position == 0 && positionOffset == 0f && isSwipingRight) {
                    viewPager.isUserInputEnabled = false  // 禁止滑动
                }
                // 完全显示最后一页且尝试向左滑动
                else if (position == (viewPager.adapter?.itemCount ?: 0) - 1 &&
                    positionOffset == 0f && isSwipingLeft) {
                    viewPager.isUserInputEnabled = false  // 禁止滑动
                }
                // 其他情况允许滑动
                else {
                    viewPager.isUserInputEnabled = true
                }
            }


        })
    }

    // 判断滑动方向（根据当前事件和上次事件的X坐标比较）
    private var lastX = 0f
    private fun isSwipingLeft(event: MotionEvent): Boolean {
        val isLeft = event.x < lastX
        lastX = event.x
        return isLeft
    }

    private fun isSwipingRight(event: MotionEvent): Boolean {
        val isRight = event.x > lastX
        lastX = event.x
        return isRight
    }
}