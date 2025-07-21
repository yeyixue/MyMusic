package com.example.mymusic.view.fragment

import DepthPageTransformer
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.adapter.MyMusicAdapter
import com.example.mymusic.view.mymusicfragment.DownLoadFragment
import com.example.mymusic.view.mymusicfragment.PlayListFragment
import com.example.mymusic.viewmodel.fragment.MyMusicViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MyMusicFragment : BaseMusicFragment() {
    private lateinit var mMyMusicFragmentViewModel: MyMusicViewModel
    private lateinit var mDownLoadFragment:DownLoadFragment
    private lateinit var mPlayListFragment:PlayListFragment
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    // 用于灵敏度计算的变量
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private val scaledTouchSlop by lazy {
        ViewConfiguration.get(requireActivity()).scaledPagingTouchSlop // 系统默认滑动阈值
    }
    // 灵敏度系数（值越小越灵敏，建议 0.5f-2.0f）
    private val sensitivityFactor = 0.7f // 调整这个值控制灵敏度
    override fun getLayoutResId(): Int {
        return R.layout.fragment_my_music
    }

    override fun initView() {
        // 正确初始化 ViewModel
        mMyMusicFragmentViewModel = ViewModelProvider(this).get(MyMusicViewModel::class.java)
        mDownLoadFragment=DownLoadFragment()
        mPlayListFragment=PlayListFragment()
        viewPager=rootView.findViewById(R.id.myViewPager)
        tabLayout=rootView.findViewById(R.id.tabsMyMusic)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun setListener() {
        // 关键：给 ViewPager2 设置触摸监听
        viewPager.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = event.x
                    initialY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = Math.abs(event.x - initialX) // 水平滑动距离
                    val dy = Math.abs(event.y - initialY) // 垂直滑动距离

                    // 计算调整后的滑动阈值（默认阈值 × 灵敏度系数）
                    val adjustedTouchSlop = (scaledTouchSlop * sensitivityFactor).toInt()

                    // 仅当水平滑动超过阈值，才允许 ViewPager2 响应滑动
                    viewPager.isUserInputEnabled = dx > adjustedTouchSlop && dx > dy
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    viewPager.isUserInputEnabled = true // 触摸结束后恢复响应
                }
            }
            // 返回 false，让 ViewPager2 继续处理滑动事件（否则会阻断滚动）
            false
        }
    }

    override fun initData() {
        setViewPager()
    }

    fun setViewPager(){
        // 这个顺序就是页面显示的顺序
        val fragmentList = mutableListOf<Fragment>(mPlayListFragment,mDownLoadFragment)
        val adapter= MyMusicAdapter(this,fragmentList)
        viewPager.adapter=adapter

        // 设置tablayout
        // 正确关联 TabLayout 和 ViewPager2（使用 TabLayoutMediator）
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position){
                0 -> "歌单"
                1 -> "下载"
                else ->null
            }

        }.attach()

//        // 设置显示的页面
//        viewPager.currentItem = 1
        // 设置页面切换动画
//        viewPager.setPageTransformer(DepthPageTransformer())
//        viewPager.offscreenPageLimit=2

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






    override fun onDestroy() {
        super.onDestroy()
    }
}