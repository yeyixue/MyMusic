package com.example.mymusic.view.activity

import DepthPageTransformer
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.adapter.MusicAdapter
import com.example.mymusic.view.fragment.MusicHeadFragment
import com.example.mymusic.view.fragment.MusicStyleFragment
import com.example.mymusic.viewmodel.fragment.MyMusicViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class MyMusicActivity : BaseMusicActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var mMusicHeadFragment: MusicHeadFragment
    private lateinit var mMusicStyleFragment: MusicStyleFragment
    private lateinit var tabLayout: TabLayout
    private lateinit var mMyMusicViewModel: MyMusicViewModel
    private var isPlaybackPageVisible = false   // MainMusicFragment 是否可见
    private var allowScroll = false   // 是否允许滑动，


    // 定义变量保存注册的回调实例
    // 用于灵敏度计算的变量
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private val scaledTouchSlop by lazy {
        ViewConfiguration.get(this).scaledPagingTouchSlop // 系统默认滑动阈值
    }
    // 灵敏度系数（值越小越灵敏，建议 0.5f-2.0f）
    private val sensitivityFactor = 0.7f // 调整这个值控制灵敏度




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
        }.attach()

//        // 设置显示的页面
//        viewPager.currentItem = 1
        // 设置页面切换动画
        viewPager.setPageTransformer(DepthPageTransformer())
        //这会导致左右两侧的 Fragment 都被预加载并初始化（包括设置点击监听）。
//        viewPager.offscreenPageLimit=1

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateScrollPermission()
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                updateScrollPermission()

            }
        })

    }


    // 重写触摸事件，调整滑动灵敏度
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if(allowScroll){
            ev?.let { event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录初始触摸位置
                        initialX = event.x
                        initialY = event.y
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = Math.abs(event.x - initialX) // 水平滑动距离
                        val dy = Math.abs(event.y - initialY) // 垂直滑动距离

                        // 调整灵敏度：缩小触发滑动的阈值（默认阈值 * 灵敏度系数）
                        val adjustedTouchSlop = (scaledTouchSlop * sensitivityFactor).toInt()

                        // 水平滑动且超过调整后的阈值，才允许 ViewPager2 响应滑动
                        if (dx > adjustedTouchSlop && dx > dy) {
                            viewPager.isUserInputEnabled = true
                        } else {
                            // 未达到滑动阈值或垂直滑动，不响应（提高灵敏度时更易触发）
                            viewPager.isUserInputEnabled = false
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 触摸结束后恢复响应
                        viewPager.isUserInputEnabled = true
                        isPlaybackPageVisible=true
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun onPlaybackPageVisible(visible: Boolean) {
        isPlaybackPageVisible = visible
        // 这个执行完又重新执行 touchEvent,所以在up上设置判断
        updateScrollPermission()
    }

    private fun updateScrollPermission() {
        viewPager.isUserInputEnabled = isPlaybackPageVisible
    }


    override fun onDestroy() {
        super.onDestroy()
    }


}