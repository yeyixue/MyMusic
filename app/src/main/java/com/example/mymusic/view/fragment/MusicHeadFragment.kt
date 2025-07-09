package com.example.mymusic.view.fragment

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.mymusic.R
import com.example.mymusic.viewmodel.MusicHeadViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.get
import androidx.core.view.size
import com.example.mymusic.adapter.MusicPagerAdapter

class MusicHeadFragment : BaseMusicFragment() {
    private lateinit var viewmodel: MusicHeadViewModel
    private lateinit var viewPager2: ViewPager2
    private lateinit var mBottNavigationView: BottomNavigationView

    // 页面数量
    private val NUM_PAGES = 3

    override fun getLayoutResId(): Int = R.layout.fragment_music_head

    override fun initView() {
        viewmodel = ViewModelProvider(this).get(MusicHeadViewModel::class.java)
        mBottNavigationView = rootView.findViewById(R.id.bottomNavigationView)
        viewPager2 = rootView.findViewById(R.id.viewPager2_fragment)


        // 创建Fragment列表
        val fragmentList = listOf(
            MainMusicFragment(),
            FindMusicFragment(),
            MyMusicFragment()
        )
        // 设置ViewPager2适配器
        val pagerAdapter = MusicPagerAdapter(requireActivity(),fragmentList)
        viewPager2.adapter = pagerAdapter

        // 禁止滑动，只能通过底部导航栏切换
        viewPager2.isUserInputEnabled = false

        // 清除导航栏图标
        clearNavigationIcon()
    }

    override fun setListener() {
        // 底部导航栏选择监听
        mBottNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.main -> viewPager2.currentItem = 0
                R.id.find -> viewPager2.currentItem = 1
                R.id.mine -> viewPager2.currentItem = 2
            }
            true
        }

        // ViewPager2页面变化监听
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // 更新底部导航栏选中状态
                mBottNavigationView.menu.getItem(position).isChecked = true
                Log.d("MusicHeadFragment", "ViewPager2 selected page: $position")
            }
        })
    }

    override fun initData() {
        // 默认显示第一个页面
        viewPager2.currentItem = 0
    }

    fun clearNavigationIcon(){
        mBottNavigationView.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED)
        // 清除所有菜单项的图标
        for (i in 0 until mBottNavigationView.menu.size) {
            mBottNavigationView.menu[i].setIcon(null)
        }
    }


}

