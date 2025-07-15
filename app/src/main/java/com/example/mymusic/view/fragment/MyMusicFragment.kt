package com.example.mymusic.view.fragment

import DepthPageTransformer
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.adapter.MyMusicAdapter
import com.example.mymusic.view.mymusicfragment.DownLoadFragment
import com.example.mymusic.view.mymusicfragment.PlayListFragment
import com.example.mymusic.viewmodel.fragment.MyMusicFragmentViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MyMusicFragment : BaseMusicFragment() {
    private lateinit var mMyMusicFragmentViewModel: MyMusicFragmentViewModel
    private lateinit var mDownLoadFragment:DownLoadFragment
    private lateinit var mPlayListFragment:PlayListFragment
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun getLayoutResId(): Int {
        return R.layout.fragment_my_music
    }

    override fun initView() {
        // 正确初始化 ViewModel
        mMyMusicFragmentViewModel = ViewModelProvider(this).get(MyMusicFragmentViewModel::class.java)
        mDownLoadFragment=DownLoadFragment()
        mPlayListFragment=PlayListFragment()
        viewPager=rootView.findViewById(R.id.myViewPager)
        tabLayout=rootView.findViewById(R.id.tabsMyMusic)
    }

    override fun setListener() {
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
        viewPager.setPageTransformer(DepthPageTransformer())
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